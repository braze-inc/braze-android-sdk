package com.braze.ui.inappmessage.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.braze.ui.R
import androidx.core.content.withStyledAttributes

/**
 * A [RelativeLayout] that respects maximum/minimum dimension bounds.
 */
open class InAppMessageBoundedLayout : RelativeLayout {
    private var maxDefinedWidthPixels = 0
    private var minDefinedWidthPixels = 0
    private var maxDefinedHeightPixels = 0
    private var minDefinedHeightPixels = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.InAppMessageBoundedLayout) {
            maxDefinedWidthPixels = getDimensionPixelSize(
                R.styleable.InAppMessageBoundedLayout_inAppMessageBoundedLayoutMaxWidth,
                0
            )
            minDefinedWidthPixels = getDimensionPixelSize(
                R.styleable.InAppMessageBoundedLayout_inAppMessageBoundedLayoutMinWidth,
                0
            )
            maxDefinedHeightPixels = getDimensionPixelSize(
                R.styleable.InAppMessageBoundedLayout_inAppMessageBoundedLayoutMaxHeight,
                0
            )
            minDefinedHeightPixels = getDimensionPixelSize(
                R.styleable.InAppMessageBoundedLayout_inAppMessageBoundedLayoutMinHeight,
                0
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var newWidthMeasureSpec = widthMeasureSpec
        var newHeightMeasureSpec = heightMeasureSpec

        val measuredWidth = MeasureSpec.getSize(newWidthMeasureSpec)
        if (minDefinedWidthPixels > 0 && measuredWidth < minDefinedWidthPixels) {
            val measureMode = MeasureSpec.getMode(newWidthMeasureSpec)
            newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(minDefinedWidthPixels, measureMode)
        } else if (maxDefinedWidthPixels > 0 && measuredWidth > maxDefinedWidthPixels) {
            val measureMode = MeasureSpec.getMode(newWidthMeasureSpec)
            newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxDefinedWidthPixels, measureMode)
        }

        val measuredHeight = MeasureSpec.getSize(newHeightMeasureSpec)
        if (minDefinedHeightPixels > 0 && measuredHeight < minDefinedHeightPixels) {
            val measureMode = MeasureSpec.getMode(newHeightMeasureSpec)
            newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(minDefinedHeightPixels, measureMode)
        } else if (maxDefinedHeightPixels > 0 && measuredHeight > maxDefinedHeightPixels) {
            val measureMode = MeasureSpec.getMode(newHeightMeasureSpec)
            newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxDefinedHeightPixels, measureMode)
        }
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
    }
}
