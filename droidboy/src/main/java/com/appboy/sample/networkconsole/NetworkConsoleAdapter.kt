package com.appboy.sample.networkconsole

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

/**
 * RecyclerView adapter for the Droidboy in-app network console.
 *
 * Backed by a fixed-capacity ring of [NetworkLogEntry] entries to bound memory usage.
 */
class NetworkConsoleAdapter(
    context: Context,
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val onEntryClick: (NetworkLogEntry) -> Unit = {},
) : RecyclerView.Adapter<NetworkConsoleAdapter.ViewHolder>() {
    private val entries = ArrayDeque<NetworkLogEntry>(maxEntries)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val palette = DirectionPalette(context)

    fun appendAll(newEntries: List<NetworkLogEntry>) {
        if (newEntries.isEmpty()) return
        newEntries.forEach { appendInternal(it) }
        notifyDataSetChanged()
    }

    fun append(entry: NetworkLogEntry) {
        val didEvict = appendInternal(entry)
        // Notify the removal before the insertion so RecyclerView sees a consistent
        // sequence of positional mutations (position 0 evicted, then new tail inserted).
        if (didEvict) {
            notifyItemRemoved(0)
        }
        notifyItemInserted(entries.size - 1)
    }

    fun clearEntries() {
        val previousSize = entries.size
        if (previousSize == 0) return
        entries.clear()
        notifyItemRangeRemoved(0, previousSize)
    }

    fun snapshot(): List<NetworkLogEntry> = entries.toList()

    /**
     * Adds [entry] to the ring, evicting the oldest entry if we're already at capacity.
     *
     * @return true when an existing entry was evicted to make room.
     */
    private fun appendInternal(entry: NetworkLogEntry): Boolean {
        val didEvict = entries.size >= maxEntries
        if (didEvict) {
            entries.removeFirst()
        }
        entries.addLast(entry)
        return didEvict
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.network_console_item, parent, false)
        return ViewHolder(view, palette)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val entry = entries[position]
        holder.bind(entry, timeFormatter)
        holder.itemView.setOnClickListener { onEntryClick(entry) }
    }

    override fun getItemCount(): Int = entries.size

    class ViewHolder(
        view: View,
        private val palette: DirectionPalette,
    ) : RecyclerView.ViewHolder(view) {
        private val directionView: TextView = view.findViewById(R.id.network_console_item_direction)
        private val timestampView: TextView = view.findViewById(R.id.network_console_item_timestamp)
        private val statusView: TextView = view.findViewById(R.id.network_console_item_status)
        private val urlView: TextView = view.findViewById(R.id.network_console_item_url)
        private val messageView: TextView = view.findViewById(R.id.network_console_item_message)

        fun bind(
            entry: NetworkLogEntry,
            timeFormatter: SimpleDateFormat,
        ) {
            directionView.text = entry.direction.name
            directionView.setTextColor(palette.colorFor(entry.direction))
            timestampView.text = timeFormatter.format(Date(entry.timestampMillis))
            statusView.text = entry.statusCode?.let { "[$it]" }.orEmpty()
            entry.url?.let {
                urlView.visibility = View.VISIBLE
                urlView.text = it
            } ?: run {
                urlView.visibility = View.GONE
            }
            messageView.text = entry.message
        }
    }

    /**
     * Resolves direction-tag colors once at adapter construction so we don't hit the
     * resource system on every bind while the user scrolls a long history.
     */
    class DirectionPalette(
        context: Context,
    ) {
        @ColorInt private val request = ContextCompat.getColor(context, R.color.network_console_request)

        @ColorInt private val response = ContextCompat.getColor(context, R.color.network_console_response)

        @ColorInt private val failure = ContextCompat.getColor(context, R.color.network_console_failure)

        @ColorInt private val dispatch = ContextCompat.getColor(context, R.color.network_console_dispatch)

        @ColorInt private val other = Color.DKGRAY

        @ColorInt
        fun colorFor(direction: NetworkLogEntry.Direction): Int =
            when (direction) {
                NetworkLogEntry.Direction.REQUEST -> request
                NetworkLogEntry.Direction.RESPONSE -> response
                NetworkLogEntry.Direction.FAILURE -> failure
                NetworkLogEntry.Direction.DISPATCH -> dispatch
                NetworkLogEntry.Direction.OTHER -> other
            }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 500
    }
}
