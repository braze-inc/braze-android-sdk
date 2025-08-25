package com.appboy.sample.activity

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class SampleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.rgb(139, 110, 255))
        val textView = TextView(this)
        textView.text = "Sample activity of type Activity()"
        layout.addView(textView)
        setContentView(layout)
    }
}
