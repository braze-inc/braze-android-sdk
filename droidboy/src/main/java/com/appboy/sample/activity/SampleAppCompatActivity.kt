package com.appboy.sample.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SampleAppCompatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.rgb(199, 103, 150))
        val textView = TextView(this)
        textView.text = "Sample activity of type AppCompatActivity()"
        layout.addView(textView)
        setContentView(layout)
    }
}
