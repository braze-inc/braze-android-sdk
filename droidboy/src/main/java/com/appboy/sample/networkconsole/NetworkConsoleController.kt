package com.appboy.sample.networkconsole

import android.content.Context

/**
 * Process-wide singleton owning the lifetime of the [BrazeNetworkLogTailer] used by the
 * Droidboy in-app network console. Keeping a single tailer lets the captured history
 * survive across open/close cycles so a user who dismisses and re-opens the console
 * still sees the recent backlog, and avoids installing the logger callback more than once.
 *
 * All entry points are expected to be called from the main thread; the singleton holds
 * no cross-thread state.
 */
object NetworkConsoleController {
    private var tailer: BrazeNetworkLogTailer? = null

    fun start(context: Context): BrazeNetworkLogTailer {
        val existing = tailer
        if (existing != null) {
            existing.start()
            return existing
        }
        val created = BrazeNetworkLogTailer(context.applicationContext)
        created.start()
        tailer = created
        return created
    }

    fun stop() {
        tailer?.stop()
    }

    fun current(): BrazeNetworkLogTailer? = tailer
}
