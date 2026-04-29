package com.appboy.sample.networkconsole

import android.content.Context
import com.braze.Braze
import com.braze.events.BrazeNetworkFailureEvent
import com.braze.events.IEventSubscriber
import com.braze.support.BrazeLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Surfaces Braze SDK network activity to the Droidboy in-app console.
 *
 * Rather than scraping `logcat`, this hooks [BrazeLogger.onLoggedCallback], which
 * receives every `brazelog { ... }` call in-process with the complete untruncated
 * message (including the multi-line HTTP request/response bodies that logcat would
 * otherwise split). Each invocation is fed through [BrazeLogEntryClassifier] so only
 * real network log statements are published on [events].
 *
 * The previously installed logger callback (e.g. one set by GeofencesMapActivity) is
 * preserved and chained so starting the console never silences another consumer.
 *
 * A [Braze.subscribeToNetworkFailures] subscription is also kept as a safety net so
 * failure events are still surfaced if a host app lowers the Braze log level below
 * VERBOSE or a future SDK change stops logging a given failure path.
 *
 * `start`, `stop`, and `clear` are expected to be called from the main thread.
 */
class BrazeNetworkLogTailer(
    private val applicationContext: Context,
    private val classifier: BrazeLogEntryClassifier = BrazeLogEntryClassifier()
) {

    private val _events = MutableSharedFlow<NetworkLogEntry>(
        replay = 0,
        extraBufferCapacity = EXTRA_BUFFER
    )

    /**
     * Hot stream of network entries. Carries only newly emitted entries; recent history is
     * surfaced separately via [history] so newly attached consoles can render the backlog
     * without relying on the SharedFlow's replay cache.
     */
    val events: SharedFlow<NetworkLogEntry> = _events.asSharedFlow()

    private val historyLock = Any()
    private val history = ArrayDeque<NetworkLogEntry>(HISTORY_SIZE)

    private var previousLoggerCallback:
        ((BrazeLogger.Priority, String, Throwable?) -> Unit)? = null
    private var installedLoggerCallback:
        ((BrazeLogger.Priority, String, Throwable?) -> Unit)? = null
    private var failureSubscriber: IEventSubscriber<BrazeNetworkFailureEvent>? = null

    val isRunning: Boolean
        get() = installedLoggerCallback != null

    /**
     * Installs the logger callback and the failure subscription. Idempotent.
     */
    fun start() {
        if (isRunning) return
        installLoggerCallback()
        registerFailureSubscriber()
    }

    /**
     * Restores the previous logger callback and removes the failure subscription. Idempotent.
     */
    fun stop() {
        uninstallLoggerCallback()
        unregisterFailureSubscriber()
    }

    /**
     * Snapshot of recently captured entries (oldest first), bounded to [HISTORY_SIZE].
     * Used by newly attached consoles to render history before subscribing to [events].
     */
    fun history(): List<NetworkLogEntry> = synchronized(historyLock) { history.toList() }

    /**
     * Clears the captured history so newly attached consoles start empty.
     */
    fun clear() {
        synchronized(historyLock) { history.clear() }
    }

    private fun publish(entry: NetworkLogEntry) {
        synchronized(historyLock) {
            if (history.size == HISTORY_SIZE) history.removeFirst()
            history.addLast(entry)
        }
        _events.tryEmit(entry)
    }

    private fun installLoggerCallback() {
        previousLoggerCallback = BrazeLogger.onLoggedCallback
        val chainTo = previousLoggerCallback
        val callback: (BrazeLogger.Priority, String, Throwable?) -> Unit =
            { priority, message, throwable ->
                chainTo?.invoke(priority, message, throwable)
                classifier.classify(priority, message, throwable)?.let { entry ->
                    publish(entry)
                }
            }
        installedLoggerCallback = callback
        BrazeLogger.onLoggedCallback = callback
    }

    private fun uninstallLoggerCallback() {
        // Only restore the previous callback when our callback is still the active one.
        // If a host screen replaced it after we installed, leave their callback alone
        // to avoid clobbering a newer consumer.
        if (BrazeLogger.onLoggedCallback === installedLoggerCallback) {
            BrazeLogger.onLoggedCallback = previousLoggerCallback
        }
        installedLoggerCallback = null
        previousLoggerCallback = null
    }

    private fun registerFailureSubscriber() {
        if (failureSubscriber != null) return
        val subscriber = IEventSubscriber<BrazeNetworkFailureEvent> { event ->
            publish(buildFailureEntry(event))
        }
        failureSubscriber = subscriber
        Braze.getInstance(applicationContext).subscribeToNetworkFailures(subscriber)
    }

    private fun unregisterFailureSubscriber() {
        val subscriber = failureSubscriber ?: return
        Braze.getInstance(applicationContext)
            .removeSingleSubscription(subscriber, BrazeNetworkFailureEvent::class.java)
        failureSubscriber = null
    }

    private fun buildFailureEntry(event: BrazeNetworkFailureEvent): NetworkLogEntry {
        val text = event.toString()
        return NetworkLogEntry(
            timestampMillis = System.currentTimeMillis(),
            direction = NetworkLogEntry.Direction.FAILURE,
            tag = "BrazeNetworkFailureEvent",
            message = text,
            rawLine = text
        )
    }

    companion object {
        private const val HISTORY_SIZE = 200
        private const val EXTRA_BUFFER = 500
    }
}
