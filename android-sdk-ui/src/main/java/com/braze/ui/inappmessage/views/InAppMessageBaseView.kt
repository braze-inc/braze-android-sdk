package com.braze.ui.inappmessage.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.window.BackEvent
import androidx.core.view.WindowInsetsCompat
import com.braze.enums.inappmessage.TextAlign
import com.braze.models.inappmessage.IInAppMessageWithImage
import com.braze.support.BrazeLogger.Priority.D
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils.setIcon
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils.setImage
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils.setTextAlignment
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils.setTextViewColor
import com.braze.ui.inappmessage.utils.InAppMessageViewUtils.setViewBackgroundColor
import com.braze.ui.support.removeViewFromParent
import java.io.File

abstract class InAppMessageBaseView(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), IInAppMessageView, IInAppMessageBackEventListener {
    override val messageClickableView: View?
        get() = this

    override var hasAppliedWindowInsets: Boolean = false

    abstract val messageTextView: TextView?
    abstract val messageImageView: ImageView?
    abstract val messageIconView: TextView?
    abstract val messageBackgroundObject: Any?

    open fun setMessageBackgroundColor(color: Int) {
        setViewBackgroundColor(messageBackgroundObject as View, color)
    }

    open fun setMessageTextColor(color: Int) {
        messageTextView?.let { setTextViewColor(it, color) }
    }

    open fun setMessageTextAlign(textAlign: TextAlign) {
        messageTextView?.let { setTextAlignment(it, textAlign) }
    }

    open fun setMessage(text: String) {
        messageTextView?.text = text
    }

    open fun setMessageImageView(bitmap: Bitmap) {
        messageImageView?.let { setImage(bitmap, it) }
    }

    open fun setMessageIcon(icon: String, iconColor: Int, iconBackgroundColor: Int) {
        messageIconView?.let { setIcon(context, icon, iconColor, iconBackgroundColor, it) }
    }

    open fun resetMessageMargins(imageRetrievalSuccessful: Boolean) {
        messageImageView?.let {
            if (!imageRetrievalSuccessful) {
                it.removeViewFromParent()
            } else {
                messageIconView.removeViewFromParent()
            }
        }
        if (messageIconView?.text?.toString()?.isBlank() == true) {
            messageIconView.removeViewFromParent()
        }
    }

    override fun applyWindowInsets(insets: WindowInsetsCompat) {
        hasAppliedWindowInsets = true
    }

    /**
     * Predictive Back Animation - Progressed
     *
     * When the back button or gesture is held, the full in-app message view will shrink in size and
     * translate off to the right, allowing the user to see the view behind in the stack. This imitates
     * Android's system animation for predictive back, which is used for activities and tasks.
     */
    override fun onBackProgressed(backEvent: BackEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val originalWidth = width.toFloat()
            val originalHeight = height.toFloat()

            val scaledWidth = originalWidth * PREDICTIVE_BACK_MAX_SCALE_FACTOR
            val scaledHeight = originalHeight * PREDICTIVE_BACK_MAX_SCALE_FACTOR

            // Shrink the view and translate the view to the right
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(
                ObjectAnimator.ofFloat(this, "scaleX", PREDICTIVE_BACK_MAX_SCALE_FACTOR),
                ObjectAnimator.ofFloat(this, "scaleY", PREDICTIVE_BACK_MAX_SCALE_FACTOR),
                ObjectAnimator.ofFloat(this, "translationX", (originalWidth - scaledWidth) / TRANSLATE_X_FACTOR),
                ObjectAnimator.ofFloat(this, "translationY", (originalHeight - scaledHeight) / TRANSLATE_Y_FACTOR)
            )
            animatorSet.start()
        }
    }

    /**
     * Predictive Back Animation - Cancelled
     *
     * When the back button or gesture is cancelled (i.e. finger moved away from completing the action),
     * the view will animate back to the original position and size.
     */
    override fun onBackCancelled() {
        // Reset the size of the view and translate back to original position
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(this, "scaleX", 1.0f),
            ObjectAnimator.ofFloat(this, "scaleY", 1.0f),
            ObjectAnimator.ofFloat(this, "translationX", 0.0f)
        )
        animatorSet.start()
    }

    companion object {
        /**
         * Used for the predictive back animation.
         *
         * The float amount between 0-1 to scale the view size when shrinking.
         */
        private const val PREDICTIVE_BACK_MAX_SCALE_FACTOR = 0.85f

        /**
         * Used for the predictive back animation.
         *
         * The value to divide the delta for the X translation
         */
        private const val TRANSLATE_X_FACTOR = 2

        /**
         * Used for the predictive back animation.
         *
         * The value to divide the delta for the Y translation
         */
        private const val TRANSLATE_Y_FACTOR = 8

        /**
         * Gets either the local or remote image URL for an in-app message containing an image.
         *
         * @param inAppMessage The in-app message with an image.
         * @return The local image URL, if present. Otherwise, return the remote image URL. Local
         * image URLs are URLs for images pre-fetched by the SDK for triggers.
         */
        @JvmStatic
        fun getAppropriateImageUrl(inAppMessage: IInAppMessageWithImage): String? {
            val localImagePath = inAppMessage.localImageUrl
            if (!localImagePath.isNullOrBlank()) {
                val imageFile = File(localImagePath)
                if (imageFile.exists()) {
                    return localImagePath
                } else {
                    brazelog(D) {
                        "Local bitmap file does not exist. Using remote url instead. Local path: $localImagePath"
                    }
                }
            }
            return inAppMessage.remoteImageUrl
        }
    }
}
