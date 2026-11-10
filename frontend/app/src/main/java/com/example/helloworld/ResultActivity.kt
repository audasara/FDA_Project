package com.example.helloworld

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "ENTRY_ID"
        private const val EXTRA_FROM_HISTORY = "FROM_HISTORY"

        fun open(ctx: Context, id: Long, fromHistory: Boolean): Intent =
            Intent(ctx, ResultActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_FROM_HISTORY, fromHistory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge(lightStatus = true)

        val fromHistory = intent.getBooleanExtra(EXTRA_FROM_HISTORY, false)
        val e = HistoryStore.get(this, intent.getLongExtra(EXTRA_ID, -1L))
        if (e == null) {
            Toast.makeText(this, "ไม่พบรายการนี้ในประวัติแล้ว", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(4), dpx(20), dpx(24))
        }

        root.addView(cropCard(e))
        root.addView(statusCard(e))
        root.addView(detailCard(e))

        root.addView(noteCard(Ico.INFO,
            "ข้อมูลอ้างอิงจากฐานข้อมูลสำนักงานคณะกรรมการอาหารและยา หากพบความผิดปกติแจ้งสายด่วน 1556",
            if (e.valid) P.slate else P.error, if (e.valid) P.skySoft else P.errorSoft, 12))

        // ---------- ปุ่ม ----------
        root.addView(pill("ตรวจรายการใหม่", Btn.PRIMARY, Ico.SCAN) {
            goTab(ImportActivity::class.java)
            finish()
        }.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(18) })

        if (fromHistory) {
            root.addView(pill("กลับไปที่ประวัติ", Btn.SECONDARY, Ico.CLOCK) { finish() }.view.apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(10)
            })
        } else {
            root.addView(pill("ดูประวัติทั้งหมด", Btn.SECONDARY, Ico.CLOCK) {
                goTab(HistoryActivity::class.java)
            }.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(10) })
        }
        if (!e.valid) {
            root.addView(pill("โทรแจ้งสายด่วน อย. 1556", Btn.DANGER, Ico.PHONE) { dialHotline() }.view.apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(10)
            })
        }

        setContentView(appStage(
            root,
            if (fromHistory) P.TAB_HISTORY else P.TAB_SCAN,
            topBar("ผลการตรวจสอบ", if (fromHistory) "จากประวัติ" else "ขั้นตอนที่ 2 จาก 2", back = true)
        ))

        // ---------- แบนเนอร์แจ้งผล ----------
        if (savedInstanceState == null) {
            window.decorView.post {
                when {
                    fromHistory -> showBanner(Banner.INFO, "เปิดจากประวัติ",
                        "ข้อมูลชุดนี้บันทึกไว้เมื่อ ${thaiDate(e.time)}")
                    e.valid -> showBanner(Banner.SUCCESS, "ตรวจสอบสำเร็จ",
                        "พบข้อมูลในฐานข้อมูล อย. และบันทึกลงประวัติแล้ว")
                    else -> showBanner(Banner.WARN, "ไม่พบเลขนี้ในฐานข้อมูล",
                        "บันทึกลงประวัติแล้ว ควรตรวจสอบสินค้าให้แน่ใจก่อนซื้อ", 5200L)
                }
            }
        }
    }

    // ================================================================ การ์ดภาพครอป

    private fun cropCard(e: HistoryStore.Entry) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = solidOf(P.white, 28, P.line)
        elevation = dpx(3).toFloat()
        shadow(a(P.navy, 0.4f))
        setPadding(dpx(16), dpx(16), dpx(16), dpx(18))
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(8))

        addView(LinearLayout(this@ResultActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icoView(Ico.SPARK, P.teal, 38, 9, solidOf(P.skySoft, 12)))
            addView(LinearLayout(this@ResultActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(11) }
                addView(txt(if (e.isCrop) "ภาพที่ AI ครอบได้" else "ภาพฉลากที่ใช้ตรวจ", 15.5f, P.ink, true))
                addView(txt(
                    if (e.isCrop) "YOLO ตรวจจับกรอบเลข อย. แล้วส่งให้ OCR อ่าน"
                    else "เซิร์ฟเวอร์ไม่ได้ส่งภาพครอปมา จึงแสดงภาพต้นฉบับ",
                    12.5f, P.muted
                ).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(1)) })
            })
            if (e.isCrop) addView(chipOf("YOLO", P.navy, P.skySoft))
        })

        val frame = CropFrameView(this@ResultActivity).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))
            contentDescription = "ภาพบริเวณเลข อย. ที่ครอบได้"
        }
        addView(frame)
        loadThumb(e.imagePath, 1200) { b ->
            frame.bitmap = b
            if (b != null) frame.postDelayed({ frame.playScan() }, 250)
        }

        // ---------- เลขที่อ่านได้ ----------
        addView(LinearLayout(this@ResultActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = solidOf(P.skyHi, 16)
            setPadding(dpx(16), dpx(12), dpx(10), dpx(12))
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
            addView(LinearLayout(this@ResultActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                addView(txt("เลขที่อ่านได้", 12f, P.muted, true))
                addView(txt(e.fdaNo, 23f, P.navy, true).apply {
                    letterSpacing = 0.05f
                    layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
                })
            })
            addView(circleBtn(Ico.COPY, 42, P.teal) { copyNumber(e.fdaNo) }.apply {
                contentDescription = "คัดลอกเลข อย."
            })
        })
    }

    // ================================================================ การ์ดสถานะ

    private fun statusCard(e: HistoryStore.Entry): View {
        val ok = e.valid
        val card = FrameLayout(this).apply {
            background = gradOf(
                if (ok) intArrayOf(P.navyHi, P.navy, P.tealDeep)
                else intArrayOf(Color.parseColor("#C0735E"), P.error, Color.parseColor("#8E3F2E")),
                26, GradientDrawable.Orientation.TL_BR, P.white, 3
            )
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dpx(8).toFloat()
            shadow(if (ok) P.navy else P.error)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))
        }
        card.addView(LoopArt(this, Color.WHITE, 0.4f), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(22), dpx(22), dpx(22), dpx(22))
        }
        val icon = icoView(if (ok) Ico.SUCCESS else Ico.ERROR, P.white, 54, 13,
            circleOf(a(P.white, 0.18f)), 2f).apply {
            scaleX = 0.5f
            scaleY = 0.5f
            alpha = 0f
        }
        inner.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icon)
            addView(View(this@ResultActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(statusPill(ok, onDark = true))
        })
        inner.addView(txt(if (ok) "มีทะเบียน อย. ในระบบ" else "ไม่พบทะเบียนในระบบ", 23f, P.white, true).apply {
            letterSpacing = -0.01f
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16))
        })

        val advice = if (ok) "เลขนี้ตรงกับข้อมูลในฐานข้อมูลของสำนักงานคณะกรรมการอาหารและยา"
        else "เลขอาจอ่านผิด หรือสินค้าไม่ได้ขึ้นทะเบียน ลองสแกนใหม่ให้ชัดขึ้น หรือแจ้งสายด่วน 1556"
        val msg = if (e.message.isNotBlank() && e.message != advice) "${e.message}\n$advice" else advice
        inner.addView(txt(msg, 13.5f, if (ok) P.sky else Color.parseColor("#F4D9D1")).apply {
            setLineSpacing(dpx(5).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })

        inner.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = solidOf(a(P.white, 0.14f), 100)
            setPadding(dpx(12), dpx(7), dpx(14), dpx(7))
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dpx(16) }
            addView(icoView(Ico.CLOCK, P.white, 16, 0))
            addView(txt(
                "ตรวจเมื่อ ${thaiDate(e.time)}" + if (e.count > 1) " · ตรวจแล้ว ${e.count} ครั้ง" else "",
                12f, P.white
            ).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dpx(7) }
            })
        })
        card.addView(inner)

        icon.post {
            icon.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(150)
                .setDuration(460).setInterpolator(OvershootInterpolator(2f)).start()
        }
        card.alpha = 0f
        card.translationY = dpx(16).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(380)
            .setInterpolator(DecelerateInterpolator()).start()
        return card
    }

    // ================================================================ การ์ดรายละเอียด

    private fun detailCard(e: HistoryStore.Entry) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = solidOf(P.white, 24, P.line)
        setPadding(dpx(18), dpx(18), dpx(18), dpx(18))
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))

        addView(LinearLayout(this@ResultActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@ResultActivity).apply {
                background = solidOf(P.teal, 2)
                layoutParams = LinearLayout.LayoutParams(dpx(4), dpx(18))
            })
            addView(txt("ข้อมูลผลิตภัณฑ์", 16f, P.ink, true).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(10) }
            })
            addView(LinearLayout(this@ResultActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = solidOf(P.successSoft, 100)
                setPadding(dpx(9), dpx(4), dpx(11), dpx(4))
                addView(icoView(Ico.CHECK, P.success, 13, 0, weight = 2.8f))
                addView(txt("บันทึกแล้ว", 11.5f, P.success, true).apply {
                    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dpx(4) }
                })
            })
        })

        var shown = 0
        fun add(icon: Int, label: String, value: String) {
            if (value.isBlank()) return
            addView(infoRow(icon, label, value, dpx(if (shown == 0) 16 else 14)))
            shown++
        }
        add(Ico.TAG, "ชื่อผลิตภัณฑ์", e.name)
        add(Ico.BUILDING, "ผู้รับอนุญาต / สถานที่ผลิต", e.company)
        add(Ico.BOX, "ประเภทใบอนุญาต", e.productType)
        add(Ico.PIN, "ที่ตั้ง", e.address)

        if (shown == 0) {
            addView(txt("ระบบอ่านเลขได้ แต่ไม่พบรายละเอียดผลิตภัณฑ์ในฐานข้อมูล", 13.5f, P.muted).apply {
                setLineSpacing(dpx(4).toFloat(), 1f)
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))
            })
        }
    }

    private fun copyNumber(no: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("เลข อย.", no))
        showBanner(Banner.INFO, "คัดลอกเลขแล้ว", no, 2500L)
    }
}