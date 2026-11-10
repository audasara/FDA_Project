package com.example.helloworld

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.LruCache
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.GregorianCalendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


object P {
    val navy = Color.parseColor("#2F4156")
    val teal = Color.parseColor("#567C8D")
    val sky = Color.parseColor("#C8D9E6")
    val beige = Color.parseColor("#F5EFEB")
    val white = Color.WHITE

    val navyDeep = Color.parseColor("#223041")
    val navyHi = Color.parseColor("#3D5470")
    val tealDeep = Color.parseColor("#466A7A")
    val tealSoft = Color.parseColor("#9DB6C3")
    val skySoft = Color.parseColor("#E3ECF3")
    val skyHi = Color.parseColor("#F2F6FA")
    val beigeDeep = Color.parseColor("#E7DDD5")
    val beigeInk = Color.parseColor("#6B5747")

    val paper = Color.parseColor("#FAF7F4")
    val line = Color.parseColor("#E4EAF0")
    val hairline = Color.parseColor("#D2DDE7")

    val ink = Color.parseColor("#1F2B38")
    val slate = Color.parseColor("#4B5D6E")
    val muted = Color.parseColor("#728291")
    val faint = Color.parseColor("#A7B3BE")
    val disabled = Color.parseColor("#C3CFD9")

    val success = Color.parseColor("#3E8A6E")
    val successSoft = Color.parseColor("#E3F1EB")
    val warn = Color.parseColor("#B07C25")
    val warnSoft = Color.parseColor("#FAF0DC")
    val error = Color.parseColor("#B4553F")
    val errorSoft = Color.parseColor("#F9E8E3")
    val info = Color.parseColor("#567C8D")
    val infoSoft = Color.parseColor("#E2EDF2")

    const val TAB_HOME = 0
    const val TAB_HISTORY = 1
    const val TAB_SCAN = 2
    const val TAB_PROFILE = 3
    const val TAB_NONE = -1
}

internal fun a(c: Int, alpha: Float): Int =
    Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))

class Ico(private val kind: Int, tint: Int, weight: Float = 1.8f) : Drawable() {

    companion object {
        const val HOME = 0
        const val SCAN = 1
        const val CLOCK = 2
        const val USER = 3
        const val LOCK = 4
        const val EYE = 5
        const val EYE_OFF = 6
        const val SHIELD = 7
        const val CHECK = 8
        const val UPLOAD = 9
        const val CLOSE = 10
        const val PLUS = 11
        const val INFO = 12
        const val WARN = 13
        const val ERROR = 14
        const val SUCCESS = 15
        const val PHONE = 16
        const val IMAGE = 17
        const val MAIL = 18
        const val SEARCH = 19
        const val TRASH = 20
        const val TAG = 21
        const val BUILDING = 22
        const val BOX = 23
        const val PIN = 24
        const val BACK = 25
        const val CHEVRON = 26
        const val SPARK = 27
        const val COPY = 28
        const val ARROW = 29
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = weight
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = tint
    }
    private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = tint
    }
    private val path = Path()

    private fun Canvas.poly(vararg xy: Float) {
        path.reset()
        path.moveTo(xy[0], xy[1])
        var i = 2
        while (i + 1 < xy.size) {
            path.lineTo(xy[i], xy[i + 1])
            i += 2
        }
        drawPath(path, p)
    }

    private fun Canvas.dot(x: Float, y: Float) {
        drawCircle(x, y, 1.15f, f)
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()) / 24f
        if (s <= 0f) return

        canvas.save()
        canvas.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f)
        canvas.scale(s, s)

        with(canvas) {
            when (kind) {
                HOME -> {
                    poly(3f, 10.5f, 12f, 3.5f, 21f, 10.5f)
                    poly(5.2f, 9.5f, 5.2f, 20.5f, 18.8f, 20.5f, 18.8f, 9.5f)
                    poly(9.8f, 20.5f, 9.8f, 14.5f, 14.2f, 14.5f, 14.2f, 20.5f)
                }
                SCAN -> {
                    poly(3.5f, 8.5f, 3.5f, 4.5f, 7.5f, 4.5f)
                    poly(16.5f, 4.5f, 20.5f, 4.5f, 20.5f, 8.5f)
                    poly(20.5f, 15.5f, 20.5f, 19.5f, 16.5f, 19.5f)
                    poly(7.5f, 19.5f, 3.5f, 19.5f, 3.5f, 15.5f)
                    poly(6.5f, 12f, 17.5f, 12f)
                }
                CLOCK -> {
                    drawCircle(12f, 12f, 8.6f, p)
                    poly(12f, 7f, 12f, 12f, 15.8f, 14f)
                }
                USER -> {
                    drawCircle(12f, 8f, 3.9f, p)
                    drawArc(RectF(4.6f, 13.4f, 19.4f, 26.6f), 180f, 180f, false, p)
                }
                LOCK -> {
                    drawRoundRect(RectF(4.5f, 10.5f, 19.5f, 20.5f), 2.5f, 2.5f, p)
                    drawArc(RectF(7.8f, 3.5f, 16.2f, 12f), 180f, 180f, false, p)
                    poly(7.8f, 7.75f, 7.8f, 10.5f)
                    poly(16.2f, 7.75f, 16.2f, 10.5f)
                }
                EYE, EYE_OFF -> {
                    path.reset()
                    path.moveTo(2.6f, 12f)
                    path.quadTo(12f, 4.4f, 21.4f, 12f)
                    path.quadTo(12f, 19.6f, 2.6f, 12f)
                    path.close()
                    drawPath(path, p)
                    drawCircle(12f, 12f, 2.9f, p)
                    if (kind == EYE_OFF) poly(4.5f, 20f, 19.5f, 4f)
                }
                SHIELD -> {
                    path.reset()
                    path.moveTo(12f, 3f)
                    path.lineTo(19.5f, 6.4f)
                    path.lineTo(19.5f, 11.5f)
                    path.cubicTo(19.5f, 16f, 16.4f, 19.8f, 12f, 21f)
                    path.cubicTo(7.6f, 19.8f, 4.5f, 16f, 4.5f, 11.5f)
                    path.lineTo(4.5f, 6.4f)
                    path.close()
                    drawPath(path, p)
                    poly(9f, 12.2f, 11.2f, 14.4f, 15.4f, 10f)
                }
                CHECK -> poly(4.5f, 12.5f, 9.5f, 17.5f, 19.5f, 6.5f)
                UPLOAD -> {
                    poly(12f, 15f, 12f, 4f)
                    poly(7.5f, 8.5f, 12f, 4f, 16.5f, 8.5f)
                    path.reset()
                    path.moveTo(4f, 14f)
                    path.lineTo(4f, 17.5f)
                    path.quadTo(4f, 20f, 6.5f, 20f)
                    path.lineTo(17.5f, 20f)
                    path.quadTo(20f, 20f, 20f, 17.5f)
                    path.lineTo(20f, 14f)
                    drawPath(path, p)
                }
                CLOSE -> {
                    poly(6.5f, 6.5f, 17.5f, 17.5f)
                    poly(17.5f, 6.5f, 6.5f, 17.5f)
                }
                PLUS -> {
                    poly(12f, 5f, 12f, 19f)
                    poly(5f, 12f, 19f, 12f)
                }
                INFO -> {
                    drawCircle(12f, 12f, 9f, p)
                    poly(12f, 11f, 12f, 16.5f)
                    dot(12f, 7.8f)
                }
                WARN -> {
                    poly(12f, 3.8f, 21f, 19.6f, 3f, 19.6f, 12f, 3.8f)
                    poly(12f, 9.5f, 12f, 13.8f)
                    dot(12f, 16.6f)
                }
                ERROR -> {
                    drawCircle(12f, 12f, 9f, p)
                    poly(12f, 7.5f, 12f, 12.8f)
                    dot(12f, 16.2f)
                }
                SUCCESS -> {
                    drawCircle(12f, 12f, 9f, p)
                    poly(8f, 12.3f, 10.9f, 15.1f, 16.2f, 9.4f)
                }
                PHONE -> {
                    path.reset()
                    path.moveTo(5f, 4f)
                    path.lineTo(8.5f, 4f)
                    path.lineTo(10f, 8f)
                    path.lineTo(8f, 9.5f)
                    path.cubicTo(9f, 12f, 12f, 15f, 14.5f, 16f)
                    path.lineTo(16f, 14f)
                    path.lineTo(20f, 15.5f)
                    path.lineTo(20f, 19f)
                    path.cubicTo(20f, 20f, 19f, 21f, 18f, 20.8f)
                    path.cubicTo(10f, 20f, 4f, 14f, 3.2f, 6f)
                    path.cubicTo(3f, 5f, 4f, 4f, 5f, 4f)
                    path.close()
                    drawPath(path, p)
                }
                IMAGE -> {
                    drawRoundRect(RectF(3.5f, 4.5f, 20.5f, 19.5f), 2.5f, 2.5f, p)
                    drawCircle(9f, 9.5f, 1.7f, p)
                    poly(3.8f, 17f, 9f, 12.5f, 13f, 16f, 15.8f, 13.6f, 20.2f, 17.6f)
                }
                MAIL -> {
                    drawRoundRect(RectF(3f, 5.5f, 21f, 18.5f), 2.5f, 2.5f, p)
                    poly(3.6f, 7f, 12f, 13f, 20.4f, 7f)
                }
                SEARCH -> {
                    drawCircle(10.5f, 10.5f, 6.5f, p)
                    poly(15.3f, 15.3f, 20.5f, 20.5f)
                }
                TRASH -> {
                    poly(4f, 6.5f, 20f, 6.5f)
                    poly(9f, 6.5f, 9f, 4f, 15f, 4f, 15f, 6.5f)
                    poly(6f, 6.5f, 7f, 20f, 17f, 20f, 18f, 6.5f)
                    poly(10f, 10f, 10f, 16.5f)
                    poly(14f, 10f, 14f, 16.5f)
                }
                TAG -> {
                    poly(3.5f, 3.5f, 11.5f, 3.5f, 20.5f, 12.5f, 12.5f, 20.5f, 3.5f, 11.5f, 3.5f, 3.5f)
                    drawCircle(8f, 8f, 1.4f, p)
                }
                BUILDING -> {
                    drawRect(RectF(4.5f, 3.5f, 14.5f, 20.5f), p)
                    poly(14.5f, 9.5f, 19.5f, 9.5f, 19.5f, 20.5f)
                    poly(3f, 20.5f, 21f, 20.5f)
                    poly(7.5f, 7.5f, 8.5f, 7.5f)
                    poly(10.5f, 7.5f, 11.5f, 7.5f)
                    poly(7.5f, 11f, 8.5f, 11f)
                    poly(10.5f, 11f, 11.5f, 11f)
                    poly(7.5f, 14.5f, 8.5f, 14.5f)
                    poly(10.5f, 14.5f, 11.5f, 14.5f)
                }
                BOX -> {
                    poly(12f, 3f, 20.5f, 7.5f, 20.5f, 16.5f, 12f, 21f, 3.5f, 16.5f, 3.5f, 7.5f, 12f, 3f)
                    poly(3.5f, 7.5f, 12f, 12f, 20.5f, 7.5f)
                    poly(12f, 12f, 12f, 21f)
                }
                PIN -> {
                    path.reset()
                    path.moveTo(12f, 21f)
                    path.cubicTo(8f, 17f, 5f, 13.5f, 5f, 9.8f)
                    path.cubicTo(5f, 5.8f, 8.1f, 3f, 12f, 3f)
                    path.cubicTo(15.9f, 3f, 19f, 5.8f, 19f, 9.8f)
                    path.cubicTo(19f, 13.5f, 16f, 17f, 12f, 21f)
                    path.close()
                    drawPath(path, p)
                    drawCircle(12f, 9.8f, 2.6f, p)
                }
                BACK -> poly(15f, 5f, 8f, 12f, 15f, 19f)
                CHEVRON -> poly(9.5f, 5.5f, 16f, 12f, 9.5f, 18.5f)
                SPARK -> {
                    path.reset()
                    path.moveTo(11f, 3f)
                    path.quadTo(12f, 11f, 19f, 12f)
                    path.quadTo(12f, 13f, 11f, 21f)
                    path.quadTo(10f, 13f, 3f, 12f)
                    path.quadTo(10f, 11f, 11f, 3f)
                    path.close()
                    drawPath(path, p)
                    poly(18.5f, 3.5f, 18.5f, 7.5f)
                    poly(16.5f, 5.5f, 20.5f, 5.5f)
                }
                COPY -> {
                    drawRoundRect(RectF(8.5f, 8.5f, 20f, 20f), 2.2f, 2.2f, p)
                    path.reset()
                    path.moveTo(15.5f, 8.5f)
                    path.lineTo(15.5f, 6f)
                    path.quadTo(15.5f, 4f, 13.5f, 4f)
                    path.lineTo(6f, 4f)
                    path.quadTo(4f, 4f, 4f, 6f)
                    path.lineTo(4f, 13.5f)
                    path.quadTo(4f, 15.5f, 6f, 15.5f)
                    path.lineTo(8.5f, 15.5f)
                    drawPath(path, p)
                }
                ARROW -> {
                    poly(4.5f, 12f, 19.5f, 12f)
                    poly(13.5f, 6f, 19.5f, 12f, 13.5f, 18f)
                }
            }
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        p.alpha = alpha
        f.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        p.colorFilter = cf
        f.colorFilter = cf
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = 48
    override fun getIntrinsicHeight() = 48
}

// ========================= หน่วยวัดและฟอนต์ =========================

internal fun Context.dpx(v: Int) = (v * resources.displayMetrics.density).toInt()

private val fontCache = HashMap<Boolean, Typeface>()

internal fun Context.appFont(bold: Boolean): Typeface = fontCache.getOrPut(bold) {
    val name = if (bold) "prompt_semibold" else "prompt_regular"
    val fallback = Typeface.defaultFromStyle(if (bold) Typeface.BOLD else Typeface.NORMAL)
    try {
        val id = resources.getIdentifier(name, "font", packageName)
        if (id != 0) ResourcesCompat.getFont(this, id) ?: fallback else fallback
    } catch (e: Exception) {
        fallback
    }
}

internal fun Context.txt(s: CharSequence, size: Float, color: Int, bold: Boolean = false) =
    TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color)
        typeface = appFont(bold)
    }

internal fun lpOf(w: Int, h: Int, top: Int = 0) =
    LinearLayout.LayoutParams(w, h).apply { topMargin = top }

internal fun View.shadow(c: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        outlineSpotShadowColor = c
        outlineAmbientShadowColor = c
    }
}

// ========================= พื้นหลังสำเร็จรูป =========================

internal fun Context.solidOf(fill: Int, radius: Int, stroke: Int? = null, strokeW: Int = 1) =
    GradientDrawable().apply {
        cornerRadius = dpx(radius).toFloat()
        setColor(fill)
        stroke?.let { setStroke(dpx(strokeW), it) }
    }

internal fun Context.gradOf(
    colors: IntArray,
    radius: Int,
    orient: GradientDrawable.Orientation = GradientDrawable.Orientation.TOP_BOTTOM,
    stroke: Int? = null,
    strokeW: Int = 1
) = GradientDrawable(orient, colors).apply {
    cornerRadius = dpx(radius).toFloat()
    stroke?.let { setStroke(dpx(strokeW), it) }
}

internal fun Context.circleOf(fill: Int, stroke: Int? = null, strokeW: Int = 1) =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        stroke?.let { setStroke(dpx(strokeW), it) }
    }

internal fun Context.topRounded(fill: Int, radius: Int) = GradientDrawable().apply {
    val r = dpx(radius).toFloat()
    cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
    setColor(fill)
}

/** กรอบเส้นประแบบพื้นที่วางไฟล์ */
internal fun Context.dashedOf(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
    cornerRadius = dpx(radius).toFloat()
    setColor(fill)
    setStroke(dpx(2), stroke, dpx(7).toFloat(), dpx(6).toFloat())
}

internal fun Context.rippleOf(content: Drawable, rippleColor: Int, radius: Int): Drawable {
    val mask = GradientDrawable().apply {
        cornerRadius = dpx(radius).toFloat()
        setColor(Color.WHITE)
    }
    return RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask)
}

// ========================= ชิ้นส่วนเล็ก =========================

internal fun Context.icoView(
    kind: Int, tint: Int, box: Int, pad: Int,
    bg: Drawable? = null, weight: Float = 1.8f
) = ImageView(this).apply {
    setImageDrawable(Ico(kind, tint, weight))
    scaleType = ImageView.ScaleType.FIT_CENTER
    background = bg
    setPadding(dpx(pad), dpx(pad), dpx(pad), dpx(pad))
    layoutParams = LinearLayout.LayoutParams(dpx(box), dpx(box))
}

internal fun Context.chipOf(s: String, fg: Int, bg: Int, stroke: Int? = null) =
    txt(s, 12f, fg, true).apply {
        background = solidOf(bg, 100, stroke)
        setPadding(dpx(12), dpx(6), dpx(12), dpx(6))
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

/** ป้ายสถานะ พบทะเบียน / ไม่พบทะเบียน */
internal fun Context.statusPill(valid: Boolean, onDark: Boolean = false) = LinearLayout(this).apply {
    gravity = Gravity.CENTER_VERTICAL
    val fg = if (valid) P.success else P.error
    background = solidOf(
        if (onDark) P.white else if (valid) P.successSoft else P.errorSoft, 100
    )
    setPadding(dpx(10), dpx(5), dpx(12), dpx(5))
    addView(icoView(if (valid) Ico.CHECK else Ico.CLOSE, fg, 14, 0, weight = 2.8f))
    addView(txt(if (valid) "พบทะเบียน" else "ไม่พบทะเบียน", 11.5f, fg, true).apply {
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dpx(5) }
    })
    layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
}

/** พาดหัวสองน้ำหนัก บรรทัดบนบาง บรรทัดล่างหนา */
internal fun Context.headline(light: String, bold: String, size: Float = 30f, top: Int = 10) =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
        addView(txt(light, size, P.teal).apply { letterSpacing = -0.015f })
        addView(txt(bold, size, P.navy, true).apply {
            letterSpacing = -0.015f
            layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, -dpx(3))
        })
    }

internal fun Context.sectionHead(
    title: String, top: Int,
    action: String? = null, onAction: (() -> Unit)? = null
) = LinearLayout(this).apply {
    gravity = Gravity.CENTER_VERTICAL
    layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
    addView(txt(title, 16.5f, P.ink, true).apply {
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
    })
    if (action != null) {
        addView(txt(action, 13.5f, P.teal, true).apply {
            setPadding(dpx(10), dpx(6), dpx(4), dpx(6))
            isClickable = true
            background = rippleOf(solidOf(Color.TRANSPARENT, 100), a(P.teal, 0.12f), 100)
            setOnClickListener { onAction?.invoke() }
        })
    }
}

/** แถวข้อมูลผลิตภัณฑ์ ไอคอนซ้าย ป้ายกำกับเล็ก ค่าข้อมูลใหญ่ */
internal fun Context.infoRow(icon: Int, label: String, value: String, top: Int) =
    LinearLayout(this).apply {
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, top)
        addView(icoView(icon, P.teal, 40, 10, solidOf(P.skySoft, 12)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(12) }
            addView(txt(label, 12f, P.muted, true))
            addView(txt(value, 15f, P.ink, true).apply {
                setLineSpacing(dpx(4).toFloat(), 1f)
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
            })
        })
    }

internal fun Context.statTile(value: String, label: String, fg: Int, bg: Int) =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = solidOf(bg, 18)
        setPadding(dpx(8), dpx(14), dpx(8), dpx(14))
        addView(txt(value, 22f, fg, true))
        addView(txt(label, 12f, fg).apply {
            alpha = 0.85f
            gravity = Gravity.CENTER
            layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, dpx(1))
        })
    }

internal fun Context.statsRow(total: Int, pass: Int, fail: Int, top: Int) = LinearLayout(this).apply {
    layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
    addView(statTile("$total", "ตรวจทั้งหมด", P.navy, P.skySoft),
        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    addView(statTile("$pass", "พบทะเบียน", P.success, P.successSoft),
        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(10) })
    addView(statTile("$fail", "ไม่พบ", P.error, P.errorSoft),
        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(10) })
}

internal fun Context.menuRow(icon: Int, title: String, sub: String?, top: Int, onClick: () -> Unit) =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = rippleOf(solidOf(P.white, 20, P.line), a(P.navy, 0.08f), 20)
        setPadding(dpx(14), dpx(14), dpx(14), dpx(14))
        isClickable = true
        isFocusable = true
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
        setOnClickListener { onClick() }
        addView(icoView(icon, P.teal, 44, 11, circleOf(P.skySoft)))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(13) }
            addView(txt(title, 15f, P.ink, true))
            if (sub != null) addView(txt(sub, 12.5f, P.muted).apply {
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
            })
        })
        addView(icoView(Ico.CHEVRON, P.tealSoft, 20, 0))
    }

/** การ์ดหมายเหตุเล็ก ไอคอนซ้าย ข้อความขวา */
internal fun Context.noteCard(icon: Int, text: String, fg: Int, bg: Int, top: Int, stroke: Int? = null) =
    LinearLayout(this).apply {
        background = solidOf(bg, 18, stroke)
        setPadding(dpx(14), dpx(13), dpx(14), dpx(13))
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
        addView(icoView(icon, fg, 20, 0))
        addView(txt(text, 12.5f, fg).apply {
            setLineSpacing(dpx(4).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(10) }
        })
    }

internal fun Context.circleBtn(icon: Int, size: Int = 42, tint: Int = P.slate, onClick: () -> Unit) =
    ImageView(this).apply {
        setImageDrawable(Ico(icon, tint, 1.9f))
        scaleType = ImageView.ScaleType.FIT_CENTER
        background = rippleOf(circleOf(P.white, P.hairline), a(P.navy, 0.1f), 100)
        val pad = (size * 0.27f).toInt()
        setPadding(dpx(pad), dpx(pad), dpx(pad), dpx(pad))
        isClickable = true
        isFocusable = true
        layoutParams = LinearLayout.LayoutParams(dpx(size), dpx(size))
        setOnClickListener { onClick() }
    }

internal fun Context.roundImage(radius: Int, bg: Int = P.skySoft) = ImageView(this).apply {
    scaleType = ImageView.ScaleType.FIT_CENTER
    background = solidOf(bg, radius)
    clipToOutline = true
    outlineProvider = ViewOutlineProvider.BACKGROUND
}

internal fun Context.linkRow(lead: String, action: String, onClick: () -> Unit) =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(22))
        addView(txt(lead, 14f, P.muted))
        addView(txt(action, 14f, P.teal, true).apply {
            isClickable = true
            background = rippleOf(solidOf(Color.TRANSPARENT, 100), a(P.teal, 0.12f), 100)
            setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
            setOnClickListener { onClick() }
        })
    }

internal fun Context.fieldLabel(s: String, top: Int = 20) = txt(s, 12.5f, P.slate, true).apply {
    layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
}

// ========================= ปุ่มทรงแคปซูล =========================

object Btn {
    const val PRIMARY = 0
    const val SECONDARY = 1
    const val SOFT = 2
    const val DANGER = 3
    const val LIGHT = 4
}

internal class Pill(
    val view: LinearLayout,
    val label: TextView,
    val icon: ImageView?,
    val iconKind: Int?
)

internal fun Context.pill(
    text: String,
    style: Int = Btn.PRIMARY,
    icon: Int? = null,
    height: Int = 56,
    onClick: () -> Unit
): Pill {
    val row = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        setPadding(dpx(18), 0, dpx(18), 0)
        layoutParams = lpOf(MATCH_PARENT, dpx(height), 0)
    }
    val iv = if (icon != null) ImageView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dpx(20), dpx(20)).apply { marginEnd = dpx(10) }
    } else null
    if (iv != null) row.addView(iv)
    val label = txt(text, 15.5f, P.white, true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    row.addView(label)
    val p = Pill(row, label, iv, icon)
    row.setOnClickListener { if (row.isEnabled) onClick() }
    stylePill(p, style, true)
    return p
}

internal fun Context.stylePill(p: Pill, style: Int, enabled: Boolean, text: String? = null) {
    if (text != null) p.label.text = text
    val spec: Triple<Int, Int, Int?> = when {
        !enabled && style == Btn.PRIMARY -> Triple(P.disabled, P.white, null)
        !enabled -> Triple(P.white, P.faint, P.line)
        style == Btn.SECONDARY -> Triple(P.white, P.navy, P.hairline)
        style == Btn.SOFT -> Triple(P.skySoft, P.navy, null)
        style == Btn.DANGER -> Triple(P.errorSoft, P.error, null)
        style == Btn.LIGHT -> Triple(P.white, P.navy, null)
        else -> Triple(P.navy, P.white, null)
    }
    val (fill, fg, stroke) = spec
    p.view.isEnabled = enabled
    p.view.background = rippleOf(
        solidOf(fill, 100, stroke),
        if (style == Btn.PRIMARY) a(P.white, 0.22f) else a(P.navy, 0.1f), 100
    )
    p.label.setTextColor(fg)
    val k = p.iconKind
    if (k != null) p.icon?.setImageDrawable(Ico(k, fg, 2f))
    p.view.elevation = if (enabled && style == Btn.PRIMARY) dpx(5).toFloat() else 0f
    p.view.shadow(P.navy)
}

// ========================= ช่องกรอกทรงแคปซูล =========================

internal class Field(
    val view: LinearLayout,
    val frame: FrameLayout,
    val edit: EditText,
    val error: TextView
) {
    var focused = false
    var hasError = false
    val value: String get() = edit.text?.toString().orEmpty()
}

internal fun Context.pillField(
    hint: String, icon: Int?, inputType: Int, password: Boolean = false
): Field {
    val frame = FrameLayout(this).apply { layoutParams = lpOf(MATCH_PARENT, dpx(56), dpx(8)) }

    val edit = EditText(this).apply {
        this.inputType = inputType
        typeface = appFont(false)
        this.hint = hint
        setHintTextColor(P.faint)
        setTextColor(P.ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
        maxLines = 1
        background = null
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dpx(if (icon != null) 50 else 22), 0, dpx(if (password) 56 else 22), 0)
        if (password) transformationMethod = PasswordTransformationMethod.getInstance()
    }
    frame.addView(edit, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

    if (icon != null) {
        frame.addView(ImageView(this).apply { setImageDrawable(Ico(icon, P.muted, 1.8f)) },
            FrameLayout.LayoutParams(dpx(20), dpx(20), Gravity.START or Gravity.CENTER_VERTICAL)
                .apply { marginStart = dpx(20) })
    }

    if (password) {
        var shown = false
        val eye = ImageView(this).apply {
            setImageDrawable(Ico(Ico.EYE, P.muted, 1.7f))
            contentDescription = "แสดงรหัสผ่าน"
            isClickable = true
            isFocusable = true
            background = rippleOf(solidOf(Color.TRANSPARENT, 100), a(P.navy, 0.08f), 100)
            setPadding(dpx(11), dpx(11), dpx(11), dpx(11))
            setOnClickListener {
                shown = !shown
                edit.transformationMethod = if (shown) null else PasswordTransformationMethod.getInstance()
                edit.setSelection(edit.text?.length ?: 0)
                setImageDrawable(Ico(if (shown) Ico.EYE_OFF else Ico.EYE, if (shown) P.teal else P.muted, 1.7f))
            }
        }
        frame.addView(eye, FrameLayout.LayoutParams(dpx(44), dpx(44), Gravity.END or Gravity.CENTER_VERTICAL)
            .apply { marginEnd = dpx(6) })
    }

    val err = txt("", 12.5f, P.error).apply {
        visibility = View.GONE
        setPadding(dpx(20), dpx(6), dpx(12), 0)
    }
    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, 0)
        addView(frame)
        addView(err)
    }
    val field = Field(box, frame, edit, err)

    edit.setOnFocusChangeListener { _, has ->
        field.focused = has
        paintField(field)
    }
    edit.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (field.hasError) setFieldError(field, null)
        }
    })
    paintField(field)
    return field
}

internal fun Context.paintField(f: Field) {
    val stroke = when {
        f.hasError -> P.error
        f.focused -> P.teal
        else -> P.hairline
    }
    f.frame.background = solidOf(
        if (f.focused) P.white else P.skyHi, 100, stroke,
        if (f.focused || f.hasError) 2 else 1
    )
}

internal fun Context.setFieldError(f: Field, msg: String?) {
    f.hasError = msg != null
    f.error.text = msg.orEmpty()
    f.error.visibility = if (msg == null) View.GONE else View.VISIBLE
    paintField(f)
}

internal fun EditText.onText(block: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = block(s?.toString().orEmpty())
    })
}

// ========================= แบนเนอร์แจ้งผล แบบการ์ดลอยด้านบน =========================

object Banner {
    const val SUCCESS = 0
    const val INFO = 1
    const val WARN = 2
    const val ERROR = 3
}

private const val BANNER_TAG = "fda_banner"

internal fun Activity.showBanner(kind: Int, title: String, message: String, durationMs: Long = 4200L) {
    val host = findViewById<FrameLayout>(android.R.id.content) ?: return
    host.findViewWithTag<View>(BANNER_TAG)?.let { host.removeView(it) }

    val tone: Int
    val soft: Int
    val icon: Int
    when (kind) {
        Banner.SUCCESS -> { tone = P.success; soft = P.successSoft; icon = Ico.SUCCESS }
        Banner.WARN -> { tone = P.warn; soft = P.warnSoft; icon = Ico.WARN }
        Banner.ERROR -> { tone = P.error; soft = P.errorSoft; icon = Ico.ERROR }
        else -> { tone = P.info; soft = P.infoSoft; icon = Ico.INFO }
    }

    val card = LinearLayout(this).apply {
        tag = BANNER_TAG
        gravity = Gravity.TOP
        background = LayerDrawable(arrayOf<Drawable>(
            solidOf(P.white, 20),
            gradOf(intArrayOf(soft, a(P.white, 0f), a(P.white, 0f)), 20,
                GradientDrawable.Orientation.LEFT_RIGHT)
        ))
        elevation = dpx(14).toFloat()
        shadow(a(P.navy, 0.6f))
        setPadding(dpx(14), dpx(14), dpx(8), dpx(14))
        isClickable = true
    }
    card.addView(icoView(icon, tone, 42, 10, solidOf(P.white, 13, P.line)))
    card.addView(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            leftMargin = dpx(12)
            topMargin = dpx(1)
        }
        addView(txt(title, 15.5f, P.ink, true))
        addView(txt(message, 13f, P.slate).apply {
            setLineSpacing(dpx(3).toFloat(), 1f)
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
        })
    })
    val close = icoView(Ico.CLOSE, P.faint, 34, 8).apply {
        isClickable = true
        contentDescription = "ปิด"
        background = rippleOf(solidOf(Color.TRANSPARENT, 100), a(P.navy, 0.08f), 100)
    }
    card.addView(close)

    val topInset = ViewCompat.getRootWindowInsets(host)
        ?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
    host.addView(card, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP).apply {
        setMargins(dpx(16), topInset + dpx(10), dpx(16), 0)
    })

    fun dismiss() {
        if (card.parent == null || card.tag != BANNER_TAG) return
        card.tag = null
        card.animate().translationY(-dpx(30).toFloat()).alpha(0f).setDuration(200)
            .withEndAction { (card.parent as? ViewGroup)?.removeView(card) }
            .start()
    }
    close.setOnClickListener { dismiss() }
    card.setOnClickListener { dismiss() }

    card.translationY = -dpx(40).toFloat()
    card.alpha = 0f
    card.animate().translationY(0f).alpha(1f).setDuration(320)
        .setInterpolator(DecelerateInterpolator()).start()
    card.announceForAccessibility("$title $message")
    card.postDelayed({ dismiss() }, durationMs)
}

// ========================= ขอบจอ แถบสถานะ คีย์บอร์ด =========================

/** วาดใต้แถบสถานะ แล้วเว้นระยะเองจาก insets ใช้ได้ทั้ง Android 15 ที่บังคับ edge-to-edge และรุ่นเก่า */
internal fun Activity.edgeToEdge(lightStatus: Boolean = true) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightStatus
        isAppearanceLightNavigationBars = true
    }
    @Suppress("DEPRECATION")
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

internal fun View.onInsets(block: (top: Int, nav: Int, ime: Int) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, ins ->
        val sys = ins.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = ins.getInsets(WindowInsetsCompat.Type.ime())
        block(sys.top, sys.bottom, ime.bottom)
        ins
    }
    ViewCompat.requestApplyInsets(this)
}

/** เลื่อนให้ช่องที่กำลังพิมพ์โผล่พ้นคีย์บอร์ด */
internal fun ScrollView.revealFocus(ime: Int) {
    if (ime <= 0) return
    val f = findFocus() ?: return
    post {
        val r = Rect()
        f.getDrawingRect(r)
        offsetDescendantRectToMyCoords(f, r)
        val visibleBottom = scrollY + height - ime - context.dpx(24)
        if (r.bottom > visibleBottom) smoothScrollBy(0, r.bottom - visibleBottom)
    }
}

// ========================= แถบบน =========================

internal fun Activity.topBar(title: String, badge: String? = null, back: Boolean = false): View =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dpx(20), dpx(10), dpx(20), dpx(8))

        if (back) {
            addView(circleBtn(Ico.BACK) { finish() }.apply { contentDescription = "ย้อนกลับ" })
        } else {
            addView(icoView(Ico.SHIELD, P.white, 36, 8,
                gradOf(intArrayOf(P.navyHi, P.navy), 12, GradientDrawable.Orientation.TL_BR), 2f))
        }
        addView(txt(title, 14.5f, P.navy, true).apply {
            letterSpacing = 0.02f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(12) }
        })
        if (badge != null) addView(chipOf(badge, P.beigeInk, P.beige, P.beigeDeep))
    }

// ========================= แถบล่าง ปุ่มสแกนลอยตรงกลาง =========================

private class TabItem(val label: String, val icon: Int, val id: Int, val go: Activity.() -> Unit)

internal fun Activity.goTab(target: Class<*>, extra: Intent.() -> Unit = {}) {
    try {
        startActivity(Intent(this, target).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            extra()
        })
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    } catch (e: Exception) {
        showBanner(Banner.ERROR, "เปิดหน้านี้ไม่ได้",
            "ตรวจว่าได้ประกาศ ${target.simpleName} ใน AndroidManifest.xml แล้ว")
    }
}

internal fun Activity.dialHotline() {
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1556")))
    } catch (e: ActivityNotFoundException) {
        showBanner(Banner.INFO, "สายด่วน อย. 1556", "เครื่องนี้ไม่มีแอปโทรศัพท์ จดเบอร์ 1556 ไว้โทรภายหลังได้")
    }
}

private fun Activity.tabCell(t: TabItem, on: Boolean) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    isClickable = true
    isFocusable = true
    contentDescription = t.label
    background = rippleOf(solidOf(Color.TRANSPARENT, 20), a(P.navy, 0.08f), 20)
    layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)

    addView(FrameLayout(this@tabCell).apply {
        background = if (on) solidOf(P.skySoft, 100) else null
        addView(ImageView(this@tabCell).apply {
            setImageDrawable(Ico(t.icon, if (on) P.navy else P.muted, if (on) 2.1f else 1.8f))
        }, FrameLayout.LayoutParams(dpx(22), dpx(22), Gravity.CENTER))
        layoutParams = LinearLayout.LayoutParams(dpx(54), dpx(30))
    })
    addView(txt(t.label, 11.5f, if (on) P.navy else P.muted, on).apply {
        layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, dpx(2))
    })
    setOnClickListener { if (!on) t.go(this@tabCell) }
}

internal fun Activity.bottomBar(active: Int): View {
    val holder = FrameLayout(this).apply {
        clipChildren = false
        clipToPadding = false
    }

    val bar = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = solidOf(P.white, 28, P.line)
        elevation = dpx(12).toFloat()
        shadow(P.navy)
        setPadding(dpx(4), 0, dpx(4), 0)
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, dpx(68), Gravity.BOTTOM)
    }

    val left = listOf(
        TabItem("หน้าแรก", Ico.HOME, P.TAB_HOME) { goTab(MainActivity::class.java) },
        TabItem("ประวัติ", Ico.CLOCK, P.TAB_HISTORY) { goTab(HistoryActivity::class.java) }
    )
    val right = listOf(
        TabItem("สายด่วน", Ico.PHONE, P.TAB_NONE) { dialHotline() },
        TabItem("บัญชี", Ico.USER, P.TAB_PROFILE) {
            goTab(LoginActivity::class.java) { putExtra("FROM_TAB", true) }
        }
    )
    left.forEach { bar.addView(tabCell(it, it.id == active)) }

    val scanOn = active == P.TAB_SCAN
    bar.addView(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        setPadding(0, 0, 0, dpx(9))
        addView(txt("สแกน", 11.5f, if (scanOn) P.navy else P.muted, scanOn))
        layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
    })
    right.forEach { bar.addView(tabCell(it, it.id == active && it.id != P.TAB_NONE)) }
    holder.addView(bar)

    val fab = FrameLayout(this).apply {
        background = rippleOf(
            GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(P.navyHi, P.navy, P.navyDeep)).apply {
                shape = GradientDrawable.OVAL
                setStroke(dpx(4), if (scanOn) P.sky else P.white)
            },
            a(P.white, 0.25f), 100
        )
        elevation = dpx(16).toFloat()
        shadow(P.navy)
        isClickable = true
        isFocusable = true
        contentDescription = "สแกนฉลาก"
        addView(ImageView(this@bottomBar).apply { setImageDrawable(Ico(Ico.SCAN, P.white, 2f)) },
            FrameLayout.LayoutParams(dpx(26), dpx(26), Gravity.CENTER))
        setOnClickListener { if (!scanOn) goTab(ImportActivity::class.java) }
        layoutParams = FrameLayout.LayoutParams(dpx(60), dpx(60), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }
    holder.addView(fab)
    return holder
}

// ========================= ประกอบหน้าจอ =========================

internal fun Activity.appStage(content: View, activeTab: Int, top: View, showBar: Boolean = true): View {
    val scroll = ScrollView(this).apply {
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        clipToPadding = false
        addView(content)
    }
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(top, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
    }
    val stage = FrameLayout(this).apply {
        setBackgroundColor(P.paper)
        clipChildren = false
    }
    stage.addView(column, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

    val bar = if (showBar) bottomBar(activeTab) else null
    if (bar != null) {
        stage.addView(bar, FrameLayout.LayoutParams(MATCH_PARENT, dpx(92), Gravity.BOTTOM).apply {
            setMargins(dpx(14), 0, dpx(14), dpx(12))
        })
    }

    val baseTop = top.paddingTop
    stage.onInsets { t, nav, ime ->
        top.setPadding(top.paddingLeft, baseTop + t, top.paddingRight, top.paddingBottom)
        if (bar != null) {
            bar.visibility = if (ime > 0) View.GONE else View.VISIBLE
            (bar.layoutParams as FrameLayout.LayoutParams).bottomMargin = dpx(12) + nav
            bar.requestLayout()
        }
        val room = if (bar != null) nav + dpx(118) else nav + dpx(28)
        scroll.setPadding(0, 0, 0, maxOf(room, ime + dpx(24)))
        scroll.revealFocus(ime)
    }
    return stage
}

// ========================= วันที่แบบไทย =========================

private val TH_MONTHS = arrayOf(
    "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
    "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
)

internal fun thaiDate(t: Long): String {
    val c = GregorianCalendar().apply { timeInMillis = t }
    val now = GregorianCalendar()
    val time = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    val sameYear = c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val day = c.get(Calendar.DAY_OF_YEAR)
    val today = now.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && day == today -> "วันนี้ · $time"
        sameYear && day == today - 1 -> "เมื่อวาน · $time"
        else -> "${c.get(Calendar.DAY_OF_MONTH)} ${TH_MONTHS[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR) + 543} · $time"
    }
}

// ========================= โหลดภาพจากไฟล์ มีแคช =========================

object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(path: String, maxSide: Int): Bitmap? {
        if (path.isBlank()) return null
        val key = "$path@$maxSide"
        cache.get(key)?.let { return it }
        if (!File(path).exists()) return null

        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        var s = 1
        while (o.outWidth / (s * 2) >= maxSide || o.outHeight / (s * 2) >= maxSide) s *= 2
        val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
            ?: return null
        cache.put(key, bmp)
        return bmp
    }

    fun forget(path: String) {
        cache.snapshot().keys.filter { it.startsWith("$path@") }.forEach { cache.remove(it) }
    }
}

internal fun AppCompatActivity.loadThumb(path: String, maxSide: Int, into: (Bitmap?) -> Unit) {
    lifecycleScope.launch {
        val b = withContext(Dispatchers.IO) {
            try { Thumbs.get(path, maxSide) } catch (e: Throwable) { null }
        }
        into(b)
    }
}

// ========================= กรอบภาพที่ YOLO ครอบ =========================

/**
 * กรอบแสดงภาพที่ครอบแล้ว มีมุมวงเล็บแบบช่องมองกล้อง
 * ความสูงปรับตามสัดส่วนภาพ ภาพเลข อย. มักเป็นแถบยาวแนวนอน
 */
class CropFrameView(context: Context, private val compact: Boolean = false) : View(context) {

    private val d = resources.displayMetrics.density

    var bitmap: Bitmap? = null
        set(v) {
            field = v
            requestLayout()
            invalidate()
        }
    var surface: Int = P.skyHi
    var bracket: Int = P.teal

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val br = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val img = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val scan = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val band = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val dst = RectF()
    private val clip = Path()
    private val empty = Ico(Ico.IMAGE, P.tealSoft, 1.6f)

    private var scanT = -1f
    private var anim: ValueAnimator? = null

    private val pad: Float get() = (if (compact) 12f else 22f) * d

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        val inner = (w - pad * 2).coerceAtLeast(1f)
        val b = bitmap
        val natural = if (b != null && b.width > 0) inner * b.height / b.width + pad * 2
        else (if (compact) 84f else 124f) * d
        val h = natural.coerceIn((if (compact) 76f else 108f) * d, (if (compact) 150f else 260f) * d)
        setMeasuredDimension(w, h.toInt())
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = (if (compact) 14f else 20f) * d

        box.set(0f, 0f, w, h)
        bg.color = surface
        c.drawRoundRect(box, r, r, bg)
        border.color = P.line
        border.strokeWidth = d
        box.inset(d / 2f, d / 2f)
        c.drawRoundRect(box, r, r, border)

        val b = bitmap
        if (b != null && b.width > 0 && b.height > 0) {
            val aw = w - pad * 2
            val ah = h - pad * 2
            val s = minOf(aw / b.width, ah / b.height)
            val iw = b.width * s
            val ih = b.height * s
            dst.set((w - iw) / 2f, (h - ih) / 2f, (w + iw) / 2f, (h + ih) / 2f)
            clip.reset()
            clip.addRoundRect(dst, 6 * d, 6 * d, Path.Direction.CW)
            c.save()
            c.clipPath(clip)
            c.drawBitmap(b, null, dst, img)
            c.restore()
        } else {
            val s = 28 * d
            empty.setBounds((w / 2 - s / 2).toInt(), (h / 2 - s / 2).toInt(),
                (w / 2 + s / 2).toInt(), (h / 2 + s / 2).toInt())
            empty.draw(c)
            dst.set(w * 0.22f, h * 0.24f, w * 0.78f, h * 0.76f)
        }

        val o = (if (compact) 5f else 8f) * d
        val left = (dst.left - o).coerceAtLeast(3 * d)
        val top = (dst.top - o).coerceAtLeast(3 * d)
        val right = (dst.right + o).coerceAtMost(w - 3 * d)
        val bottom = (dst.bottom + o).coerceAtMost(h - 3 * d)
        val len = minOf((if (compact) 14f else 22f) * d, (right - left) / 3f, (bottom - top) / 2.4f)

        br.color = bracket
        br.strokeWidth = (if (compact) 2.5f else 3f) * d
        corner(c, left, top, len, 1, 1)
        corner(c, right, top, len, -1, 1)
        corner(c, left, bottom, len, 1, -1)
        corner(c, right, bottom, len, -1, -1)

        if (scanT in 0f..1f) {
            val y = top + (bottom - top) * scanT
            val fade = 1f - scanT
            band.shader = LinearGradient(0f, y - 22 * d, 0f, y,
                a(bracket, 0f), a(bracket, 0.24f * fade), Shader.TileMode.CLAMP)
            c.drawRect(left, (y - 22 * d).coerceAtLeast(top), right, y, band)
            scan.color = bracket
            scan.alpha = (255 * fade).toInt()
            scan.strokeWidth = 2f * d
            c.drawLine(left + 4 * d, y, right - 4 * d, y, scan)
        }
    }

    private fun corner(c: Canvas, x: Float, y: Float, len: Float, dx: Int, dy: Int) {
        clip.reset()
        clip.moveTo(x, y + len * dy)
        clip.lineTo(x, y)
        clip.lineTo(x + len * dx, y)
        c.drawPath(clip, br)
    }

    /** เส้นสแกนวิ่งผ่านหนึ่งรอบ บอกผู้ใช้ว่านี่คือส่วนที่ AI จับได้ */
    fun playScan() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                scanT = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scanT = -1f
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}

// ========================= แถบความคืบหน้า =========================

class ProgressLine(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = P.skySoft }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private var value = 0f
    private var anim: ValueAnimator? = null

    fun set(v: Float, animate: Boolean = true, duration: Long = 300L) {
        val target = v.coerceIn(0f, 1f)
        anim?.cancel()
        if (!animate) {
            value = target
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(value, target).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                value = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val h = height.toFloat()
        val r = h / 2f
        c.drawRoundRect(0f, 0f, width.toFloat(), h, r, r, track)
        if (value > 0f) {
            val w = maxOf(h, width * value)
            fill.shader = LinearGradient(0f, 0f, w, 0f, P.teal, P.navy, Shader.TileMode.CLAMP)
            c.drawRoundRect(0f, 0f, w, h, r, r, fill)
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}

// ========================= ลายเส้นโค้งตกแต่ง =========================

/** เส้นริบบิ้นขดเป็นห่วง ค่อย ๆ วาดตัวเองตอนเปิดหน้า */
class LoopArt(context: Context, private val tint: Int = Color.WHITE, private val strength: Float = 1f) : View(context) {
    private val d = resources.displayMetrics.density
    private val thick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = tint
    }
    private val thin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = tint
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val p1 = Path()
    private val p2 = Path()
    private var l1 = 1f
    private var l2 = 1f
    private var t = 0f
    private var anim: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w == 0 || h == 0) return
        val W = w.toFloat()
        val H = h.toFloat()
        p1.reset()
        p1.moveTo(-0.08f * W, 0.30f * H)
        p1.cubicTo(0.25f * W, 0.02f * H, 0.78f * W, 0.10f * H, 0.70f * W, 0.36f * H)
        p1.cubicTo(0.64f * W, 0.56f * H, 0.36f * W, 0.58f * H, 0.40f * W, 0.42f * H)
        p1.cubicTo(0.44f * W, 0.27f * H, 0.80f * W, 0.30f * H, 0.92f * W, 0.52f * H)
        p1.cubicTo(1.00f * W, 0.66f * H, 1.02f * W, 0.74f * H, 1.12f * W, 0.80f * H)
        p2.reset()
        p2.moveTo(1.10f * W, 0.10f * H)
        p2.cubicTo(0.80f * W, 0.20f * H, 0.92f * W, 0.52f * H, 0.56f * W, 0.64f * H)
        p2.cubicTo(0.30f * W, 0.72f * H, 0.12f * W, 0.60f * H, -0.10f * W, 0.74f * H)
        l1 = PathMeasure(p1, false).length.coerceAtLeast(1f)
        l2 = PathMeasure(p2, false).length.coerceAtLeast(1f)
        glow.shader = RadialGradient(0.82f * W, 0.14f * H, 0.55f * W,
            a(tint, 0.22f * strength), a(tint, 0f), Shader.TileMode.CLAMP)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener {
                t = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
        thick.strokeWidth = 15f * d
        thick.alpha = (230 * strength).toInt().coerceIn(0, 255)
        thick.pathEffect = DashPathEffect(floatArrayOf(l1, l1), l1 * (1f - t))
        c.drawPath(p1, thick)
        thin.strokeWidth = 4f * d
        thin.alpha = (120 * strength).toInt().coerceIn(0, 255)
        thin.pathEffect = DashPathEffect(floatArrayOf(l2, l2), l2 * (1f - t))
        c.drawPath(p2, thin)
    }
}

/** ภาพประกอบบนการ์ดหน้าแรก กรอบครอปจำลองกับเส้นสแกนวิ่งวน */
class FrameArt(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    private val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a(Color.WHITE, 0.10f) }
    private val br = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = P.sky
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = a(Color.WHITE, 0.92f)
        textAlign = Paint.Align.CENTER
        typeface = context.appFont(true)
        letterSpacing = 0.08f
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val rect = RectF()
    private val path = Path()
    private val sample = "10-1-23456-7-8901"
    private var t = 0f
    private var anim: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                t = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, 16 * d, 16 * d, box)

        val o = 9 * d
        val len = 16 * d
        br.strokeWidth = 2.6f * d
        fun corner(x: Float, y: Float, dx: Int, dy: Int) {
            path.reset()
            path.moveTo(x, y + len * dy)
            path.lineTo(x, y)
            path.lineTo(x + len * dx, y)
            c.drawPath(path, br)
        }
        corner(o, o, 1, 1)
        corner(w - o, o, -1, 1)
        corner(o, h - o, 1, -1)
        corner(w - o, h - o, -1, -1)

        text.textSize = h * 0.32f
        val tw = text.measureText(sample)
        if (tw > w * 0.8f) text.textSize *= (w * 0.8f) / tw
        val baseline = h / 2f - (text.descent() + text.ascent()) / 2f
        c.drawText(sample, w / 2f, baseline, text)

        val y = o + (h - o * 2) * t
        val alpha = kotlin.math.sin(t * Math.PI).toFloat()
        line.color = P.sky
        line.alpha = (220 * alpha).toInt()
        line.strokeWidth = 2 * d
        c.drawLine(o + 6 * d, y, w - o - 6 * d, y, line)
    }
}

/* =====================================================================
 *  ที่เก็บประวัติการตรวจ มีที่เดียวทั้งแอป
 *
 *  - เก็บครบทุกฟิลด์ เลข ชื่อ ผู้ผลิต ประเภท ที่ตั้ง ผลตรวจ ข้อความจากระบบ
 *  - เก็บภาพที่ YOLO ครอบไว้เป็นไฟล์ในเครื่อง เปิดดูย้อนหลังได้เหมือนตอนตรวจสด
 *  - เก็บใน SharedPreferences ของแอป ปิดแอป ออกจากระบบ หรือเข้าใหม่ ก็ยังอยู่
 *    ลบได้เฉพาะเมื่อกดลบเอง หรือถอนการติดตั้งแอป
 *  - ตรวจเลขเดิมซ้ำ จะย้ายขึ้นบนสุด อัปเดตข้อมูล และนับจำนวนครั้ง
 *  - ย้ายข้อมูลจากที่เก็บเก่าสองชุด (fda_history, fda_scan_log) มาให้อัตโนมัติครั้งแรก
 * ===================================================================== */
object HistoryStore {

    private const val PREF = "fda_history_v2"
    private const val KEY = "items"
    private const val KEY_MIGRATED = "migrated_legacy"
    private const val LIMIT = 100
    private const val IMG_DIR = "scan_images"
    private val LEGACY_PREFS = listOf("fda_history", "fda_scan_log")
    private val lock = Any()

    data class Entry(
        val id: Long,
        val fdaNo: String,
        val name: String,
        val company: String,
        val productType: String,
        val address: String,
        val valid: Boolean,
        val message: String,
        val imagePath: String,
        val isCrop: Boolean,
        val count: Int,
        val time: Long
    ) {
        /** ชื่อที่ใช้แสดง ว่างเมื่อระบบไม่ส่งชื่อมา */
        val title: String get() = name
        val hasImage: Boolean get() = imagePath.isNotBlank() && File(imagePath).exists()
    }

    // ------------------------------------------------------------------ อ่าน

    fun all(context: Context): List<Entry> = synchronized(lock) {
        val ctx = context.applicationContext
        migrateIfNeeded(ctx)
        read(ctx)
    }

    fun latest(context: Context): Entry? = all(context).firstOrNull()

    fun get(context: Context, id: Long): Entry? = all(context).firstOrNull { it.id == id }

    fun counts(context: Context): Triple<Int, Int, Int> {
        val list = all(context)
        val pass = list.count { it.valid }
        return Triple(list.size, pass, list.size - pass)
    }

    // ------------------------------------------------------------------ เขียน

    /** บันทึกผลตรวจหนึ่งครั้ง คืนรายการที่บันทึกแล้ว ใช้ id นี้เปิดหน้าผลตรวจ */
    fun add(
        context: Context,
        fdaNo: String,
        name: String?,
        company: String?,
        productType: String?,
        address: String?,
        valid: Boolean,
        message: String?,
        imagePath: String,
        isCrop: Boolean
    ): Entry = synchronized(lock) {
        val ctx = context.applicationContext
        migrateIfNeeded(ctx)
        val no = fdaNo.trim()
        val list = read(ctx).toMutableList()

        val old = list.firstOrNull { it.fdaNo == no }
        if (old != null) {
            list.remove(old)
            if (old.imagePath.isNotBlank() && old.imagePath != imagePath) deleteImage(old.imagePath)
        }

        var id = System.currentTimeMillis()
        while (list.any { it.id == id }) id++

        val entry = Entry(
            id = id,
            fdaNo = no,
            name = clean(name).ifBlank { old?.name.orEmpty() },
            company = clean(company).ifBlank { old?.company.orEmpty() },
            productType = clean(productType).ifBlank { old?.productType.orEmpty() },
            address = clean(address).ifBlank { old?.address.orEmpty() },
            valid = valid,
            message = clean(message),
            imagePath = imagePath.ifBlank { old?.imagePath.orEmpty() },
            isCrop = if (imagePath.isBlank()) old?.isCrop ?: false else isCrop,
            count = (old?.count ?: 0) + 1,
            time = id
        )
        list.add(0, entry)

        list.drop(LIMIT).forEach { deleteImage(it.imagePath) }
        write(ctx, list.take(LIMIT))
        entry
    }

    fun remove(context: Context, id: Long) {
        synchronized(lock) {
            val ctx = context.applicationContext
            val list = read(ctx).toMutableList()
            val hit = list.firstOrNull { it.id == id } ?: return
            list.remove(hit)
            deleteImage(hit.imagePath)
            write(ctx, list)
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            val ctx = context.applicationContext
            read(ctx).forEach { Thumbs.forget(it.imagePath) }
            prefs(ctx).edit().remove(KEY).apply()
            File(ctx.filesDir, IMG_DIR).listFiles()?.forEach { it.delete() }
        }
    }

    // ------------------------------------------------------------------ ภาพ

    fun saveBytes(context: Context, bytes: ByteArray): String {
        val f = newImageFile(context)
        FileOutputStream(f).use { it.write(bytes) }
        return f.absolutePath
    }

    fun saveBitmap(context: Context, bmp: Bitmap, quality: Int = 88): String {
        val f = newImageFile(context)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        return f.absolutePath
    }

    private fun newImageFile(context: Context): File {
        val dir = File(context.applicationContext.filesDir, IMG_DIR).apply { mkdirs() }
        return File(dir, "scan_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
    }

    private fun deleteImage(path: String) {
        if (path.isBlank()) return
        Thumbs.forget(path)
        try { File(path).delete() } catch (e: Exception) { }
    }

    // ------------------------------------------------------------------ ภายใน

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun clean(s: String?): String {
        val v = s?.trim().orEmpty()
        return if (v == "-" || v == "ไม่ระบุ" || v.equals("null", true)) "" else v
    }

    private fun read(ctx: Context): List<Entry> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parse(it) } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parse(o: JSONObject): Entry? {
        val no = o.optString("no").trim()
        if (no.isBlank() || no == "-") return null
        val time = o.optLong("time", System.currentTimeMillis())
        return Entry(
            id = o.optLong("id", time),
            fdaNo = no,
            name = clean(o.optString("name")),
            company = clean(o.optString("company")),
            productType = clean(o.optString("type")),
            address = clean(o.optString("address")),
            valid = o.optBoolean("valid"),
            message = clean(o.optString("message")),
            imagePath = o.optString("image"),
            isCrop = o.optBoolean("crop"),
            count = o.optInt("count", 1).coerceAtLeast(1),
            time = time
        )
    }

    private fun write(ctx: Context, list: List<Entry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("no", it.fdaNo)
                put("name", it.name)
                put("company", it.company)
                put("type", it.productType)
                put("address", it.address)
                put("valid", it.valid)
                put("message", it.message)
                put("image", it.imagePath)
                put("crop", it.isCrop)
                put("count", it.count)
                put("time", it.time)
            })
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    /** ย้ายประวัติจากที่เก็บเก่ามารวมที่นี่ ทำครั้งเดียว ไม่ลบของเดิม */
    private fun migrateIfNeeded(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_MIGRATED, false)) return

        val merged = LinkedHashMap<String, Entry>()
        read(ctx).forEach { merged[it.fdaNo] = it }

        LEGACY_PREFS.forEach { name ->
            val raw = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).getString("items", null)
            if (raw != null) {
                try {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        val e = arr.optJSONObject(i)?.let { parse(it) } ?: continue
                        val cur = merged[e.fdaNo]
                        if (cur == null) {
                            merged[e.fdaNo] = e
                        } else if (e.time > cur.time) {
                            merged[e.fdaNo] = e.copy(
                                name = e.name.ifBlank { cur.name },
                                company = e.company.ifBlank { cur.company },
                                productType = e.productType.ifBlank { cur.productType },
                                address = e.address.ifBlank { cur.address }
                            )
                        }
                    }
                } catch (e: Exception) {
                    // ข้อมูลเก่าเสีย ข้ามไป
                }
            }
        }

        var lastId = Long.MAX_VALUE
        val list = merged.values.sortedByDescending { it.time }.map {
            val id = if (it.id >= lastId) lastId - 1 else it.id
            lastId = id
            it.copy(id = id)
        }.take(LIMIT)
        write(ctx, list)
        p.edit().putBoolean(KEY_MIGRATED, true).apply()
    }
}

/* =====================================================================
 *  หน้าเข้าสู่ระบบ หน้าแรกของแอป
 *
 *  ยังไม่เข้าสู่ระบบ  ภาพหัวไล่สีกรมท่า ฟ้า เบจ มีเส้นริบบิ้นขด
 *                    แผ่นล่างมีตัวเลือก อีเมล สมัคร Google Apple ข้าม
 *                    กดอีเมลแล้วแผ่นล่างเปลี่ยนเป็นฟอร์ม ภาพหัวหดลง
 *  เข้าสู่ระบบแล้ว    หน้าบัญชี การ์ดสมาชิก สถิติ เมนู ออกจากระบบ
 * ===================================================================== */
class LoginActivity : AppCompatActivity() {

    private var hero: FrameLayout? = null
    private var heroFull = 0
    private var heroHead: View? = null
    private var options: View? = null
    private var form: View? = null
    private var scroll: ScrollView? = null
    private lateinit var emailF: Field
    private lateinit var passF: Field
    private lateinit var signIn: Pill

    private val backToOptions = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = showOptions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backToOptions)

        val fromTab = intent.getBooleanExtra("FROM_TAB", false)
        if (TokenStore.isSignedIn(this) && !fromTab) {
            goHome()
            finish()
            return
        }
        if (!TokenStore.isSignedIn(this)) setContentView(buildWelcome())
    }

    override fun onResume() {
        super.onResume()
        // วาดหน้าบัญชีใหม่ทุกครั้ง ตัวเลขสถิติจะได้ตรงกับประวัติล่าสุด
        if (TokenStore.isSignedIn(this)) setContentView(buildProfile())
    }

    // ================================================================ ต้อนรับ

    private fun buildWelcome(): View {
        edgeToEdge(lightStatus = false)
        backToOptions.isEnabled = false

        val stage = FrameLayout(this).apply { setBackgroundColor(P.paper) }
        val sc = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        scroll = sc
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ---------- ภาพหัว ----------
        heroFull = (resources.displayMetrics.heightPixels * 0.47f).toInt().coerceAtLeast(dpx(320))
        val h = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(P.navy, P.teal, P.sky, P.paper))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, heroFull)
        }
        hero = h
        h.addView(LoopArt(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val brand = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpx(24), dpx(14), dpx(24), 0)
            addView(icoView(Ico.SHIELD, P.white, 36, 8,
                solidOf(a(P.white, 0.16f), 12, a(P.white, 0.4f)), 2f))
            addView(txt("อย. เช็กเกอร์", 16f, P.white, true).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    .apply { leftMargin = dpx(10) }
            })
        }
        h.addView(brand, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(26), 0, dpx(26), dpx(46))
            addView(txt("ตรวจก่อนซื้อ", 38f, P.navy, true).apply { letterSpacing = -0.02f })
            addView(txt("มั่นใจทุกฉลาก", 38f, P.teal, true).apply {
                letterSpacing = -0.02f
                layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, -dpx(8))
            })
        }
        heroHead = head
        h.addView(head, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        col.addView(h)

        // ---------- แผ่นล่าง ----------
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = topRounded(P.white, 30)
            elevation = dpx(12).toFloat()
            shadow(P.navy)
            setPadding(dpx(24), dpx(12), dpx(24), dpx(28))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = -dpx(28) }
        }
        sheet.addView(View(this).apply {
            background = solidOf(P.sky, 100)
            layoutParams = LinearLayout.LayoutParams(dpx(44), dpx(5)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpx(18)
            }
        })
        val opt = buildOptions()
        val frm = buildForm().apply { visibility = View.GONE }
        options = opt
        form = frm
        sheet.addView(opt)
        sheet.addView(frm)
        col.addView(sheet)

        sc.addView(col)
        stage.addView(sc, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        stage.onInsets { t, nav, ime ->
            brand.setPadding(dpx(24), dpx(14) + t, dpx(24), 0)
            sheet.setPadding(dpx(24), dpx(12), dpx(24), dpx(28) + maxOf(nav, ime))
            sc.revealFocus(ime)
        }
        return stage
    }

    private fun buildOptions() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        addView(txt("เริ่มต้นใช้งาน", 24f, P.ink, true))
        addView(txt("ตรวจเลข อย. จากรูปฉลากได้ในไม่กี่วินาที เข้าสู่ระบบเพื่อใช้บัญชีเดียวได้ทุกเครื่อง",
            14f, P.muted).apply {
            setLineSpacing(dpx(5).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })

        addView(pill("เข้าสู่ระบบด้วยอีเมล", Btn.PRIMARY, Ico.MAIL) { showForm() }.view.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(22)
        })
        addView(pill("สมัครสมาชิกใหม่", Btn.SECONDARY, Ico.USER) { openRegister() }.view.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(10)
        })
        addView(LinearLayout(this@LoginActivity).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(10))
            addView(pill("Google", Btn.SOFT, height = 50) { notReady("Google") }.view,
                LinearLayout.LayoutParams(0, dpx(50), 1f))
            addView(pill("Apple", Btn.SOFT, height = 50) { notReady("Apple") }.view,
                LinearLayout.LayoutParams(0, dpx(50), 1f).apply { leftMargin = dpx(10) })
        })

        addView(txt("ใช้งานโดยไม่เข้าสู่ระบบ  →", 14.5f, P.teal, true).apply {
            gravity = Gravity.CENTER
            isClickable = true
            background = rippleOf(solidOf(Color.TRANSPARENT, 14), a(P.teal, 0.1f), 14)
            setPadding(dpx(12), dpx(14), dpx(12), dpx(14))
            setOnClickListener { goHome() }
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(10))
        })
    }

    private fun buildForm() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        addView(LinearLayout(this@LoginActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(circleBtn(Ico.BACK, 40) { showOptions() }.apply { contentDescription = "ย้อนกลับ" })
            addView(txt("เข้าสู่ระบบ", 24f, P.ink, true).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    .apply { leftMargin = dpx(12) }
            })
        })
        addView(txt("ใช้อีเมลและรหัสผ่านที่สมัครไว้", 14f, P.muted).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(8))
        })

        addView(fieldLabel("อีเมล", 22))
        emailF = pillField("you@example.com", Ico.MAIL,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        addView(emailF.view)

        addView(LinearLayout(this@LoginActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16))
            addView(fieldLabel("รหัสผ่าน", 0).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            addView(txt("ลืมรหัสผ่าน?", 13.5f, P.teal, true).apply {
                isClickable = true
                background = rippleOf(solidOf(Color.TRANSPARENT, 100), a(P.teal, 0.12f), 100)
                setPadding(dpx(8), dpx(4), dpx(4), dpx(4))
                setOnClickListener { forgotPassword() }
            })
        })
        passF = pillField("อย่างน้อย 6 ตัวอักษร", Ico.LOCK,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password = true)
        addView(passF.view)

        signIn = pill("เข้าสู่ระบบ", Btn.PRIMARY) { doSignIn() }
        addView(signIn.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(24) })

        addView(linkRow("ยังไม่มีบัญชี?", "สมัครสมาชิก") { openRegister() })
    }

    private fun showForm() {
        val o = options ?: return
        val f = form ?: return
        crossfade(o, f)
        animateHero(dpx(170), 0f)
        backToOptions.isEnabled = true
        scroll?.post { scroll?.smoothScrollTo(0, 0) }
    }

    private fun showOptions() {
        val o = options ?: return
        val f = form ?: return
        hideKeyboard()
        crossfade(f, o)
        animateHero(heroFull, 1f)
        backToOptions.isEnabled = false
    }

    private fun crossfade(out: View, into: View) {
        out.animate().alpha(0f).setDuration(140).withEndAction {
            out.visibility = View.GONE
            into.alpha = 0f
            into.visibility = View.VISIBLE
            into.animate().alpha(1f).setDuration(220).start()
        }.start()
    }

    private fun animateHero(target: Int, headAlpha: Float) {
        val h = hero ?: return
        val from = if (h.height > 0) h.height else h.layoutParams.height
        ValueAnimator.ofInt(from, target).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                h.layoutParams.height = it.animatedValue as Int
                h.requestLayout()
            }
            start()
        }
        heroHead?.animate()?.alpha(headAlpha)?.setDuration(220)?.start()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    // ================================================================ เรียก API

    private fun doSignIn() {
        val mail = emailF.value.trim().lowercase()
        val pass = passF.value

        var ok = true
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            setFieldError(emailF, "รูปแบบอีเมลไม่ถูกต้อง เช่น you@example.com")
            ok = false
        }
        if (pass.length < 6) {
            setFieldError(passF, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
            ok = false
        }
        if (!ok) return

        hideKeyboard()
        setBusy(true)
        lifecycleScope.launch {
            try {
                val res = AuthClient.api.login(LoginBody(mail, pass))
                val body = res.body()
                if (res.isSuccessful && body != null && body.success) {
                    TokenStore.save(this@LoginActivity, body.token, body.user?.email ?: mail, body.user?.name)
                    goHome()
                    finish()
                } else {
                    showBanner(Banner.ERROR, "เข้าสู่ระบบไม่สำเร็จ",
                        body?.message ?: "อีเมลหรือรหัสผ่านไม่ถูกต้อง (HTTP ${res.code()})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LoginActivity", "ล็อกอินไม่สำเร็จ", e)
                showBanner(Banner.ERROR, "ต่อเซิร์ฟเวอร์ไม่ได้",
                    "ตรวจว่า backend เปิดอยู่และ BASE_URL ถูกต้อง แล้วลองอีกครั้ง")
            } finally {
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        if (!this::signIn.isInitialized) return
        stylePill(signIn, Btn.PRIMARY, !busy, if (busy) "กำลังเข้าสู่ระบบ…" else "เข้าสู่ระบบ")
    }

    private fun forgotPassword() = showBanner(Banner.INFO, "ยังไม่เปิดให้กู้รหัสผ่าน",
        "ติดต่อผู้ดูแลระบบเพื่อรีเซ็ตรหัสผ่านได้เลย")

    private fun notReady(who: String) = showBanner(Banner.INFO, "ยังไม่รองรับ $who",
        "ตอนนี้เข้าสู่ระบบด้วยอีเมล หรือใช้งานโดยไม่เข้าสู่ระบบได้ก่อน")

    private fun openRegister() = startActivity(Intent(this, RegisterActivity::class.java))

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // ================================================================ หน้าบัญชี

    private fun buildProfile(): View {
        edgeToEdge(lightStatus = true)
        backToOptions.isEnabled = false

        val mail = TokenStore.email(this)
        val name = TokenStore.name(this).ifBlank { mail.substringBefore("@") }
        val (total, pass, fail) = HistoryStore.counts(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(4), dpx(20), dpx(24))
        }
        root.addView(headline("บัญชี", "ของฉัน"))

        // ---------- การ์ดสมาชิก ----------
        val card = FrameLayout(this).apply {
            background = gradOf(intArrayOf(P.navyHi, P.navy, P.tealDeep), 28,
                GradientDrawable.Orientation.TL_BR, P.white, 3)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dpx(10).toFloat()
            shadow(P.navy)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(18))
        }
        card.addView(LoopArt(this, Color.WHITE, 0.35f), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(22), dpx(22), dpx(22), dpx(22))
        }
        inner.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(txt(name.take(1).uppercase().ifBlank { "?" }, 22f, P.navy, true).apply {
                gravity = Gravity.CENTER
                background = circleOf(P.sky, P.white, 2)
                layoutParams = LinearLayout.LayoutParams(dpx(56), dpx(56))
            })
            addView(View(this@LoginActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(chipOf("สมาชิก", P.beigeInk, P.beige))
        })
        inner.addView(txt(name, 22f, P.white, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16))
        })
        inner.addView(txt(mail, 13.5f, P.sky).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
        })
        card.addView(inner)
        root.addView(card)

        card.alpha = 0f
        card.translationY = dpx(16).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(360)
            .setInterpolator(DecelerateInterpolator()).start()

        // ---------- สถิติ ----------
        root.addView(statsRow(total, pass, fail, 14))

        // ---------- เมนู ----------
        root.addView(sectionHead("เมนู", 24))
        root.addView(menuRow(Ico.CLOCK, "ประวัติการตรวจสอบ", "ดูทุกรายการที่เคยตรวจ พร้อมรายละเอียด", 10) {
            goTab(HistoryActivity::class.java)
        })
        root.addView(menuRow(Ico.SCAN, "สแกนฉลากใหม่", "อัปโหลดรูปฉลากเพื่อตรวจเลข อย.", 10) {
            goTab(ImportActivity::class.java)
        })
        root.addView(menuRow(Ico.PHONE, "โทรสายด่วน อย. 1556", "แจ้งเมื่อพบสินค้าผิดปกติ", 10) {
            dialHotline()
        })

        root.addView(noteCard(Ico.INFO,
            "ประวัติการตรวจเก็บไว้ในเครื่องนี้ ออกจากระบบหรือเข้าสู่ระบบใหม่ ประวัติก็ยังอยู่ครบ",
            P.slate, P.skySoft, 16))

        root.addView(pill("ออกจากระบบ", Btn.DANGER) { confirmLogout() }.view.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(18)
        })

        return appStage(root, P.TAB_PROFILE, topBar("บัญชีผู้ใช้"))
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("ออกจากระบบ?")
            .setMessage("ประวัติการตรวจที่บันทึกไว้ในเครื่องนี้จะยังอยู่ครบ")
            .setNegativeButton("ยกเลิก", null)
            .setPositiveButton("ออกจากระบบ") { _, _ ->
                TokenStore.clear(this)
                setContentView(buildWelcome())
                window.decorView.post {
                    showBanner(Banner.INFO, "ออกจากระบบแล้ว", "ประวัติการตรวจยังอยู่ในเครื่องนี้ครบถ้วน")
                }
            }
            .show()
    }
}