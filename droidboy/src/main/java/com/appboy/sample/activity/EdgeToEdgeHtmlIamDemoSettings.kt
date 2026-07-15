package com.appboy.sample.activity

/**
 * Runtime toggles for the edge-to-edge HTML IAM demo activity.
 */
internal object EdgeToEdgeHtmlIamDemoSettings {
    /**
     * When true, HTML IAMs on this screen receive [androidx.core.graphics.Insets] of zero
     * instead of system or demo inset values.
     */
    var forceZeroIamInsets: Boolean = false

    /**
     * When true and [forceZeroIamInsets] is false, left/right inset values are at least the
     * host side-indicator width so horizontal gaps appear on emulators.
     */
    var useDemoHorizontalInsets: Boolean = true
}
