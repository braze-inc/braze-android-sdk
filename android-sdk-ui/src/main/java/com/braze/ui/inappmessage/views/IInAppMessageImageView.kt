package com.braze.ui.inappmessage.views

import com.braze.enums.inappmessage.CropType

/**
 * IInAppMessageImageView is a unifying interface for [android.view.View] implementations
 * that hold in-app message images, defining the required radius and cropping behavior for in-app
 * messages images.
 */
interface IInAppMessageImageView {
    /**
     * Instruct the view to use the given radii for its corners.
     *
     * @param topLeft top-left corner radius in px
     * @param topRight top-right corner radius in px
     * @param bottomLeft bottom-left corner radius in px
     * @param bottomRight bottom-right corner radius in px
     */
    fun setCornersRadiiPx(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float)

    /**
     * Instruct the view to use the given radius for its corners.
     *
     * @param cornersRadius radius for all corners in px
     */
    fun setCornersRadiusPx(cornersRadius: Float)

    /**
     * Instruct the view to use [android.widget.ImageView.ScaleType.CENTER_CROP] or equivalent.
     */
    fun setInAppMessageImageCropType(cropType: CropType?)

    /**
     * Instruct the view to use the given aspect ratio for the image.
     *
     * @param aspectRatio aspectRatio for the image
     */
    fun setAspectRatio(aspectRatio: Float)

    /**
     * Instruct the view to take up half the height of the parent view.
     *
     * @param setToHalfHeight set to true if view should take up half the height of the parent view
     */
    fun setToHalfParentHeight(setToHalfHeight: Boolean)

    /**
     * Instruct the view to set alternate text for the image to be read aloud when device
     * is in accessibility mode.
     *
     * @param altImage alternate image text
     */
    fun setAltImageText(altImage: String?)
}
