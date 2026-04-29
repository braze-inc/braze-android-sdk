package com.appboy.sample.networkconsole

/**
 * A single entry in the in-app Braze network console.
 *
 * Entries originate from one of two sources:
 *  - A [com.braze.support.BrazeLogger.onLoggedCallback] hook that receives the full
 *    (untruncated) message of every `brazelog { ... }` call in-process. Messages are
 *    filtered to Braze SDK request/response/dispatch lines by [BrazeLogEntryClassifier].
 *  - A subscription to [com.braze.Braze.subscribeToNetworkFailures] that surfaces
 *    failures as a safety net even if a future SDK change stops logging them.
 */
data class NetworkLogEntry(
    val timestampMillis: Long,
    val direction: Direction,
    val tag: String,
    val message: String,
    val rawLine: String,
    val url: String? = null,
    val statusCode: Int? = null
) {
    enum class Direction {
        REQUEST,
        RESPONSE,
        FAILURE,
        DISPATCH,
        OTHER
    }
}
