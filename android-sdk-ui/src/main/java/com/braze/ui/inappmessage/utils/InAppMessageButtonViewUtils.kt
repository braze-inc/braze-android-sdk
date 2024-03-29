package com.braze.ui.inappmessage.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.Button
import com.braze.models.inappmessage.MessageButton
import com.braze.ui.R

object InAppMessageButtonViewUtils {
    /**
     * Sets the appropriate colors for the button text, background, and border.
     *
     * @param buttonViews    The destination views for the attributes found in the [MessageButton] objects.
     * @param messageButtons The [MessageButton] source objects.
     */
    @JvmStatic
    fun setButtons(
        buttonViews: List<View>,
        messageButtons: List<MessageButton>
    ) {
        for (i in buttonViews.indices) {
            val buttonView = buttonViews[i]
            val messageButton = messageButtons[i]
            val strokeWidth = buttonView.context
                .resources
                .getDimensionPixelSize(R.dimen.com_braze_inappmessage_button_border_stroke)
            val strokeFocusedWidth = buttonView.context
                .resources
                .getDimensionPixelSize(R.dimen.com_braze_inappmessage_button_border_stroke_focused)
            if (messageButtons.size <= i) {
                buttonView.visibility = View.GONE
            } else {
                if (buttonView is Button) {
                    setButton(buttonView, messageButton, strokeWidth, strokeFocusedWidth)
                }
            }
        }
    }

    @JvmStatic
    fun setButton(
        button: Button,
        messageButton: MessageButton,
        strokeWidth: Int,
        strokeFocusedWidth: Int
    ) {
        button.text = messageButton.text
        button.contentDescription = messageButton.text
        InAppMessageViewUtils.setTextViewColor(button, messageButton.textColor)

        // StateListDrawable is the background, holding everything else
        val stateListDrawableBackground = StateListDrawable()
        // The rounded corners in the background give a "shadow" that's actually a state list animator
        // See https://stackoverflow.com/questions/44527700/android-button-with-rounded-corners-ripple-effect-and-no-shadow
        button.stateListAnimator = null
        val defaultButtonDrawable =
            getButtonDrawable(button.context, messageButton, strokeWidth, strokeFocusedWidth, false)
        val focusedButtonDrawable =
            getButtonDrawable(button.context, messageButton, strokeWidth, strokeFocusedWidth, true)

        // The focused state MUST be added before the enabled state to work properly
        stateListDrawableBackground.addState(
            intArrayOf(android.R.attr.state_focused),
            focusedButtonDrawable
        )
        stateListDrawableBackground.addState(
            intArrayOf(android.R.attr.state_enabled),
            defaultButtonDrawable
        )
        button.background = stateListDrawableBackground
    }

    @JvmStatic
    fun getDrawable(
        context: Context,
        drawableId: Int
    ): Drawable =
        context.resources.getDrawable(drawableId, null)

    @JvmStatic
    fun getButtonDrawable(
        context: Context,
        messageButton: MessageButton,
        newStrokeWidth: Int,
        strokeFocusedWidth: Int,
        isFocused: Boolean
    ): Drawable {
        val buttonDrawable = getDrawable(context, R.drawable.com_braze_inappmessage_button_background)
        buttonDrawable.mutate()
        val backgroundFillGradientDrawable =
            // The drawable pulled from resources is a ripple drawable
            (buttonDrawable as RippleDrawable)
                .findDrawableByLayerId(R.id.com_braze_inappmessage_button_background_ripple_internal_gradient) as GradientDrawable
        var strokeWidth = newStrokeWidth
        if (isFocused) {
            strokeWidth = strokeFocusedWidth
        }
        backgroundFillGradientDrawable.setStroke(strokeWidth, messageButton.borderColor)
        backgroundFillGradientDrawable.setColor(messageButton.backgroundColor)
        return buttonDrawable
    }
}
