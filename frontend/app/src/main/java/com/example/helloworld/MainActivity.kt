package com.example.helloworld

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge(lightStatus = true)
    }

    // วาดใหม่ทุกครั้งที่กลับมา รายการล่าสุดจะได้อัปเดต
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(4), dpx(20), dpx(24))
        }

        // ---------- ทักทาย ----------
        val signedIn = TokenStore.isSignedIn(this)
        val name = if (signedIn) TokenStore.name(this).ifBlank {
            TokenStore.email(this).substringBefore("@")
        } else ""
        root.addView(txt(if (name.isBlank()) "สวัสดี" else "สวัสดี คุณ$name", 14.5f, P.muted).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })
        root.addView(headline("ตรวจเลข อย.", "ก่อนตัดสินใจซื้อ", 31f, 2))

        // ---------- ชิป ----------
        val chips = LinearLayout(this)
        chips.addView(chipOf("ฐานข้อมูล อย. จริง", P.beigeInk, P.beige, P.beigeDeep))
        chips.addView(chipOf("AI ครอบเลขอัตโนมัติ", P.navy, P.white, P.line).apply {
            (layoutParams as LinearLayout.LayoutParams).leftMargin = dpx(8)
        })
        chips.addView(chipOf("ใช้ฟรี", P.navy, P.white, P.line).apply {
            (layoutParams as LinearLayout.LayoutParams).leftMargin = dpx(8)
        })
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(chips)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))
        })

        // ---------- การ์ดหลัก ----------
        root.addView(heroCard())

        // ---------- 3 ขั้นตอน ----------
        root.addView(sectionHead("ใช้งานง่ายใน 3 ขั้น", 26))
        val steps = LinearLayout(this).apply {
            isBaselineAligned = false
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
        }
        listOf(
            Triple(Ico.UPLOAD, "อัปโหลด", "รูปฉลากสินค้า"),
            Triple(Ico.SPARK, "AI ครอบเลข", "และอ่านเลข อย."),
            Triple(Ico.SUCCESS, "ดูผลตรวจ", "เก็บลงประวัติ")
        ).forEachIndexed { i, (icon, title, sub) ->
            steps.addView(stepCard(i + 1, icon, title, sub),
                LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { if (i > 0) leftMargin = dpx(10) })
        }
        root.addView(steps)

        // ---------- ตรวจล่าสุด ----------
        val recent = HistoryStore.all(this).take(3)
        root.addView(sectionHead("ตรวจล่าสุด", 26, if (recent.isNotEmpty()) "ดูทั้งหมด" else null) {
            goTab(HistoryActivity::class.java)
        })
        if (recent.isEmpty()) {
            root.addView(noteCard(Ico.CLOCK,
                "ยังไม่มีประวัติ ผลตรวจทุกครั้งจะเก็บไว้ที่นี่ พร้อมชื่อสินค้า ผู้ผลิต และที่ตั้ง",
                P.slate, P.white, 12, P.line))
        } else {
            recent.forEachIndexed { i, e -> root.addView(recentRow(e, if (i == 0) 12 else 10)) }
        }

        // ---------- สายด่วน ----------
        root.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rippleOf(solidOf(P.beige, 22, P.beigeDeep), a(P.beigeInk, 0.1f), 22)
            setPadding(dpx(14), dpx(14), dpx(16), dpx(14))
            isClickable = true
            setOnClickListener { dialHotline() }
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(22))
            addView(icoView(Ico.PHONE, P.beigeInk, 46, 12, circleOf(P.white)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(13) }
                addView(txt("เจอสินค้าน่าสงสัย?", 15f, P.beigeInk, true))
                addView(txt("แตะเพื่อโทรแจ้งสายด่วน อย. 1556", 13f, P.beigeInk).apply {
                    alpha = 0.85f
                    layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2))
                })
            })
            addView(icoView(Ico.CHEVRON, P.beigeInk, 20, 0))
        })

        root.addView(txt("ข้อมูลอ้างอิงจากฐานข้อมูลสำนักงานคณะกรรมการอาหารและยา", 11.5f, P.faint).apply {
            gravity = Gravity.CENTER
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(18))
        })

        setContentView(appStage(root, P.TAB_HOME, topBar("อย. เช็กเกอร์")))
    }

    // ================================================================ ชิ้นส่วน

    /** การ์ดกรมท่าไล่สี ขอบขาว มีภาพกรอบครอปจำลองกับเส้นสแกน */
    private fun heroCard(): View {
        val hero = FrameLayout(this).apply {
            background = rippleOf(
                gradOf(intArrayOf(P.navyHi, P.navy, P.tealDeep), 28,
                    GradientDrawable.Orientation.TL_BR, P.white, 3),
                a(P.white, 0.18f), 28
            )
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dpx(10).toFloat()
            shadow(P.navy)
            isClickable = true
            setOnClickListener { goTab(ImportActivity::class.java) }
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(18))
        }
        hero.addView(LoopArt(this, Color.WHITE, 0.35f), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(22), dpx(22), dpx(22), dpx(22))
        }
        inner.addView(chipOf("ขั้นตอนเดียวจบ", P.beigeInk, P.beige))
        inner.addView(txt("สแกนฉลากสินค้า", 24f, P.white, true).apply {
            letterSpacing = -0.01f
            layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, dpx(14))
        })
        inner.addView(txt("อัปโหลดรูปฉลาก AI จะครอบบริเวณเลข อย. ให้เอง แล้วเทียบกับฐานข้อมูลจริง",
            14f, P.sky).apply {
            setLineSpacing(dpx(5).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })
        inner.addView(FrameArt(this), lpOf(MATCH_PARENT, dpx(76), dpx(16)))
        inner.addView(pill("เริ่มตรวจสอบ", Btn.LIGHT, Ico.ARROW, 54) {
            goTab(ImportActivity::class.java)
        }.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(16) })
        hero.addView(inner)

        hero.alpha = 0f
        hero.translationY = dpx(20).toFloat()
        hero.animate().alpha(1f).translationY(0f).setDuration(420)
            .setInterpolator(DecelerateInterpolator()).start()
        return hero
    }

    private fun stepCard(n: Int, icon: Int, title: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = solidOf(P.white, 20, P.line)
        setPadding(dpx(12), dpx(14), dpx(12), dpx(14))
        addView(LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icoView(icon, P.teal, 40, 10, circleOf(P.skySoft)))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(txt("0$n", 12f, P.faint, true))
        })
        addView(txt(title, 14f, P.ink, true).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(10)) })
        addView(txt(sub, 12f, P.muted).apply { layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(2)) })
    }

    private fun recentRow(e: HistoryStore.Entry, top: Int) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = rippleOf(solidOf(P.white, 20, P.line), a(P.navy, 0.08f), 20)
        setPadding(dpx(12), dpx(12), dpx(14), dpx(12))
        isClickable = true
        setOnClickListener { startActivity(ResultActivity.open(this@MainActivity, e.id, true)) }
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))

        val thumb = roundImage(12, P.skySoft).apply {
            setPadding(dpx(3), dpx(3), dpx(3), dpx(3))
            setImageDrawable(Ico(Ico.IMAGE, P.tealSoft, 1.6f))
        }
        addView(thumb, LinearLayout.LayoutParams(dpx(70), dpx(48)))
        loadThumb(e.imagePath, 400) { b -> if (b != null) thumb.setImageBitmap(b) }

        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                leftMargin = dpx(12)
                rightMargin = dpx(8)
            }
            addView(txt(e.name.ifBlank { "ไม่พบชื่อผลิตภัณฑ์" }, 14.5f, P.ink, true).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            addView(txt(e.fdaNo, 12.5f, P.teal, true).apply {
                letterSpacing = 0.03f
                layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, dpx(1))
            })
            if (e.company.isNotBlank()) addView(txt(e.company, 12f, P.muted).apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(1))
            })
        })
        addView(View(this@MainActivity).apply {
            background = circleOf(if (e.valid) P.success else P.error)
            contentDescription = if (e.valid) "พบทะเบียน" else "ไม่พบทะเบียน"
            layoutParams = LinearLayout.LayoutParams(dpx(10), dpx(10)).apply { rightMargin = dpx(6) }
        })
        addView(icoView(Ico.CHEVRON, P.tealSoft, 18, 0))
    }
}