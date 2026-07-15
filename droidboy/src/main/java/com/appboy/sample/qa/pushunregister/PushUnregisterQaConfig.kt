package com.appboy.sample.qa.pushunregister

import android.content.Context
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsInt
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsString
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsInt
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys

data class PushUnregisterQaConfig(
    val scenario: PushUnregisterQaScenario = PushUnregisterQaScenario.Off,
    val failuresBeforeSuccess: Int = DEFAULT_FAILURES_BEFORE_SUCCESS,
    val maxRetryAttempts: Int = DEFAULT_MAX_RETRY_ATTEMPTS,
    val retryDelayMs: Int = DEFAULT_RETRY_DELAY_MS,
    val targetApi: PushUnregisterQaTargetApi = PushUnregisterQaTargetApi.UnregisterPush,
) {
    fun persist(context: Context) {
        context.writePrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_SCENARIO, scenario.name)
        context.writePrefsInt(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_FAILURES_BEFORE_SUCCESS, failuresBeforeSuccess)
        context.writePrefsInt(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_MAX_RETRY_ATTEMPTS, maxRetryAttempts)
        context.writePrefsInt(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_RETRY_DELAY_MS, retryDelayMs)
        context.writePrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_TARGET_API, targetApi.name)
    }

    companion object {
        const val DEFAULT_FAILURES_BEFORE_SUCCESS = 0
        const val DEFAULT_MAX_RETRY_ATTEMPTS = 3
        const val DEFAULT_RETRY_DELAY_MS = 1000

        fun load(context: Context): PushUnregisterQaConfig =
            PushUnregisterQaConfig(
                scenario =
                    PushUnregisterQaScenario.fromStoredValue(
                        context.readPrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_SCENARIO),
                    ),
                failuresBeforeSuccess =
                    context.readPrefsInt(
                        DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_FAILURES_BEFORE_SUCCESS,
                        DEFAULT_FAILURES_BEFORE_SUCCESS,
                    ),
                maxRetryAttempts =
                    context.readPrefsInt(
                        DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_MAX_RETRY_ATTEMPTS,
                        DEFAULT_MAX_RETRY_ATTEMPTS,
                    ),
                retryDelayMs =
                    context.readPrefsInt(
                        DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_RETRY_DELAY_MS,
                        DEFAULT_RETRY_DELAY_MS,
                    ),
                targetApi =
                    PushUnregisterQaTargetApi.fromStoredValue(
                        context.readPrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_TARGET_API),
                    ),
            )
    }
}
