package com.appboy.sample.qa.pushunregister

data class PushUnregisterQaAttempt(
    val attemptNumber: Int,
    val succeeded: Boolean,
    val errorMessage: String? = null,
    val isRetriable: Boolean? = null,
    val httpStatusCode: Int? = null,
)

enum class PushUnregisterQaRunOutcome {
    Success,
    FailedRetriableExhausted,
    FailedNonRetriable,
    FailedOther,
}

data class PushUnregisterQaRunResult(
    val attempts: List<PushUnregisterQaAttempt>,
    val outcome: PushUnregisterQaRunOutcome,
    val targetApi: PushUnregisterQaTargetApi,
) {
    fun formatLog(): String {
        val lines = mutableListOf<String>()
        attempts.forEach { attempt ->
            if (attempt.succeeded) {
                lines.add("Attempt ${attempt.attemptNumber}: SUCCESS")
            } else {
                lines.add(
                    "Attempt ${attempt.attemptNumber}: FAILED " +
                        "retriable=${attempt.isRetriable} " +
                        "status=${attempt.httpStatusCode} — ${attempt.errorMessage}",
                )
            }
        }
        lines.add(
            when (outcome) {
                PushUnregisterQaRunOutcome.Success -> "Final: SUCCESS"
                PushUnregisterQaRunOutcome.FailedRetriableExhausted -> "Final: FAILED (retriable attempts exhausted)"
                PushUnregisterQaRunOutcome.FailedNonRetriable -> "Final: FAILED (non-retriable)"
                PushUnregisterQaRunOutcome.FailedOther -> "Final: FAILED (unexpected error)"
            },
        )
        if (targetApi == PushUnregisterQaTargetApi.Logout && outcome == PushUnregisterQaRunOutcome.Success) {
            lines.add("Logout completed: local data wiped and SDK disabled.")
        }
        return lines.joinToString("\n")
    }

    companion object {
        fun formatAttemptsLog(
            attempts: List<PushUnregisterQaAttempt>,
            runningSuffix: String? = null,
        ): String {
            val lines =
                attempts.map { attempt ->
                    if (attempt.succeeded) {
                        "Attempt ${attempt.attemptNumber}: SUCCESS"
                    } else {
                        "Attempt ${attempt.attemptNumber}: FAILED " +
                            "retriable=${attempt.isRetriable} " +
                            "status=${attempt.httpStatusCode} — ${attempt.errorMessage}"
                    }
                }
            return if (runningSuffix.isNullOrBlank()) {
                lines.joinToString("\n")
            } else {
                (lines + runningSuffix).joinToString("\n")
            }
        }
    }
}
