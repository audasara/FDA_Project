package com.example.helloworld

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val REGISTER_URL = "http://10.0.2.2:8000/register"

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameF: Field
    private lateinit var emailF: Field
    private lateinit var passF: Field
    private lateinit var confirmF: Field
    private lateinit var submit: Pill

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge(lightStatus = true)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            setBackgroundColor(P.paper)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(24), dpx(8), dpx(24), dpx(24))
        }

        // ---------- แถบบน ----------
        col.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(circleBtn(Ico.BACK) { finish() }.apply { contentDescription = "ย้อนกลับ" })
            addView(txt("สมัครสมาชิก", 16f, P.teal, true).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            addView(View(this@RegisterActivity), LinearLayout.LayoutParams(dpx(42), dpx(1)))
        })

        // ---------- ชื่อแอป ----------
        col.addView(txt("อย. เช็กเกอร์", 38f, P.navy, true).apply {
            letterSpacing = -0.02f
            layoutParams = lpOf(WRAP_CONTENT, WRAP_CONTENT, dpx(28))
        })
        col.addView(View(this).apply {
            background = gradOf(intArrayOf(P.teal, P.sky), 100, GradientDrawable.Orientation.LEFT_RIGHT)
            layoutParams = lpOf(dpx(88), dpx(6), dpx(6))
        })
        col.addView(txt("สร้างบัญชีเพื่อใช้ได้ทุกเครื่อง ตรวจเลข อย. ก่อนซื้อได้ในไม่กี่วินาที",
            14f, P.muted).apply {
            setLineSpacing(dpx(5).toFloat(), 1f)
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(14))
        })

        // ---------- ฟอร์ม ----------
        col.addView(fieldLabel("ชื่อที่ใช้แสดง", 22))
        nameF = pillField("เช่น สมชาย", Ico.USER,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
        col.addView(nameF.view)

        col.addView(fieldLabel("อีเมล", 16))
        emailF = pillField("you@example.com", Ico.MAIL,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        col.addView(emailF.view)

        col.addView(fieldLabel("รหัสผ่าน", 16))
        passF = pillField("อย่างน้อย 6 ตัวอักษร", Ico.LOCK,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password = true)
        col.addView(passF.view)

        col.addView(fieldLabel("ยืนยันรหัสผ่าน", 16))
        confirmF = pillField("พิมพ์รหัสผ่านอีกครั้ง", Ico.LOCK,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password = true)
        col.addView(confirmF.view)

        submit = pill("สร้างบัญชี", Btn.PRIMARY) { doRegister() }
        col.addView(submit.view.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpx(26) })

        col.addView(txt("ประวัติการตรวจจะเก็บไว้ในเครื่องนี้ และยังอยู่ครบแม้ออกจากระบบ",
            12f, P.faint).apply {
            gravity = Gravity.CENTER
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, dpx(12))
        })

        // ดันลิงก์เข้าสู่ระบบไปไว้ล่างสุด
        col.addView(View(this), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = dpx(24) })
        col.addView(txt("มีบัญชีอยู่แล้ว?", 14f, P.muted).apply {
            gravity = Gravity.CENTER
            layoutParams = lpOf(MATCH_PARENT, WRAP_CONTENT, 0)
        })
        col.addView(txt("เข้าสู่ระบบ", 16f, P.teal, true).apply {
            gravity = Gravity.CENTER
            isClickable = true
            background = rippleOf(solidOf(android.graphics.Color.TRANSPARENT, 100), a(P.teal, 0.12f), 100)
            setPadding(dpx(16), dpx(8), dpx(16), dpx(8))
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        })

        scroll.addView(col)
        scroll.onInsets { t, nav, ime ->
            col.setPadding(dpx(24), dpx(8) + t, dpx(24), dpx(20) + maxOf(nav, ime))
            scroll.revealFocus(ime)
        }
        setContentView(scroll)
    }

    private fun doRegister() {
        val name = nameF.value.trim()
        val mail = emailF.value.trim().lowercase()
        val pass = passF.value
        val confirm = confirmF.value

        var ok = true
        if (name.length < 2) {
            setFieldError(nameF, "กรอกชื่ออย่างน้อย 2 ตัวอักษร"); ok = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            setFieldError(emailF, "รูปแบบอีเมลไม่ถูกต้อง เช่น you@example.com"); ok = false
        }
        if (pass.length < 6) {
            setFieldError(passF, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"); ok = false
        }
        if (confirm != pass) {
            setFieldError(confirmF, "รหัสผ่านสองช่องไม่ตรงกัน"); ok = false
        }
        if (!ok) return

        setBusy(true)
        lifecycleScope.launch {
            try {
                val (code, json) = withContext(Dispatchers.IO) { postRegister(name, mail, pass) }
                val success = json?.optBoolean("success", code in 200..299) ?: false
                val message = json?.optString("message")?.takeIf { it.isNotBlank() }
                if (code in 200..299 && success) {
                    val token = json?.optString("token").orEmpty()
                    val user = json?.optJSONObject("user")
                    if (token.isNotBlank()) {
                        TokenStore.save(this@RegisterActivity, token,
                            user?.optString("email")?.ifBlank { null } ?: mail,
                            user?.optString("name")?.ifBlank { null } ?: name)
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                        finish()
                    } else {
                        showBanner(Banner.SUCCESS, "สมัครสมาชิกสำเร็จ", "เข้าสู่ระบบด้วยอีเมลนี้ได้เลย")
                        delay(1500)
                        finish()
                    }
                } else {
                    showBanner(Banner.ERROR, "สมัครไม่สำเร็จ",
                        message ?: "อีเมลนี้อาจถูกใช้แล้ว (HTTP $code)")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("RegisterActivity", "สมัครไม่สำเร็จ", e)
                showBanner(Banner.ERROR, "ต่อเซิร์ฟเวอร์ไม่ได้",
                    "ตรวจว่า backend เปิดอยู่และ BASE_URL ถูกต้อง แล้วลองอีกครั้ง")
            } finally {
                setBusy(false)
            }
        }
    }

    /** ส่งคำขอสมัคร คืนรหัส HTTP กับ JSON ที่ได้ (อ่าน JSON ไม่ได้คืน null) */
    private fun postRegister(name: String, mail: String, pass: String): Pair<Int, JSONObject?> {
        val payload = JSONObject().apply {
            put("name", name)
            put("email", mail)
            put("password", pass)
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        client.newCall(Request.Builder().url(REGISTER_URL).post(payload).build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val json = try { JSONObject(text) } catch (e: Exception) { null }
            return r.code to json
        }
    }

    private fun setBusy(busy: Boolean) {
        stylePill(submit, Btn.PRIMARY, !busy, if (busy) "กำลังสร้างบัญชี…" else "สร้างบัญชี")
    }
}