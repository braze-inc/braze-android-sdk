package com.appboy.sample.channelevents

/**
 * Newest-first in-memory feed for the DroidBoy Events tab.
 */
internal class ChannelEventsFeed(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    private val allRows = ArrayDeque<ChannelEventRow>(maxEntries)
    private val visible = mutableListOf<ChannelEventRow>()

    var channelFilter: EventsChannel? = null
        set(value) {
            if (field == value) return
            field = value
            rebuildVisible()
        }

    fun visibleRows(): List<ChannelEventRow> = visible

    fun prepend(row: ChannelEventRow): ChannelEventsPrependResult {
        val evicted = if (allRows.size >= maxEntries) allRows.removeLast() else null
        allRows.addFirst(row)
        val newRowVisible = isVisible(row)
        val evictedWasVisible = evicted != null && isVisible(evicted)
        if (!newRowVisible && !evictedWasVisible) {
            return ChannelEventsPrependResult(newRowVisible = false, visibleChanged = false)
        }
        rebuildVisible()
        return ChannelEventsPrependResult(newRowVisible = newRowVisible, visibleChanged = true)
    }

    fun clear(): Boolean {
        if (allRows.isEmpty()) return false
        allRows.clear()
        rebuildVisible()
        return true
    }

    private fun isVisible(row: ChannelEventRow): Boolean {
        val filter = channelFilter
        return filter == null || row.channel == filter
    }

    private fun rebuildVisible() {
        visible.clear()
        val filter = channelFilter
        if (filter == null) {
            visible.addAll(allRows)
        } else {
            visible.addAll(allRows.filter { it.channel == filter })
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 200
    }
}

internal data class ChannelEventsPrependResult(
    val newRowVisible: Boolean,
    val visibleChanged: Boolean,
)

/**
 * Newest-first lists treat the live edge as the top. Empty lists and NO_POSITION (-1) count as following.
 */
internal fun isNewestFirstLiveEdge(firstVisiblePosition: Int): Boolean = firstVisiblePosition <= 0
