package com.example.helloworld

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.helloworld.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/* =====================================================================
 *  หน้าอัปโหลดภาพฉลาก
 *
 *  กรอบเส้นประแตะเพื่อเลือกรูป วงกลมเรืองแสงกับไอคอนอัปโหลด
 *  เลือกแล้ว แสดงแถวไฟล์ ภาพย่อ ชื่อ ขนาด ปุ่มปิด และแถบความคืบหน้า
 *  ปุ่มล่าง ยกเลิก / ตรวจสอบ
 *
 *  แถบความคืบหน้า
 *    0 - 40%   อัปโหลดจริง นับตามไบต์ที่ส่งไป
 *    40 - 94%  รอเซิร์ฟเวอร์ YOLO ครอบ OCR อ่าน เทียบฐานข้อมูล แสดงเป็นขั้นตอน
 *    100%      ได้ผลแล้ว บันทึกประวัติ เปิดหน้าผลตรวจ
 *
 *  ได้ผลแล้วบันทึกลง HistoryStore ที่นี่ที่เดียว ทั้งเลข ชื่อ ผู้ผลิต ประเภท
 *  ที่ตั้ง และภาพที่ครอบ แล้วส่งแค่ id ไปหน้าผลตรวจ
 * ===================================================================== */
class ImportActivity : AppCompatActivity() {

    private enum class St { EMPTY, READY, UPLOADING, WORKING }

    private var st = St.EMPTY
    private var imageUri: Uri? = null
    private var job: Job? = null
    private var stageJob: Job? = null
    @Volatile private var lastPct = -1
    @Volatile private var workingStarted = false

    private lateinit var drop: FrameLayout
    private lateinit var dropEmpty: LinearLayout
    private lateinit var dropPreview: ImageView
    private lateinit var changeChip: TextView
    private lateinit var fileCard: LinearLayout
    private lateinit var fileThumb: ImageView
    private lateinit var fileName: TextView
    private lateinit var fileMeta: TextView
    private lateinit var progress: ProgressLine
    private lateinit var status: TextView
    private lateinit var cancelBtn: Pill
    private lateinit var checkBtn: Pill

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge(lightStatus = true)
        setContentView(appStage(build(), P.TAB_SCAN, topBar("สแกนฉลาก", "ขั้นตอนที่ 1 จาก 2")))
        setState(St.EMPTY)

        @Suppress("DEPRECATION")
        val saved = savedInstanceState?.getParcelable<Uri>("uri")
        if (saved != null) onPicked(saved)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        imageUri?.let { outState.putParcelable("uri", it) }
    }

    // ================================================================ หน้าจอ

    private fun build(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(4), dpx(20), dpx(24))
        }
        root.addView(headline("อัปโหลด", "ภาพฉลากสินค้า"))
        root.addView(txt("AI จะครอบบริเวณเลข อย. บนฉลากให้อัตโนมัติ แล้วเทียบกับฐานข้อมูลจริง",
            14f, P.slate).apply {
            setLineSpacing(dpx(5).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(8))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = solidOf(P.white, 32, P.line)
            elevation = dpx(6).toFloat()
            shadow(a(P.navy, 0.5f))
            setPadding(dpx(14), dpx(14), dpx(14), dpx(16))
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(20))
        }

        // ---------- กรอบเส้นประ ----------
        drop = FrameLayout(this).apply {
            background = rippleOf(dashedOf(P.skyHi, 24, P.tealSoft), a(P.teal, 0.12f), 24)
            isClickable = true
            isFocusable = true
            contentDescription = "เลือกรูปฉลาก"
            setOnClickListener { if (st == St.EMPTY || st == St.READY) pickImage() }
            layoutParams = lpOf(MATCH_PARENT, dpx(272), 0)
        }
        dropEmpty = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpx(18), dpx(14), dpx(18), dpx(14))
            addView(txt("แตะเพื่อเลือกรูปฉลาก", 17f, P.ink, true).apply { gravity = Gravity.CENTER })
            addView(glowButton(), LinearLayout.LayoutParams(dpx(116), dpx(116)).apply {
                topMargin = dpx(6)
                gravity = Gravity.CENTER_HORIZONTAL
            })
            addView(txt("ใช้รูปที่เห็นเลข อย. ชัด ไม่เบลอ ไม่มีแสงสะท้อน\nรองรับไฟล์ JPG และ PNG",
                13f, P.muted).apply {
                gravity = Gravity.CENTER
                setLineSpacing(dpx(4).toFloat(), 1f)
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(4))
            })
        }
        drop.addView(dropEmpty, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        dropPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
            background = solidOf(P.skySoft, 18)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        drop.addView(dropPreview, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
            setMargins(dpx(8), dpx(8), dpx(8), dpx(8))
        })

        changeChip = txt("เปลี่ยนรูป", 12.5f, P.navy, true).apply {
            background = solidOf(P.white, 100, P.line)
            setPadding(dpx(14), dpx(7), dpx(14), dpx(7))
            elevation = dpx(3).toFloat()
            visibility = View.GONE
        }
        drop.addView(changeChip, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dpx(18), dpx(18)) })
        card.addView(drop)

        // ---------- แถวไฟล์ ----------
        fileCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = solidOf(P.skyHi, 22, P.line)
            setPadding(dpx(14), dpx(14), dpx(14), dpx(14))
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        fileThumb = roundImage(13, P.skySoft).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        row.addView(FrameLayout(this).apply {
            background = solidOf(P.white, 16, P.line)
            setPadding(dpx(3), dpx(3), dpx(3), dpx(3))
            elevation = dpx(2).toFloat()
            addView(fileThumb, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }, LinearLayout.LayoutParams(dpx(58), dpx(58)))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                leftMargin = dpx(12)
                rightMargin = dpx(8)
            }
        }
        fileName = txt("", 15f, P.ink, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        fileMeta = txt("", 12.5f, P.muted).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2)) }
        col.addView(fileName)
        col.addView(fileMeta)
        row.addView(col)
        row.addView(circleBtn(Ico.CLOSE, 38) { onCancel() }.apply { contentDescription = "ยกเลิกรูปนี้" })
        fileCard.addView(row)

        progress = ProgressLine(this).apply { layoutParams = lpOf(MATCH_PARENT, dpx(7), dpx(14)) }
        fileCard.addView(progress)
        status = txt("", 13f, P.teal, true).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(8)) }
        fileCard.addView(status)
        card.addView(fileCard)

        // ---------- ปุ่ม ----------
        val btns = LinearLayout(this).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16)) }
        cancelBtn = pill("ยกเลิก", Btn.SECONDARY) { onCancel() }
        checkBtn = pill("ตรวจสอบ", Btn.PRIMARY, Ico.SEARCH) { startCheck() }
        btns.addView(cancelBtn.view, LinearLayout.LayoutParams(0, dpx(56), 1f))
        btns.addView(checkBtn.view, LinearLayout.LayoutParams(0, dpx(56), 1.35f).apply { leftMargin = dpx(10) })
        card.addView(btns)
        root.addView(card)

        // ---------- คำแนะนำ ----------
        val tip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = solidOf(P.beige, 24, P.beigeDeep)
            setPadding(dpx(18), dpx(16), dpx(18), dpx(18))
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(18))
        }
        tip.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icoView(Ico.INFO, P.beigeInk, 20, 0))
            addView(txt("ถ่ายยังไงให้ AI อ่านออก", 15f, P.beigeInk, true).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dpx(8) }
            })
        })
        listOf(
            "ถือกล้องให้ขนานกับกล่อง ไม่เอียงเกินไป",
            "เลข อย. มักอยู่ด้านหลัง ใกล้ตารางโภชนาการ",
            "ถ่ายในที่สว่าง หลีกเลี่ยงแสงสะท้อนบนตัวเลข"
        ).forEachIndexed { i, s -> tip.addView(tipRow(i + 1, s)) }
        root.addView(tip)

        return root
    }

    private fun glowButton() = FrameLayout(this).apply {
        addView(View(this@ImportActivity).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = dpx(58).toFloat()
                colors = intArrayOf(a(P.sky, 0.95f), a(P.sky, 0f))
            }
        }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(FrameLayout(this@ImportActivity).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(P.white, P.skySoft)).apply {
                shape = GradientDrawable.OVAL
                setStroke(dpx(2), P.white)
            }
            elevation = dpx(8).toFloat()
            shadow(P.teal)
            addView(ImageView(this@ImportActivity).apply { setImageDrawable(Ico(Ico.UPLOAD, P.teal, 2f)) },
                FrameLayout.LayoutParams(dpx(30), dpx(30), Gravity.CENTER))
        }, FrameLayout.LayoutParams(dpx(78), dpx(78), Gravity.CENTER))
    }

    private fun tipRow(n: Int, s: String) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(10))
        addView(txt("$n", 12f, P.beigeInk, true).apply {
            gravity = Gravity.CENTER
            background = circleOf(P.white, P.beigeDeep)
            layoutParams = LinearLayout.LayoutParams(dpx(24), dpx(24))
        })
        addView(txt(s, 13.5f, P.beigeInk).apply {
            setLineSpacing(dpx(4).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(10) }
        })
    }

    // ================================================================ สถานะ

    private fun setState(s: St) {
        st = s
        val busy = s == St.UPLOADING || s == St.WORKING
        stylePill(checkBtn, Btn.PRIMARY, s == St.READY, when (s) {
            St.UPLOADING -> "กำลังอัปโหลด"
            St.WORKING -> "กำลังตรวจ…"
            else -> "ตรวจสอบ"
        })
        stylePill(cancelBtn, Btn.SECONDARY, s != St.EMPTY, if (busy) "หยุด" else "ยกเลิก")
        changeChip.visibility = if (s == St.READY) View.VISIBLE else View.GONE
        drop.alpha = if (busy) 0.85f else 1f
    }

    private fun showFileCard() {
        if (fileCard.visibility == View.VISIBLE) return
        fileCard.visibility = View.VISIBLE
        fileCard.alpha = 0f
        fileCard.translationY = dpx(12).toFloat()
        fileCard.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }

    private fun resetAll() {
        job = null
        stageJob?.cancel()
        imageUri = null
        dropPreview.setImageDrawable(null)
        dropPreview.visibility = View.GONE
        dropEmpty.visibility = View.VISIBLE
        fileCard.visibility = View.GONE
        progress.set(0f, false)
        setState(St.EMPTY)
    }

    // ================================================================ เลือกรูป

    private fun pickImage() {
        try {
            picker.launch("image/*")
        } catch (e: Exception) {
            Log.e("ImportActivity", "เปิดตัวเลือกรูปไม่ได้", e)
            showBanner(Banner.ERROR, "เปิดตัวเลือกรูปไม่ได้", "ไม่พบแอปสำหรับเลือกรูปบนเครื่องนี้")
        }
    }

    private fun onPicked(uri: Uri) {
        if (st == St.UPLOADING || st == St.WORKING) return
        imageUri = uri
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try { decodeUpright(uri, 1400) } catch (e: Throwable) { null }
            }
            if (bmp == null) {
                imageUri = null
                showBanner(Banner.ERROR, "เปิดรูปนี้ไม่ได้", "ลองเลือกรูปอื่น หรือถ่ายใหม่อีกครั้ง")
                return@launch
            }
            dropPreview.setImageBitmap(bmp)
            dropPreview.visibility = View.VISIBLE
            dropPreview.alpha = 0f
            dropPreview.animate().alpha(1f).setDuration(260).start()
            dropEmpty.visibility = View.GONE
            fileThumb.setImageBitmap(bmp)

            val (name, size) = withContext(Dispatchers.IO) { fileInfo(uri) }
            fileName.text = name
            val ext = name.substringAfterLast('.', "").uppercase().ifBlank { "IMAGE" }
            fileMeta.text = if (size != null) "$ext · ${formatSize(size)}" else ext

            progress.set(0f, false)
            status.setTextColor(P.teal)
            status.text = "พร้อมตรวจสอบ กดปุ่ม ตรวจสอบ ได้เลย"
            showFileCard()
            setState(St.READY)
        }
    }

    // ================================================================ ตรวจสอบ

    private fun startCheck() {
        val uri = imageUri
        if (uri == null) {
            showBanner(Banner.INFO, "ยังไม่ได้เลือกรูป", "แตะที่กรอบด้านบนเพื่อเลือกรูปฉลากก่อน")
            return
        }
        if (st != St.READY) return

        lastPct = -1
        workingStarted = false
        setState(St.UPLOADING)
        status.setTextColor(P.teal)
        status.text = "กำลังเตรียมรูป…"
        progress.set(0.03f)

        job = lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { prepareUpload(uri) }
                val body = ProgressBody(file, "image/jpeg".toMediaType()) { f -> onUpload(f) }
                val part = MultipartBody.Part.createFormData("file", file.name, body)

                val res = ApiClient.api.checkOy(part)
                stageJob?.cancel()

                if (!res.isSuccessful) {
                    fail("เซิร์ฟเวอร์ตอบกลับผิดพลาด", "รหัส HTTP ${res.code()} ลองใหม่อีกครั้งในอีกสักครู่")
                    return@launch
                }

                val b = res.body()
                val number = b?.oyNumber?.trim().orEmpty()
                if (b == null || number.isBlank() || number == "-") {
                    val msg = b?.message?.takeIf { it.isNotBlank() && it != "-" }
                        ?: "ลองถ่ายให้ใกล้และชัดขึ้น ให้เลข อย. อยู่กลางภาพ"
                    fail("อ่านเลข อย. ไม่ได้", msg)
                    return@launch
                }

                // ภาพที่ YOLO ครอบเฉพาะกรอบเลข อย.
                // 1) ถ้า /check-oy ส่ง crop_image มาด้วย ใช้อันนั้น
                // 2) ถ้าไม่ส่งมา ขอจาก /debug/crop ด้วยรูปเดียวกัน
                // 3) ขอไม่ได้จริง ๆ ค่อยใช้ภาพต้นฉบับ
                val cropB64 = cropOf(b)
                var cropBytes: ByteArray? = null
                if (cropB64.isNullOrBlank()) {
                    status.text = "กำลังครอบภาพเฉพาะกรอบเลข อย.…"
                    cropBytes = withContext(Dispatchers.IO) {
                        // ส่งไฟล์ต้นฉบับ (ไม่ย่อ) ให้ได้กรอบเดียวกับที่ทดสอบใน Swagger
                        val original = copyOriginal(uri)
                        fetchCrop(res.raw().request.url, original ?: file)
                            ?: if (original != null) fetchCrop(res.raw().request.url, file) else null
                    }
                }

                progress.set(1f, true, 350)
                status.setTextColor(P.success)
                status.text = "เสร็จสิ้น กำลังเปิดผลตรวจ"

                val (imagePath, isCrop) = withContext(Dispatchers.IO) { saveEvidence(cropB64, cropBytes, file) }

                // บันทึกประวัติครบทุกฟิลด์ ก่อนเปิดหน้าผลตรวจ
                val entry = HistoryStore.add(
                    this@ImportActivity,
                    fdaNo = number,
                    name = b.detail?.nameTh,
                    company = b.detail?.locationName,
                    productType = b.detail?.productType,
                    address = b.detail?.address,
                    valid = b.success,
                    message = b.message,
                    imagePath = imagePath,
                    isCrop = isCrop
                )

                delay(250)
                startActivity(ResultActivity.open(this@ImportActivity, entry.id, false))
                resetAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ImportActivity", "เรียก API ไม่สำเร็จ", e)
                stageJob?.cancel()
                fail("เชื่อมต่อเซิร์ฟเวอร์ไม่ได้",
                    "ตรวจว่า backend เปิดอยู่และ BASE_URL ถูกต้อง แล้วลองอีกครั้ง")
            }
        }
    }

    /** เรียกจากเธรดของ OkHttp */
    private fun onUpload(f: Float) {
        val pct = (f * 100).toInt().coerceIn(0, 100)
        if (pct == lastPct) return
        lastPct = pct
        runOnUiThread {
            if (st != St.UPLOADING) return@runOnUiThread
            progress.set(0.04f + 0.36f * f, false)
            status.text = "กำลังอัปโหลดรูป $pct%"
            if (pct >= 100 && !workingStarted) {
                workingStarted = true
                startWorking()
            }
        }
    }

    private fun startWorking() {
        setState(St.WORKING)
        progress.set(0.94f, true, 16000L)
        stageJob?.cancel()
        stageJob = lifecycleScope.launch {
            val steps = listOf(
                "AI กำลังหากรอบเลข อย. บนฉลาก…" to 0L,
                "กำลังครอบภาพและอ่านตัวเลข…" to 3500L,
                "กำลังเทียบกับฐานข้อมูล อย.…" to 4500L,
                "ใกล้เสร็จแล้ว ขอบคุณที่รอ…" to 6000L
            )
            for ((text, wait) in steps) {
                delay(wait)
                status.text = text
            }
        }
    }

    private fun fail(title: String, message: String) {
        stageJob?.cancel()
        progress.set(0f)
        status.setTextColor(P.error)
        status.text = title
        setState(St.READY)
        showBanner(Banner.ERROR, title, message, 5200L)
    }

    private fun onCancel() {
        when (st) {
            St.UPLOADING, St.WORKING -> {
                job?.cancel()
                stageJob?.cancel()
                progress.set(0f)
                status.setTextColor(P.muted)
                status.text = "หยุดการตรวจแล้ว กดตรวจสอบเพื่อเริ่มใหม่"
                setState(St.READY)
                showBanner(Banner.INFO, "หยุดการตรวจสอบแล้ว", "รูปยังอยู่ กดตรวจสอบเพื่อลองใหม่ได้")
            }
            St.READY -> resetAll()
            St.EMPTY -> Unit
        }
    }

    // ================================================================ ไฟล์

    /** ย่อไม่เกิน 2000px หมุนตาม EXIF แล้วบีบเป็น JPEG ก่อนส่ง ส่งเร็วขึ้นและตัวเลขไม่ตะแคง */
    private fun prepareUpload(uri: Uri): File {
        val out = File(cacheDir, "upload_label.jpg")
        val bmp = try { decodeUpright(uri, 2000) } catch (e: Throwable) { null }
        if (bmp != null) {
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } else {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { input.copyTo(it) }
            } ?: throw IllegalStateException("อ่านไฟล์รูปไม่ได้")
        }
        return out
    }

    private fun decodeUpright(uri: Uri, maxSide: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0 || o.outHeight <= 0) return null

        var s = 1
        while (o.outWidth / (s * 2) >= maxSide || o.outHeight / (s * 2) >= maxSide) s *= 2
        var bmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
        } ?: return null

        val longSide = maxOf(bmp.width, bmp.height)
        if (longSide > maxSide) {
            val k = maxSide.toFloat() / longSide
            bmp = Bitmap.createScaledBitmap(bmp,
                (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
        }
        val deg = exifDegrees(uri)
        if (deg != 0) {
            val m = Matrix().apply { postRotate(deg.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }

    private fun exifDegrees(uri: Uri): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 0
        return try {
            contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * อ่านภาพครอป base64 จากผลตอบกลับ ถ้าใน CheckOyResponse ยังไม่มีฟิลด์นี้ก็ไม่ error
     * คืน null แล้วใช้ภาพต้นฉบับแทน เพิ่มฟิลด์นี้ใน CheckOyResponse เมื่อพร้อม
     * @SerializedName("crop_image") val cropImage: String? = null
     */
    private fun cropOf(body: Any): String? {
        for (n in listOf("cropImage", "crop_image", "crop", "cropBase64")) {
            try {
                val f = body.javaClass.getDeclaredField(n)
                f.isAccessible = true
                val v = f.get(body) as? String
                if (!v.isNullOrBlank()) return v
            } catch (e: Exception) {
                // ไม่มีฟิลด์ชื่อนี้ ลองชื่อถัดไป
            }
        }
        return null
    }

    /** คัดลอกรูปต้นฉบับจากเครื่องมาไว้ใน cache แบบไม่ย่อ ไม่บีบ */
    private fun copyOriginal(uri: Uri): File? = try {
        val out = File(cacheDir, "crop_source.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { input.copyTo(it) }
        }
        if (out.length() > 0) out else null
    } catch (e: Exception) {
        Log.w("ImportActivity", "คัดลอกรูปต้นฉบับไม่ได้", e)
        null
    }

    /**
     * ส่งรูปเดิมไปที่ /debug/crop (อยู่ข้าง ๆ /check-oy บนเซิร์ฟเวอร์เดียวกัน)
     * แล้วรับภาพ JPEG ที่ YOLO ครอบเฉพาะกรอบเลข อย. กลับมา
     * ไม่ได้ภาพ (ไม่เจอกรอบ / endpoint ไม่มี / ต่อไม่ได้) คืน null
     */
    private fun fetchCrop(checkUrl: HttpUrl, file: File): ByteArray? {
        return try {
            val url = checkUrl.resolve("debug/crop") ?: return null
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build()
            client.newCall(Request.Builder().url(url).post(body).build()).execute().use { r ->
                val type = r.header("Content-Type").orEmpty()
                if (!r.isSuccessful || !type.startsWith("image")) {
                    Log.w("ImportActivity", "/debug/crop ไม่ได้ภาพ HTTP ${r.code} $type")
                    return null
                }
                val bytes = r.body?.bytes() ?: return null
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                if (o.outWidth > 0) bytes else null
            }
        } catch (e: Exception) {
            Log.w("ImportActivity", "ขอภาพครอปจาก /debug/crop ไม่ได้", e)
            null
        }
    }

    /** คืน path ของภาพที่เก็บ กับค่าบอกว่าเป็นภาพครอปจริงหรือไม่ */
    private fun saveEvidence(cropB64: String?, cropBytes: ByteArray?, uploaded: File): Pair<String, Boolean> {
        if (cropBytes != null && cropBytes.isNotEmpty()) {
            try {
                return HistoryStore.saveBytes(this, cropBytes) to true
            } catch (e: Exception) {
                Log.w("ImportActivity", "บันทึกภาพครอปไม่ได้", e)
            }
        }
        if (!cropB64.isNullOrBlank()) {
            try {
                val raw = cropB64.substringAfter("base64,", cropB64)
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                if (o.outWidth > 0) return HistoryStore.saveBytes(this, bytes) to true
            } catch (e: Exception) {
                Log.w("ImportActivity", "ถอดภาพครอปไม่ได้ ใช้ภาพต้นฉบับแทน", e)
            }
        }
        return try {
            val o = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bmp = BitmapFactory.decodeFile(uploaded.absolutePath, o)
            if (bmp != null) HistoryStore.saveBitmap(this, bmp, 82) to false else "" to false
        } catch (e: Throwable) {
            "" to false
        }
    }

    private fun fileInfo(uri: Uri): Pair<String, Long?> {
        var name = "label.jpg"
        var size: Long? = null
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        } catch (e: Exception) {
            // ไม่รู้ชื่อไฟล์ก็ใช้ค่าเริ่มต้น
        }
        return name to size
    }

    private fun formatSize(b: Long): String = when {
        b >= 1024L * 1024L -> String.format("%.1f MB", b / (1024f * 1024f))
        b >= 1024L -> "${b / 1024L} KB"
        else -> "$b B"
    }
}

/** ตัวห่อไฟล์ที่รายงานว่าส่งไปแล้วกี่เปอร์เซ็นต์ */
private class ProgressBody(
    private val file: File,
    private val type: MediaType,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType = type
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1L).toFloat()
        var sent = 0L
        file.source().use { src ->
            val buf = Buffer()
            while (true) {
                val n = src.read(buf, 16_384L)
                if (n == -1L) break
                sink.write(buf, n)
                sent += n
                onProgress(sent / total)
            }
        }
    }
}