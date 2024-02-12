package com.braze.ui.activities

import android.os.Bundle
import android.view.WindowManager
import com.braze.BrazeInternal
import com.braze.ui.R

/**
 * The BrazeFeedActivity in an Activity class that displays the Braze News Feed Fragment. This
 * class can be used to integrate the Braze News Feed as an Activity.
 *
 * Note: To integrate the Braze News Feed as a Fragment instead of an Activity, use the
 * [BrazeFeedFragment] class.
 */
open class BrazeFeedActivity : BrazeBaseFragmentActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BrazeInternal.getConfigurationProvider(this).shouldUseWindowFlagSecureInActivities) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        setContentView(R.layout.com_braze_feed_activity)
    }
}
