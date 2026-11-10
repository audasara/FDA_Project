#!/usr/bin/env kotlin

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // สร้าง Layout แบบ LinearLayout แนวตั้ง
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // สร้างปุ่มแรก
        val button1 = Button(this).apply {
            text = "ปุ่ม 1"
            setBackgroundColor(Color.parseColor("#FF6200EE"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setOnClickListener {
                Toast.makeText(this@MainActivity, "คุณกดปุ่ม 1!", Toast.LENGTH_SHORT).show()
            }
        }

        // สร้างปุ่มสอง
        val button2 = Button(this).apply {
            text = "ปุ่ม 2"
            setBackgroundColor(Color.parseColor("#03DAC5"))
            setTextColor(Color.BLACK)
            textSize = 18f
            setOnClickListener {
                Toast.makeText(this@MainActivity, "คุณกดปุ่ม 2!", Toast.LENGTH_SHORT).show()
            }
        }

        // เพิ่มปุ่มลงใน Layout
        layout.addView(button1)
        layout.addView(button2)

        // ตั้ง Layout เป็น ContentView ของ Activity
        setContentView(layout)
    }
}
