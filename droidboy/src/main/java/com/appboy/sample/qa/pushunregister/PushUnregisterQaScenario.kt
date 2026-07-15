package com.appboy.sample.qa.pushunregister

enum class PushUnregisterQaScenario {
    Off,
    NoNetwork,
    Http400,
    Http429,
    Http503,
    ;

    fun toFailureResponse(): Pair<Int, String?>? =
        when (this) {
            Off -> null
            // Matches [com.braze.communication.HttpConnector.post], which leaves responseCode at -1 on failure.
            NoNetwork -> NETWORK_FAILURE_RESPONSE_CODE to null
            Http400 -> HTTP_400 to """{"error":"bad_request"}"""
            Http429 -> HTTP_429 to null
            Http503 -> HTTP_503 to null
        }

    companion object {
        private const val NETWORK_FAILURE_RESPONSE_CODE = -1
        private const val HTTP_400 = 400
        private const val HTTP_429 = 429
        private const val HTTP_503 = 503

        fun fromStoredValue(value: String?): PushUnregisterQaScenario = entries.find { it.name == value } ?: Off
    }
}
