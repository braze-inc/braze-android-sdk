package com.braze.ui.inappmessage.config

/**
 * Global configuration parameters for in-app message rendering dimensions.
 * Mutable properties allow runtime customization of modal image radius and
 * graphic modal maximum dimensions.
 */
object BrazeInAppMessageParams {
    /** Default corner radius in dp for modalized in-app message images. */
    const val MODALIZED_IMAGE_RADIUS_DP = 9.0

    /** Default maximum width in dp for graphic modal in-app messages. */
    const val GRAPHIC_MODAL_MAX_WIDTH_DP = 290.0

    /** Default maximum height in dp for graphic modal in-app messages. */
    const val GRAPHIC_MODAL_MAX_HEIGHT_DP = 290.0

    /** Corner radius in dp for modalized in-app message images. */
    @JvmStatic var modalizedImageRadiusDp = MODALIZED_IMAGE_RADIUS_DP

    /** Maximum width in dp for graphic modal in-app messages. */
    @JvmStatic var graphicModalMaxWidthDp = GRAPHIC_MODAL_MAX_WIDTH_DP

    /** Maximum height in dp for graphic modal in-app messages. */
    @JvmStatic var graphicModalMaxHeightDp = GRAPHIC_MODAL_MAX_HEIGHT_DP
}
