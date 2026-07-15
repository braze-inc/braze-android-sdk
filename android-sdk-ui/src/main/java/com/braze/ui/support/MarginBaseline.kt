package com.braze.ui.support

import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

/**
 * Stores the original margins of a [MarginLayoutParams] so safe-area offsets can be
 * re-applied idempotently when window inset geometry changes.
 */
internal class MarginBaseline {
    private var baselineInsets: Insets? = null

    fun captureIfNeeded(layoutParams: MarginLayoutParams) {
        if (baselineInsets == null) {
            baselineInsets =
                Insets.of(
                    layoutParams.leftMargin,
                    layoutParams.topMargin,
                    layoutParams.rightMargin,
                    layoutParams.bottomMargin,
                )
        }
    }

    internal fun getOrCapture(layoutParams: MarginLayoutParams): Insets {
        captureIfNeeded(layoutParams)
        return baselineInsets ?: Insets.NONE
    }
}

/**
 * Applies safe-area insets on top of captured baseline margins. Safe to call multiple times
 * with different [insets] values (for example after rotation).
 */
internal fun MarginLayoutParams.applySafeAreaMargins(
    insets: WindowInsetsCompat,
    baseline: MarginBaseline,
    applyTopInset: Boolean = true,
) {
    val base = baseline.getOrCapture(this)
    setMargins(
        base.left + getMaxSafeLeftInset(insets),
        if (applyTopInset) base.top + getMaxSafeTopInset(insets) else base.top,
        base.right + getMaxSafeRightInset(insets),
        base.bottom + getMaxSafeBottomInset(insets),
    )
}
