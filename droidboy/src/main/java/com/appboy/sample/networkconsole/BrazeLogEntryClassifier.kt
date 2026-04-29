package com.appboy.sample.networkconsole

import com.braze.support.BrazeLogger

/**
 * Classifies a single `BrazeLogger.onLoggedCallback` invocation into a [NetworkLogEntry]
 * when the message represents Braze SDK network activity.
 *
 * The classifier matches against the exact log strings emitted by the SDK so the in-app
 * console only surfaces true request/response/dispatch/failure events and not unrelated
 * Braze logs (trigger manager chatter, session housekeeping, geofence lifecycle, etc.).
 *
 * Unlike the previous logcat-based parser this sees the full message (no 4 KB logcat
 * truncation and no per-line fragmentation), so the resulting [NetworkLogEntry.message]
 * preserves the complete multi-line body that the SDK logged.
 */
class BrazeLogEntryClassifier {

    /**
     * Returns a [NetworkLogEntry] when [message] is a Braze network log we want to show,
     * otherwise null.
     */
    fun classify(
        priority: BrazeLogger.Priority,
        message: String,
        throwable: Throwable?
    ): NetworkLogEntry? {
        val classification = classifyMessage(message) ?: return null
        return NetworkLogEntry(
            timestampMillis = System.currentTimeMillis(),
            direction = classification.direction,
            tag = classification.tag,
            message = message,
            rawLine = if (throwable == null) {
                "[${priority.name}] $message"
            } else {
                "[${priority.name}] $message\n${throwable.stackTraceToString()}"
            },
            url = extractUrl(message),
            statusCode = extractStatusCode(message)
        )
    }

    @Suppress("ReturnCount")
    private fun classifyMessage(message: String): Classification? {
        // HTTP request/response pairs logged by LoggedHttpDecorator. These are the most
        // useful entries: they carry headers, URL, and the full JSON payload/body.
        if (message.startsWith(MAKING_REQUEST_PREFIX)) {
            return Classification(NetworkLogEntry.Direction.REQUEST, TAG_HTTP)
        }
        if (message.startsWith(MADE_REQUEST_PREFIX)) {
            return Classification(NetworkLogEntry.Direction.RESPONSE, TAG_HTTP)
        }
        // Request framework lifecycle (RequestFramework.kt): success/failure resolutions.
        if (message.startsWith(REQUEST_SUCCESS_PREFIX)) {
            return Classification(NetworkLogEntry.Direction.DISPATCH, TAG_FRAMEWORK)
        }
        if (message.startsWith(REQUEST_FAILURE_PREFIX)) {
            return Classification(NetworkLogEntry.Direction.FAILURE, TAG_FRAMEWORK)
        }
        // Request URI hint logged by BrazeRequestBase before each dispatch.
        if (message.startsWith(REQUEST_URI_PREFIX)) {
            return Classification(NetworkLogEntry.Direction.REQUEST, TAG_FRAMEWORK)
        }
        // Dispatch lifecycle events logged by EventMessenger.publish as "<class> fired:\n<body>".
        return classifyDispatchEvent(message)
    }

    private fun classifyDispatchEvent(message: String): Classification? {
        if (!message.contains(EVENT_FIRED_MARKER)) return null
        return when {
            message.contains(REQUEST_DISPATCH_STARTED_EVENT) ->
                Classification(NetworkLogEntry.Direction.REQUEST, TAG_EVENT)
            message.contains(DISPATCH_SUCCEEDED_EVENT) ->
                Classification(NetworkLogEntry.Direction.DISPATCH, TAG_EVENT)
            message.contains(REQUEST_DISPATCH_COMPLETED_EVENT) ->
                Classification(NetworkLogEntry.Direction.DISPATCH, TAG_EVENT)
            message.contains(DISPATCH_COMMAND_EVENT) ->
                Classification(NetworkLogEntry.Direction.DISPATCH, TAG_EVENT)
            message.contains(DISPATCH_FAILED_EVENT) ->
                Classification(NetworkLogEntry.Direction.FAILURE, TAG_EVENT)
            else -> null
        }
    }

    private fun extractUrl(message: String): String? =
        URL_REGEX.find(message)
            ?.value
            ?.trimEnd(*URL_TRAILING_PUNCTUATION)
            ?.takeIf { it.isNotEmpty() }

    private fun extractStatusCode(message: String): Int? {
        val match = STATUS_CODE_REGEX.find(message) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    private data class Classification(
        val direction: NetworkLogEntry.Direction,
        val tag: String
    )

    companion object {
        // LoggedHttpDecorator.logRequest / logResponse
        private const val MAKING_REQUEST_PREFIX = "Making request with id =>"
        private const val MADE_REQUEST_PREFIX = "Made request with id =>"

        // RequestFramework.kt
        private const val REQUEST_SUCCESS_PREFIX = "Request success received for"
        private const val REQUEST_FAILURE_PREFIX = "Request failure received"

        // BrazeRequestBase.kt
        private const val REQUEST_URI_PREFIX = ">> Request Uri:"

        // EventMessenger.publish: "<EventClass> fired:\n<toString>"
        private const val EVENT_FIRED_MARKER = "fired:"
        private const val REQUEST_DISPATCH_STARTED_EVENT = "RequestDispatchStartedEvent"
        private const val REQUEST_DISPATCH_COMPLETED_EVENT = "RequestDispatchCompletedEvent"
        private const val DISPATCH_COMMAND_EVENT = "DispatchCommandEvent"
        private const val DISPATCH_SUCCEEDED_EVENT = "DispatchSucceededEvent"
        private const val DISPATCH_FAILED_EVENT = "DispatchFailedEvent"

        private const val TAG_HTTP = "BrazeHttp"
        private const val TAG_FRAMEWORK = "BrazeRequestFramework"
        private const val TAG_EVENT = "BrazeEvent"

        private val URL_REGEX = Regex("https?://[^\\s\"'<>]+")

        // Punctuation we strip from the tail of a captured URL. These characters
        // rarely terminate a real URL in logs but are commonly used as sentence
        // or list delimiters after one (e.g. `...to url: https://host/path/,`).
        // `/` is deliberately excluded because it is a legitimate trailing
        // character for API URLs like `https://elsa.braze.com/api/v3/data/`.
        private val URL_TRAILING_PUNCTUATION =
            charArrayOf('.', ',', ';', ':', ')', ']', '}', '!', '?', '>', '\'', '"')

        private val STATUS_CODE_REGEX =
            Regex("(?:status(?:\\s*code)?|response code)[^0-9]*([0-9]{3})", RegexOption.IGNORE_CASE)
    }
}
