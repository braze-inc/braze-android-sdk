package com.appboy.sample.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.appboy.sample.R
import com.braze.enums.inappmessage.DismissType
import com.braze.models.inappmessage.InAppMessageHtml
import com.braze.models.inappmessage.InAppMessageModal
import com.braze.models.inappmessage.InAppMessageSlideup
import com.braze.models.inappmessage.MessageButton
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import java.util.Random

/**
 * Activity whose sole purpose is to host a button that shows a basic IAM on screen.
 */
class InAppMessageSandboxActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_message_sandbox)

        findViewById<View>(R.id.bSandboxDisplayMessage2).setOnClickListener { this.displayMessage(2) }
        findViewById<View>(R.id.bSandboxDisplayMessage1).setOnClickListener { this.displayMessage(1) }
        findViewById<View>(R.id.bSandboxDisplayMessage0).setOnClickListener { this.displayMessage(0) }
        findViewById<View>(R.id.bSandboxDummyButton).setOnClickListener { Toast.makeText(this, "dummy button pressed!", Toast.LENGTH_SHORT).show() }
        findViewById<View>(R.id.bSandboxDisplaySlideup).setOnClickListener { displaySlideup() }
        findViewById<View>(R.id.bSandboxHtmlInApp).setOnClickListener { displayHtmlMessage() }
    }

    private fun displayMessage(numButtons: Int) {
        // Create the message
        val modal = InAppMessageModal()
        modal.header = "hello"
        modal.message = "world"
        modal.remoteImageUrl = getString(R.string.appboy_image_url_1600w_500h)
        modal.dismissType = DismissType.MANUAL

        val rnd = Random()
        val randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        modal.closeButtonColor = randomColor

        val button1 = MessageButton()
        button1.text = "Button 1"
        button1.borderColor = Color.RED
        button1.backgroundColor = Color.BLUE

        val button2 = MessageButton()
        button2.text = "Button 2"
        button2.borderColor = Color.CYAN
        button2.backgroundColor = Color.BLUE

        when (numButtons) {
            2 -> modal.messageButtons = listOf(button1, button2)
            1 -> modal.messageButtons = listOf(button1)
        }
        BrazeInAppMessageManager.getInstance().addInAppMessage(modal)
        BrazeInAppMessageManager.getInstance().requestDisplayInAppMessage()
    }

    private fun displaySlideup() {
        val slideup = InAppMessageSlideup()
        slideup.message = "Welcome to Braze! This is a slideup in-app message."
        slideup.icon = "\uf091"
        slideup.dismissType = DismissType.MANUAL
        BrazeInAppMessageManager.getInstance().addInAppMessage(slideup)
        BrazeInAppMessageManager.getInstance().requestDisplayInAppMessage()
    }

    private fun displayHtmlMessage() {
        val htmlString = this.assets.open(THE_WAY_HTML).bufferedReader().use {
            it.readText()
        }
        val htmlMessage = InAppMessageHtml().apply {
            message = htmlString
            dismissType = DismissType.MANUAL
        }
        BrazeInAppMessageManager.getInstance().addInAppMessage(htmlMessage)
        BrazeInAppMessageManager.getInstance().requestDisplayInAppMessage()
    }

    companion object {
        const val THE_WAY_HTML = "html_inapp_this_is_the_way.html"
    }
}
