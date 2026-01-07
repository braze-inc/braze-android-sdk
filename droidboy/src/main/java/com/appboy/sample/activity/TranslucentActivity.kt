package com.appboy.sample.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.appboy.sample.R

/**
 * Test activity with a translucent theme.
 * Used to reproduce a bug where the underlying activity's onStop() is not called
 * when this activity is shown (because it's translucent), and therefore onStart()
 * is not called when returning to the underlying activity.
 */
class TranslucentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translucent)

        findViewById<Button>(R.id.btn_open_another_translucent).setOnClickListener {
            startActivity(Intent(this, TranslucentActivity::class.java))
        }

        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_open_sample).setOnClickListener {
            startActivity(Intent(this, SampleActivity::class.java))
        }
    }
}
