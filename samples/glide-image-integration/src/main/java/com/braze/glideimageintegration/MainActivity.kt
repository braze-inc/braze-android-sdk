package com.braze.glideimageintegration

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.braze.Braze
import com.braze.enums.inappmessage.CropType
import com.braze.enums.inappmessage.DismissType
import com.braze.enums.inappmessage.ImageStyle
import com.braze.models.inappmessage.InAppMessageBase
import com.braze.models.inappmessage.InAppMessageModal
import com.braze.models.inappmessage.InAppMessageSlideup
import com.braze.ui.inappmessage.BrazeInAppMessageManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35+ edge-to-edge: lay out behind system bars, then inset the root so
        // the toolbar and buttons are not in the status-bar touch region.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.main_activity)

        setSupportActionBar(findViewById(R.id.main_activity_toolbar))

        val root = findViewById<View>(R.id.main_activity_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)

        findViewById<View>(R.id.com_appboy_display_modal_button).setOnClickListener {
            val inAppMessageModal = InAppMessageModal()
            inAppMessageModal.remoteImageUrl = getString(R.string.gif_1_url)
            inAppMessageModal.imageStyle = ImageStyle.GRAPHIC
            inAppMessageModal.cropType = CropType.CENTER_CROP
            showInAppMessage(inAppMessageModal)
        }
        findViewById<View>(R.id.com_appboy_display_slideup_button).setOnClickListener {
            val inAppMessageSlideup = InAppMessageSlideup()
            inAppMessageSlideup.remoteImageUrl = getString(R.string.gif_2_url)
            inAppMessageSlideup.message = "This is a slideup with a GIF"
            showInAppMessage(inAppMessageSlideup)
        }
        findViewById<View>(R.id.com_appboy_flush_button).setOnClickListener {
            Braze.getInstance(applicationContext).requestImmediateDataFlush()
        }
    }

    private fun showInAppMessage(inAppMessage: InAppMessageBase) {
        inAppMessage.dismissType = DismissType.MANUAL
        BrazeInAppMessageManager.getInstance().addInAppMessage(inAppMessage)
    }
}
