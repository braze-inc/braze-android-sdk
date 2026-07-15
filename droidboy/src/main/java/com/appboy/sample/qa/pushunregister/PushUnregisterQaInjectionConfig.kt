package com.appboy.sample.qa.pushunregister

import java.util.concurrent.atomic.AtomicInteger

internal data class PushUnregisterQaInjectionConfig(
    val failuresBeforeSuccess: Int,
    val failureResponseCode: Int,
    val failureJsonResponse: String?,
    val attemptCounter: AtomicInteger,
)
