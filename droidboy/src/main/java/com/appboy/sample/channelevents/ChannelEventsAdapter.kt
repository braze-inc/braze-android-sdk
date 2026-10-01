package com.appboy.sample.channelevents

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.appboy.sample.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun TextView.showLabeled(
    label: String,
    value: String?,
) {
    if (value == null) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    text = "$label: $value"
}

private fun TextView.showWhenPresent(value: String) {
    if (value.isEmpty()) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    text = value
}

/**
 * Newest-first feed of [ChannelEventRow] values, optionally filtered by [channelFilter].
 */
class ChannelEventsAdapter(
    context: Context,
    maxEntries: Int = ChannelEventsFeed.DEFAULT_MAX_ENTRIES,
    private val onRowClick: (ChannelEventRow) -> Unit,
) : RecyclerView.Adapter<ChannelEventsAdapter.ViewHolder>() {
    private val feed = ChannelEventsFeed(maxEntries)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val palette = KindPalette(context)

    var channelFilter: EventsChannel?
        get() = feed.channelFilter
        set(value) {
            if (feed.channelFilter == value) return
            feed.channelFilter = value
            notifyDataSetChanged()
        }

    /**
     * Inserts [row] at the newest edge.
     *
     * @return true if [row] is in the currently filtered visible list.
     */
    fun prepend(row: ChannelEventRow): Boolean {
        val result = feed.prepend(row)
        if (result.visibleChanged) {
            notifyDataSetChanged()
        }
        return result.newRowVisible
    }

    fun clearRows() {
        if (feed.clear()) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.channel_event_row, parent, false)
        return ViewHolder(view, palette)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val row = feed.visibleRows()[position]
        holder.bind(row, timeFormatter)
        holder.itemView.setOnClickListener { onRowClick(row) }
    }

    override fun getItemCount(): Int = feed.visibleRows().size

    class ViewHolder(
        view: View,
        private val palette: KindPalette,
    ) : RecyclerView.ViewHolder(view) {
        private val timestampView: TextView = view.findViewById(R.id.channel_event_timestamp)
        private val domainView: TextView = view.findViewById(R.id.channel_event_name)
        private val typeView: TextView = view.findViewById(R.id.channel_event_type)
        private val reasonView: TextView = view.findViewById(R.id.channel_event_reason)
        private val retryStateView: TextView = view.findViewById(R.id.channel_event_retry_state)
        private val actionView: TextView = view.findViewById(R.id.channel_event_action)
        private val idView: TextView = view.findViewById(R.id.channel_event_id)
        private val placementIdView: TextView = view.findViewById(R.id.channel_event_placement_id)
        private val buttonIdView: TextView = view.findViewById(R.id.channel_event_button_id)
        private val userIdView: TextView = view.findViewById(R.id.channel_event_user_id)
        private val summaryView: TextView = view.findViewById(R.id.channel_event_summary)

        fun bind(
            row: ChannelEventRow,
            timeFormatter: SimpleDateFormat,
        ) {
            timestampView.text = timeFormatter.format(Date(row.timestampMillis))
            domainView.text = row.channel.displayName
            typeView.text = row.eventType
            typeView.setTextColor(palette.colorFor(row.kind))
            reasonView.showLabeled("reason", row.reason)
            retryStateView.showLabeled("retryState", row.retryState)
            actionView.showLabeled("action", row.action)
            idView.showLabeled("id", row.id)
            placementIdView.showLabeled("placementId", row.placementId)
            buttonIdView.showLabeled("buttonId", row.buttonId)
            userIdView.showLabeled("userId", row.userId)
            summaryView.showWhenPresent(row.summary)
        }

        private fun TextView.showLabeled(
            label: String,
            value: String?,
        ) {
            if (value.isNullOrBlank()) {
                visibility = View.GONE
                return
            }
            text = "$label: $value"
            visibility = View.VISIBLE
        }

        private fun TextView.showWhenPresent(value: String) {
            if (value.isBlank()) {
                visibility = View.GONE
                return
            }
            text = value
            visibility = View.VISIBLE
        }
    }

    class KindPalette(
        private val context: Context,
    ) {
        @ColorInt
        fun colorFor(kind: EventsKind): Int =
            when (kind) {
                EventsKind.CACHE_REPLAY,
                EventsKind.CACHE_LOAD,
                -> Color.DKGRAY
                EventsKind.DATA_UPDATED -> color(R.color.network_console_request)
                EventsKind.ANALYTICS -> color(R.color.network_console_response)
                EventsKind.ERROR -> color(R.color.network_console_failure)
            }

        @ColorInt
        private fun color(colorId: Int): Int = ContextCompat.getColor(context, colorId)
    }
}
