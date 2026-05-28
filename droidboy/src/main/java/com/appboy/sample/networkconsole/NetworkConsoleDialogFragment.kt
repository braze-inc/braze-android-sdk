package com.appboy.sample.networkconsole

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appboy.sample.R
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen dialog that displays Braze SDK network activity captured by
 * [BrazeNetworkLogTailer]. Provides Pause, Clear, and Share controls in the toolbar.
 */
class NetworkConsoleDialogFragment : DialogFragment() {
    private lateinit var adapter: NetworkConsoleAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusView: TextView
    private lateinit var toolbar: MaterialToolbar

    private var collectJob: Job? = null
    private var tailer: BrazeNetworkLogTailer? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.network_console_dialog, container, false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.network_console_toolbar)
        statusView = view.findViewById(R.id.network_console_status)
        recyclerView = view.findViewById(R.id.network_console_recycler)

        adapter = NetworkConsoleAdapter(requireContext(), onEntryClick = ::showEntryDetail)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.network_console_action_pause -> togglePause(item)
                R.id.network_console_action_clear -> clearEntries()
                R.id.network_console_action_share -> shareEntries()
                R.id.network_console_action_close -> dismiss()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }

        startCollecting()
    }

    override fun onDestroyView() {
        collectJob?.cancel()
        collectJob = null
        // Leave the tailer's captured state to the controller; a re-opened console
        // will call controller.start() which is idempotent and re-arms capture if
        // the user paused and dismissed without resuming.
        tailer = null
        super.onDestroyView()
    }

    private fun togglePause(item: android.view.MenuItem) {
        // Pause actually halts capture (uninstalls the BrazeLogger callback and
        // the failure subscriber) rather than letting logs pile up invisibly in
        // the tailer's replay buffer while the UI silently drops them.
        val tailer = this.tailer ?: return
        val shouldPause = tailer.isRunning
        if (shouldPause) {
            tailer.stop()
        } else {
            tailer.start()
        }
        item.title = if (shouldPause) "Resume" else "Pause"
        showToast(if (shouldPause) "Capture paused" else "Capture resumed")
    }

    private fun clearEntries() {
        adapter.clearEntries()
        NetworkConsoleController.current()?.clear()
        updateStatus()
    }

    private fun shareEntries() {
        val context = requireContext()
        val snapshot = adapter.snapshot()
        if (snapshot.isEmpty()) {
            showToast("Nothing to share")
            return
        }
        val appContext = context.applicationContext
        val cacheDir = context.cacheDir
        val packageName = context.packageName
        // Surface immediate feedback so users know something is happening; the toast
        // naturally fades when the IO completes quickly, and remains a useful "still
        // working" signal if the buffer is large enough to take a noticeable moment.
        showToast("Preparing logs to share\u2026")
        viewLifecycleOwner.lifecycleScope.launch {
            // Joining every entry (some multi-KB) and writing to disk is big enough
            // to noticeably block the UI thread when the buffer is full, so push
            // the string build and the file I/O to a background dispatcher.
            val uri =
                withContext(Dispatchers.IO) {
                    val text =
                        snapshot.joinToString(separator = "\n") { entry ->
                            "[${formatTimestamp(entry.timestampMillis)}] " +
                                "${entry.direction} ${entry.tag}: ${entry.message}"
                        }
                    val exportDir = File(cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
                    val outputFile =
                        File(
                            exportDir,
                            "network_console_${System.currentTimeMillis()}.txt",
                        ).apply { writeText(text) }
                    FileProvider.getUriForFile(appContext, "$packageName.fileprovider", outputFile)
                }
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            startActivity(Intent.createChooser(shareIntent, "Share network console"))
        }
    }

    private fun showEntryDetail(entry: NetworkLogEntry) {
        val context = requireContext()
        val padding = (DETAIL_PADDING_DP * context.resources.displayMetrics.density).toInt()
        val messageView =
            TextView(context).apply {
                text = buildDetailText(entry)
                textSize = DETAIL_TEXT_SIZE_SP
                setTextIsSelectable(true)
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(padding, padding, padding, padding)
            }
        val scrollContainer = ScrollView(context).apply { addView(messageView) }
        AlertDialog
            .Builder(context)
            .setTitle("${entry.direction} ${entry.tag}")
            .setView(scrollContainer)
            .setPositiveButton("Copy") { dialog, _ ->
                copyEntryToClipboard(entry)
                dialog.dismiss()
            }.setNegativeButton("Close", null)
            .show()
    }

    private fun buildDetailText(entry: NetworkLogEntry): String =
        buildString {
            append(formatTimestamp(entry.timestampMillis)).append('\n')
            entry.url?.let { append("URL: ").append(it).append('\n') }
            entry.statusCode?.let { append("Status: ").append(it).append('\n') }
            if (isNotEmpty()) append('\n')
            append(entry.message)
        }

    private fun copyEntryToClipboard(entry: NetworkLogEntry) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Braze network entry", entry.rawLine))
        showToast("Copied to clipboard")
    }

    private fun startCollecting() {
        val tailer = NetworkConsoleController.start(requireContext().applicationContext)
        this.tailer = tailer

        // tailer.events carries only NEW entries (replay = 0); the recent backlog is
        // pulled explicitly from tailer.history() so we don't need the experimental
        // resetReplayCache() API to wipe a SharedFlow cache on Clear, and we don't
        // duplicate entries on dialog re-open the way a replay-enabled flow would.
        adapter.appendAll(tailer.history())
        updateStatus()

        collectJob =
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    tailer.events.collect { entry ->
                        // Capture "was the user already at/near the tail?" BEFORE the
                        // insertion so the newly appended row doesn't itself make us
                        // think we're no longer at the bottom.
                        val wasFollowingTail = isRecyclerAtTail()
                        adapter.append(entry)
                        if (wasFollowingTail) {
                            recyclerView.scrollToPosition(adapter.itemCount - 1)
                        }
                        updateStatus()
                    }
                }
            }
    }

    /**
     * Returns true when the user is at or within [AUTO_SCROLL_THRESHOLD] rows of the
     * tail of the list, or when the list was empty. Used to avoid yanking the
     * viewport back to the bottom every time a new log arrives while the user is
     * scrolled up inspecting earlier entries.
     */
    private fun isRecyclerAtTail(): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return true
        val itemCount = adapter.itemCount
        if (itemCount == 0) return true
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (lastVisible == RecyclerView.NO_POSITION) return true
        return lastVisible >= itemCount - 1 - AUTO_SCROLL_THRESHOLD
    }

    private fun updateStatus() {
        statusView.text = "${adapter.itemCount} entries"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun formatTimestamp(millis: Long): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(millis))

    companion object {
        const val TAG = "NetworkConsoleDialogFragment"
        private const val EXPORT_DIRECTORY = "network_console_exports"
        private const val DETAIL_PADDING_DP = 16
        private const val DETAIL_TEXT_SIZE_SP = 12f

        // How close to the tail the user must be for an incoming log to still
        // auto-scroll the viewport to the bottom. Anything past this threshold
        // leaves the scroll position alone so the user can read earlier entries.
        private const val AUTO_SCROLL_THRESHOLD = 2
    }
}
