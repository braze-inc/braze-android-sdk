package com.braze.ui.contentcards

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Offsets the Content Cards feed by the safe area of the window so that cards and the refresh
 * indicator stay clear of the system bars and the display cutout.
 *
 * The original padding of the [RecyclerView] and the original offsets of the [SwipeRefreshLayout]
 * indicator are captured once, then the insets are added on top of them. Repeat calls with
 * different inset values, for example after a rotation, are therefore idempotent.
 */
internal class ContentCardsWindowInsetsApplier(
    private val contentCardsRecyclerView: RecyclerView,
    private val contentCardsSwipeLayout: SwipeRefreshLayout?,
) {
    private val basePadding =
        Insets.of(
            contentCardsRecyclerView.paddingLeft,
            contentCardsRecyclerView.paddingTop,
            contentCardsRecyclerView.paddingRight,
            contentCardsRecyclerView.paddingBottom,
        )

    private val baseProgressViewStartOffset = contentCardsSwipeLayout?.progressViewStartOffset ?: 0

    private val baseProgressViewEndOffset = contentCardsSwipeLayout?.progressViewEndOffset ?: 0

    private var hasOffsetProgressView = false

    /**
     * Applies the safe area of [windowInsets] to the feed.
     *
     * @param windowInsets The [WindowInsetsCompat] object directly from
     * [androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener].
     */
    fun applyWindowInsets(windowInsets: WindowInsetsCompat) {
        val safeAreaInsets =
            windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        contentCardsRecyclerView.setPadding(
            basePadding.left + safeAreaInsets.left,
            basePadding.top + safeAreaInsets.top,
            basePadding.right + safeAreaInsets.right,
            basePadding.bottom + safeAreaInsets.bottom,
        )
        offsetProgressView(safeAreaInsets.top)
    }

    /**
     * Restores the padding and the refresh indicator offsets that the feed had before any insets
     * were applied.
     */
    fun reset() {
        contentCardsRecyclerView.setPadding(
            basePadding.left,
            basePadding.top,
            basePadding.right,
            basePadding.bottom,
        )
        offsetProgressView(0)
    }

    /**
     * Moves the refresh indicator down by [topInset].
     *
     * [SwipeRefreshLayout.setProgressViewOffset] puts the layout into custom start mode, which
     * changes how it derives the indicator start position, so it is called only while a non-zero
     * offset is in effect. A host that consumes the insets before the feed therefore keeps the
     * default pull-to-refresh behavior.
     */
    private fun offsetProgressView(topInset: Int) {
        val swipeLayout = contentCardsSwipeLayout ?: return
        if (topInset == 0 && !hasOffsetProgressView) return
        swipeLayout.setProgressViewOffset(
            false,
            baseProgressViewStartOffset + topInset,
            baseProgressViewEndOffset + topInset,
        )
        hasOffsetProgressView = topInset != 0
    }
}
