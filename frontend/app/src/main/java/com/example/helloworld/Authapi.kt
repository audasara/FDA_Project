package com.example.helloworld

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/* =====================================================================
 *  ส่วนติดต่อ API สมาชิก คู่กับไฟล์ auth.py ฝั่ง FastAPI
 *
 *  ต้องแก้ BASE_URL ด้านล่างให้ตรงกับที่ ApiClient เดิมใช้อยู่
 *  - รันบน emulator ชี้เครื่องตัวเอง : http://10.0.2.2:8000/
 *  - รันบนมือถือจริงในวงแลนเดียวกัน  : http://192.168.x.x:8000/
 * ===================================================================== */

// ---------- รูปแบบข้อมูล ----------
data class RegisterBody(val email: String, val password: String, val name: String?)
data class LoginBody(val email: String, val password: String)
data class AuthUser(val email: String?, val name: String?)
data class AuthResponse(
    val success: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val user: AuthUser? = null
)

// ---------- เส้นทาง ----------
interface AuthService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterBody): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginBody): Response<AuthResponse>
}

// ---------- ตัวเชื่อม ----------
object AuthClient {

    // แก้ตรงนี้ให้ตรงกับ BASE_URL ใน ApiClient ของคุณ ต้องปิดท้ายด้วย /
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: AuthService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}

/**
 * เก็บโทเคนกับข้อมูลผู้ใช้ไว้ในเครื่อง
 *
 * หมายเหตุด้านความปลอดภัย: SharedPreferences ธรรมดาอ่านได้ถ้าเครื่องถูก root
 * ถ้าจะใช้งานจริงควรเปลี่ยนไปใช้ EncryptedSharedPreferences จาก androidx.security
 */
object TokenStore {

    private const val PREF = "fda_user"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun save(c: Context, token: String?, email: String?, name: String?) {
        prefs(c).edit()
            .putString("token", token)
            .putString("email", email.orEmpty())
            .putString("name", name.orEmpty())
            .apply()
    }

    fun token(c: Context): String? = prefs(c).getString("token", null)

    fun email(c: Context): String = prefs(c).getString("email", "").orEmpty()

    fun name(c: Context): String = prefs(c).getString("name", "").orEmpty()

    fun isSignedIn(c: Context): Boolean = !token(c).isNullOrBlank()

    fun clear(c: Context) = prefs(c).edit().clear().apply()
}