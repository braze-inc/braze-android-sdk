package com.braze.firebasepush

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.braze.Braze.Companion.getInstance

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val applicationContext = applicationContext
        val userIdInput = findViewById<EditText>(R.id.editTextUserId)
        val submitUserId = findViewById<Button>(R.id.buttonChangeUser)
        submitUserId.setOnClickListener { _: View? ->
            val userId = userIdInput.text.toString()
            if (userId.isEmpty()) {
                showMessage("User Id should not be null or empty. Doing nothing.")
                return@setOnClickListener
            } else {
                showMessage("Changed user to $userId and requested flush")
                getInstance(applicationContext).changeUser(userId)
            }
            getInstance(applicationContext).requestImmediateDataFlush()
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
