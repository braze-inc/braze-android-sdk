package com.appboy.sample.activity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.appboy.sample.R
import com.braze.configuration.BrazeConfig
import com.braze.configuration.BrazeConfigurationProvider
import com.braze.configuration.RuntimeAppConfigurationProvider
import com.braze.enums.inappmessage.DismissType
import com.braze.models.inappmessage.IInAppMessageHtml
import com.braze.models.inappmessage.InAppMessageHtml
import com.braze.models.inappmessage.InAppMessageHtmlFull
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.braze.ui.inappmessage.IInAppMessageViewWrapperFactory
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Host activity for reproducing edge-to-edge HTML in-app message inset behavior.
 *
 * The red/blue/orange/purple host indicators extend behind system bars. When inset margins are
 * applied to the HTML IAM container, those stripes remain visible in the gaps around the WebView.
 */
class EdgeToEdgeHtmlIamActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var applyInsetsSwitch: SwitchMaterial
    private lateinit var forceZeroIamInsetsSwitch: SwitchMaterial
    private lateinit var demoHorizontalInsetsSwitch: SwitchMaterial
    private var previousViewWrapperFactory: IInAppMessageViewWrapperFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_edge_to_edge_html_iam)

        statusTextView = findViewById(R.id.edgeToEdgeHtmlIamStatus)
        applyInsetsSwitch = findViewById(R.id.edgeToEdgeApplyInsetsSwitch)
        forceZeroIamInsetsSwitch = findViewById(R.id.edgeToEdgeForceZeroIamInsetsSwitch)
        demoHorizontalInsetsSwitch = findViewById(R.id.edgeToEdgeDemoHorizontalInsetsSwitch)

        applyInsetsSwitch.isChecked =
            BrazeConfigurationProvider(this).isHtmlInAppMessageApplyWindowInsetsEnabled
        applyInsetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            RuntimeAppConfigurationProvider(this).setConfiguration(
                BrazeConfig
                    .Builder()
                    .setIsHtmlInAppMessageApplyWindowInsetsEnabled(isChecked)
                    .build(),
            )
            updateStatusText()
        }

        forceZeroIamInsetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            EdgeToEdgeHtmlIamDemoSettings.forceZeroIamInsets = isChecked
            updateStatusText()
        }

        demoHorizontalInsetsSwitch.isChecked = EdgeToEdgeHtmlIamDemoSettings.useDemoHorizontalInsets
        demoHorizontalInsetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            EdgeToEdgeHtmlIamDemoSettings.useDemoHorizontalInsets = isChecked
            updateStatusText()
        }

        findViewById<Button>(R.id.edgeToEdgeShowHtmlIamButton).setOnClickListener {
            displayHtmlMessage(InAppMessageHtml(), EDGE_TO_EDGE_HTML_ASSET)
        }
        findViewById<Button>(R.id.edgeToEdgeShowHtmlFullIamButton).setOnClickListener {
            displayHtmlMessage(InAppMessageHtmlFull(), EDGE_TO_EDGE_HTML_ASSET)
        }
        findViewById<Button>(R.id.edgeToEdgeShowCenteredHtmlIamButton).setOnClickListener {
            displayHtmlMessage(InAppMessageHtml(), EDGE_TO_EDGE_CENTERED_HTML_ASSET)
        }

        val inAppMessageManager = BrazeInAppMessageManager.getInstance()
        previousViewWrapperFactory = inAppMessageManager.inAppMessageViewWrapperFactory
        inAppMessageManager.setCustomInAppMessageViewWrapperFactory(EdgeToEdgeHtmlIamViewWrapperFactory())

        applyEdgeToEdgeInsets()
        updateStatusText()
    }

    override fun onDestroy() {
        BrazeInAppMessageManager
            .getInstance()
            .setCustomInAppMessageViewWrapperFactory(previousViewWrapperFactory)
        super.onDestroy()
    }

    private fun displayHtmlMessage(
        inAppMessage: IInAppMessageHtml,
        assetFileName: String,
    ) {
        val htmlString =
            assets.open(assetFileName).bufferedReader().use { it.readText() }
        inAppMessage.message = htmlString
        inAppMessage.dismissType = DismissType.MANUAL
        BrazeInAppMessageManager.getInstance().addInAppMessage(inAppMessage)
        BrazeInAppMessageManager.getInstance().requestDisplayInAppMessage()
    }

    private fun applyEdgeToEdgeInsets() {
        val topIndicator = findViewById<TextView>(R.id.edgeToEdgeHostTopIndicator)
        val bottomIndicator = findViewById<TextView>(R.id.edgeToEdgeHostBottomIndicator)
        val leftIndicator = findViewById<TextView>(R.id.edgeToEdgeHostLeftIndicator)
        val rightIndicator = findViewById<TextView>(R.id.edgeToEdgeHostRightIndicator)
        val controls = findViewById<android.view.View>(R.id.edgeToEdgeHtmlIamControls)

        ViewCompat.setOnApplyWindowInsetsListener(topIndicator) { view, windowInsets ->
            val bars = EdgeToEdgeHtmlIamInsets.getResolvedSystemBarInsets(this, windowInsets)
            view.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomIndicator) { view, windowInsets ->
            val bars = EdgeToEdgeHtmlIamInsets.getResolvedSystemBarInsets(this, windowInsets)
            view.updatePadding(bottom = bars.bottom, left = bars.left, right = bars.right)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(leftIndicator) { view, windowInsets ->
            val bars = EdgeToEdgeHtmlIamInsets.getResolvedSystemBarInsets(this, windowInsets)
            view.updatePadding(left = bars.left)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(rightIndicator) { view, windowInsets ->
            val bars = EdgeToEdgeHtmlIamInsets.getResolvedSystemBarInsets(this, windowInsets)
            view.updatePadding(right = bars.right)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(controls) { view, windowInsets ->
            val bars = EdgeToEdgeHtmlIamInsets.getResolvedSystemBarInsets(this, windowInsets)
            view.updatePadding(left = bars.left, right = bars.right)
            windowInsets
        }
        ViewCompat.requestApplyInsets(findViewById(R.id.edgeToEdgeHtmlIamRoot))
    }

    private fun updateStatusText() {
        val applyInsetsEnabled =
            BrazeConfigurationProvider(this).isHtmlInAppMessageApplyWindowInsetsEnabled
        val forceZero = EdgeToEdgeHtmlIamDemoSettings.forceZeroIamInsets
        val demoHorizontal = EdgeToEdgeHtmlIamDemoSettings.useDemoHorizontalInsets

        val scenario =
            when {
                applyInsetsEnabled && !forceZero ->
                    "BROKEN (letterbox): apply insets ON with non-zero inset values — host stripes show in IAM gaps."
                !applyInsetsEnabled && !forceZero ->
                    "FIXED: apply insets OFF — full-screen IAM with injected --braze-safe-area-inset-* CSS."
                forceZero && applyInsetsEnabled ->
                    "Full-screen (no IAM margins): force-zero passes 0 inset values even though apply insets is ON."
                else ->
                    "Full-screen: zero inset values and apply insets OFF."
            }

        statusTextView.text =
            """
            Host: edge-to-edge (decorFitsSystemWindows = false)
            Apply HTML IAM window insets: $applyInsetsEnabled
            Force zero IAM inset values: $forceZero
            Demo 32dp horizontal insets: $demoHorizontal
            $scenario
            """.trimIndent()
    }

    companion object {
        const val EDGE_TO_EDGE_HTML_ASSET = "html_inapp_edge_to_edge_tester.html"
        const val EDGE_TO_EDGE_CENTERED_HTML_ASSET = "html_inapp_edge_to_edge_tester_centered.html"
    }
}
