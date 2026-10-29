package com.example.helloworld

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private var query = ""
    private var items: List<HistoryStore.Entry> = emptyList()
    private var list: LinearLayout? = null
    private var countLabel: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge(lightStatus = true)
    }

    // กลับมาจากหน้าอื่นแล้วโหลดใหม่ รายการที่เพิ่งตรวจจะขึ้นบนสุดทันที
    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        items = HistoryStore.all(this)
        val pass = items.count { it.valid }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(4), dpx(20), dpx(24))
        }
        root.addView(headline("ประวัติ", "การตรวจสอบ"))
        root.addView(txt("เก็บไว้ในเครื่องนี้ ปิดแอปหรือออกจากระบบแล้วก็ยังอยู่ครบ", 13.5f, P.muted).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })
        root.addView(statsRow(items.size, pass, items.size - pass, 16))

        if (items.isEmpty()) {
            list = null
            countLabel = null
            root.addView(emptyState())
        } else {
            // ---------- ค้นหา ----------
            val search = pillField("ค้นหาชื่อ เลข อย. หรือผู้ผลิต", Ico.SEARCH, InputType.TYPE_CLASS_TEXT)
            (search.view.layoutParams as LinearLayout.LayoutParams).topMargin = dpx(18)
            search.edit.setText(query)
            search.edit.onText {
                query = it.trim()
                renderList(false)
            }
            root.addView(search.view)

            // ---------- หัวรายการ ----------
            root.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(20))
                val label = txt("", 16f, P.ink, true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                }
                countLabel = label
                addView(label)
                addView(LinearLayout(this@HistoryActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rippleOf(solidOf(P.errorSoft, 100), a(P.error, 0.12f), 100)
                    setPadding(dpx(11), dpx(6), dpx(13), dpx(6))
                    isClickable = true
                    setOnClickListener { confirmClear() }
                    addView(icoView(Ico.TRASH, P.error, 15, 0))
                    addView(txt("ล้างทั้งหมด", 12.5f, P.error, true).apply {
                        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                            .apply { leftMargin = dpx(5) }
                    })
                })
            })

            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, 0)
            }
            list = container
            root.addView(container)

            root.addView(txt("แตะเพื่อดูรายละเอียด · กดค้างเพื่อลบรายการ", 12f, P.faint).apply {
                gravity = Gravity.CENTER
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16))
            })
        }

        setContentView(appStage(root, P.TAB_HISTORY, topBar("ประวัติการตรวจสอบ")))
        renderList(true)
    }

    private fun renderList(animate: Boolean) {
        val box = list ?: return
        box.removeAllViews()

        val q = query.lowercase()
        val shown = if (q.isBlank()) items else items.filter {
            it.fdaNo.lowercase().contains(q) || it.name.lowercase().contains(q) ||
                    it.company.lowercase().contains(q) || it.address.lowercase().contains(q) ||
                    it.productType.lowercase().contains(q)
        }
        countLabel?.text = if (q.isBlank()) "ทั้งหมด ${items.size} รายการ" else "พบ ${shown.size} รายการ"

        if (shown.isEmpty()) {
            box.addView(noteCard(Ico.SEARCH, "ไม่พบรายการที่ตรงกับ “$query” ลองค้นด้วยคำอื่น",
                P.slate, P.white, 12, P.line))
            return
        }

        shown.forEachIndexed { i, e ->
            val card = entryCard(e, 12)
            box.addView(card)
            if (animate) {
                card.alpha = 0f
                card.translationY = dpx(14).toFloat()
                card.animate().alpha(1f).translationY(0f)
                    .setStartDelay((i * 50L).coerceAtMost(400L))
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    // ================================================================ ใบประวัติ

    private fun entryCard(e: HistoryStore.Entry, top: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rippleOf(solidOf(P.white, 24, P.line), a(P.navy, 0.07f), 24)
        setPadding(dpx(14), dpx(14), dpx(14), dpx(12))
        isClickable = true
        isLongClickable = true
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(top))
        setOnClickListener { startActivity(ResultActivity.open(this@HistoryActivity, e.id, true)) }
        setOnLongClickListener {
            confirmDelete(e)
            true
        }

        // ---------- สถานะ กับ เวลา ----------
        addView(LinearLayout(this@HistoryActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(statusPill(e.valid))
            addView(View(this@HistoryActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(txt(thaiDate(e.time), 12f, P.muted))
        })

        // ---------- ภาพครอป ----------
        val frame = CropFrameView(this@HistoryActivity, compact = true).apply {
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
        }
        addView(frame)
        loadThumb(e.imagePath, 700) { b -> frame.bitmap = b }

        // ---------- ชื่อ กับ เลข ----------
        addView(txt(e.name.ifBlank { "ไม่พบชื่อผลิตภัณฑ์" }, 16f,
            if (e.name.isBlank()) P.muted else P.ink, true).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpx(3).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
        })
        addView(txt(e.fdaNo, 13.5f, P.navy, true).apply {
            letterSpacing = 0.04f
            background = solidOf(P.skySoft, 10)
            setPadding(dpx(10), dpx(5), dpx(10), dpx(5))
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dpx(8) }
        })

        // ---------- รายละเอียด ----------
        fun mini(icon: Int, value: String) {
            if (value.isBlank()) return
            addView(LinearLayout(this@HistoryActivity).apply {
                layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(9))
                addView(icoView(icon, P.teal, 18, 1).apply {
                    (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(1)
                })
                addView(txt(value, 13.5f, P.slate).apply {
                    maxLines = 3
                    ellipsize = TextUtils.TruncateAt.END
                    setLineSpacing(dpx(3).toFloat(), 1f)
                    layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dpx(9) }
                })
            })
        }
        mini(Ico.BUILDING, e.company)
        mini(Ico.BOX, e.productType)
        mini(Ico.PIN, e.address)

        // ---------- ท้ายใบ ----------
        addView(View(this@HistoryActivity).apply {
            setBackgroundColor(P.line)
            layoutParams = lpOf(MATCH_PARENT, dpx(1), dpx(12))
        })
        addView(LinearLayout(this@HistoryActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(10))
            addView(txt(if (e.count > 1) "ตรวจแล้ว ${e.count} ครั้ง" else "ตรวจ 1 ครั้ง", 12f, P.muted).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            addView(txt("ดูรายละเอียด", 13f, P.teal, true))
            addView(icoView(Ico.CHEVRON, P.teal, 16, 0).apply {
                (layoutParams as LinearLayout.LayoutParams).leftMargin = dpx(2)
            })
        })
    }

    private fun emptyState() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = solidOf(P.white, 24, P.line)
        setPadding(dpx(22), dpx(36), dpx(22), dpx(30))
        layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(22))

        addView(icoView(Ico.CLOCK, P.teal, 66, 17, circleOf(P.skySoft)))
        addView(txt("ยังไม่มีประวัติ", 17f, P.ink, true).apply {
            gravity = Gravity.CENTER
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(16))
        })
        addView(txt("ตรวจเลข อย. สำเร็จเมื่อไร รายการพร้อมชื่อสินค้า ผู้ผลิต ที่ตั้ง และภาพที่ AI ครอบ จะมาเก็บไว้ที่นี่",
            13.5f, P.muted).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dpx(4).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(6))
        })
        addView(pill("เริ่มสแกนฉลาก", Btn.PRIMARY, Ico.SCAN) {
            goTab(ImportActivity::class.java)
        }.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(22) })
    }

    // ================================================================ ลบ

    private fun confirmDelete(e: HistoryStore.Entry) {
        AlertDialog.Builder(this)
            .setTitle("ลบรายการนี้?")
            .setMessage("${e.name.ifBlank { e.fdaNo }}\nจะถูกลบออกจากประวัติ พร้อมภาพที่บันทึกไว้")
            .setNegativeButton("ยกเลิก", null)
            .setPositiveButton("ลบ") { _, _ ->
                HistoryStore.remove(this, e.id)
                render()
                showBanner(Banner.SUCCESS, "ลบรายการแล้ว", e.fdaNo, 2500L)
            }
            .show()
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("ล้างประวัติทั้งหมด?")
            .setMessage("${items.size} รายการและภาพที่บันทึกไว้จะถูกลบออกจากเครื่องนี้ และกู้คืนไม่ได้")
            .setNegativeButton("ยกเลิก", null)
            .setPositiveButton("ล้างทั้งหมด") { _, _ ->
                HistoryStore.clear(this)
                query = ""
                render()
                showBanner(Banner.SUCCESS, "ล้างประวัติแล้ว", "เริ่มสแกนใหม่ได้ทุกเมื่อ")
            }
            .show()
    }
}