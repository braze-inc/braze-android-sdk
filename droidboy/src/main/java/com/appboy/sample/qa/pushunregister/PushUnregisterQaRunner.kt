package com.appboy.sample.qa.pushunregister

import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.push.BrazePushUnregistrationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

object PushUnregisterQaRunner {
    suspend fun run(
        config: PushUnregisterQaConfig,
        braze: Braze,
        onProgress: (suspend (List<PushUnregisterQaAttempt>) -> Unit)? = null,
    ): PushUnregisterQaRunResult {
        val attempts = mutableListOf<PushUnregisterQaAttempt>()
        val attemptCounter = AtomicInteger(0)

        suspend fun reportProgress() {
            onProgress?.invoke(attempts.toList())
        }

        try {
            installConnector(config, attemptCounter)

            var lastError: Throwable? = null
            repeat(config.maxRetryAttempts.coerceAtLeast(1)) { index ->
                val attemptNumber = index + 1
                val result =
                    runCatching {
                        when (config.targetApi) {
                            PushUnregisterQaTargetApi.UnregisterPush -> braze.unregisterPush()
                            PushUnregisterQaTargetApi.Logout -> braze.logout()
                        }
                    }

                result
                    .onSuccess {
                        attempts.add(PushUnregisterQaAttempt(attemptNumber, succeeded = true))
                        reportProgress()
                        return PushUnregisterQaRunResult(
                            attempts = attempts,
                            outcome = PushUnregisterQaRunOutcome.Success,
                            targetApi = config.targetApi,
                        )
                    }.onFailure { error ->
                        if (error is CancellationException) {
                            throw error
                        }
                        lastError = error
                        val pushError = error as? BrazePushUnregistrationException
                        attempts.add(
                            PushUnregisterQaAttempt(
                                attemptNumber = attemptNumber,
                                succeeded = false,
                                errorMessage = error.message,
                                isRetriable = pushError?.isRetriable,
                                httpStatusCode = pushError?.httpStatusCode,
                            ),
                        )
                        reportProgress()
                        if (pushError?.isRetriable != true) {
                            return PushUnregisterQaRunResult(
                                attempts = attempts,
                                outcome =
                                    if (pushError != null) {
                                        PushUnregisterQaRunOutcome.FailedNonRetriable
                                    } else {
                                        PushUnregisterQaRunOutcome.FailedOther
                                    },
                                targetApi = config.targetApi,
                            )
                        }
                        if (attemptNumber < config.maxRetryAttempts) {
                            delay(config.retryDelayMs.toLong().coerceAtLeast(0))
                        }
                    }
            }

            return PushUnregisterQaRunResult(
                attempts = attempts,
                outcome =
                    if ((lastError as? BrazePushUnregistrationException)?.isRetriable == true) {
                        PushUnregisterQaRunOutcome.FailedRetriableExhausted
                    } else {
                        PushUnregisterQaRunOutcome.FailedOther
                    },
                targetApi = config.targetApi,
            )
        } finally {
            BrazeInternal.setHttpConnectorOverride(null)
        }
    }

    private fun installConnector(
        config: PushUnregisterQaConfig,
        attemptCounter: AtomicInteger,
    ) {
        val failureResponse = config.scenario.toFailureResponse()
        if (failureResponse == null) {
            BrazeInternal.setHttpConnectorOverride(null)
            return
        }
        val (responseCode, jsonBody) = failureResponse
        BrazeInternal.setHttpConnectorOverride(
            PushUnregisterQaInjectingHttpConnector(
                config =
                    PushUnregisterQaInjectionConfig(
                        failuresBeforeSuccess = config.failuresBeforeSuccess,
                        failureResponseCode = responseCode,
                        failureJsonResponse = jsonBody,
                        attemptCounter = attemptCounter,
                    ),
                delegate = BrazeInternal.getHttpConnector(),
            ),
        )
    }
}
