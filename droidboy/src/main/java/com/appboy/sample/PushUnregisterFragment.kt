package com.appboy.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appboy.sample.qa.pushunregister.PushUnregisterQaConfig
import com.appboy.sample.qa.pushunregister.PushUnregisterQaRunResult
import com.appboy.sample.qa.pushunregister.PushUnregisterQaRunner
import com.appboy.sample.qa.pushunregister.PushUnregisterQaScenario
import com.appboy.sample.qa.pushunregister.PushUnregisterQaTargetApi
import com.appboy.sample.util.DroidboyDataStoreUtils.readPrefsString
import com.appboy.sample.util.DroidboyDataStoreUtils.writePrefsString
import com.appboy.sample.util.DroidboyPreferenceKeys
import com.braze.Braze
import com.braze.push.BrazePushUnregistrationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PushUnregisterFragment : Fragment() {
    private lateinit var pushTokenStatusView: TextView
    private lateinit var sdkEnabledStatusView: TextView
    private lateinit var sdkToggleButton: Button
    private lateinit var scenarioSpinner: Spinner
    private lateinit var failuresBeforeSuccessField: EditText
    private lateinit var maxRetriesField: EditText
    private lateinit var retryDelayField: EditText
    private lateinit var targetApiSpinner: Spinner
    private lateinit var attemptLogView: TextView
    private lateinit var qaRunButton: Button
    private var sdkToggleCooldownActive = false
    private var qaRunInProgress = false
    private var suppressSpinnerCallbacks = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.push_unregister_tester, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        pushTokenStatusView = view.findViewById(R.id.push_unregister_status_push_token)
        sdkEnabledStatusView = view.findViewById(R.id.push_unregister_status_sdk_enabled)
        sdkToggleButton = view.findViewById(R.id.push_unregister_toggle_sdk_button)
        scenarioSpinner = view.findViewById(R.id.push_unregister_qa_scenario_spinner)
        failuresBeforeSuccessField = view.findViewById(R.id.push_unregister_qa_failures_before_success)
        maxRetriesField = view.findViewById(R.id.push_unregister_qa_max_retries)
        retryDelayField = view.findViewById(R.id.push_unregister_qa_retry_delay_ms)
        targetApiSpinner = view.findViewById(R.id.push_unregister_qa_target_api_spinner)
        attemptLogView = view.findViewById(R.id.push_unregister_qa_attempt_log)
        qaRunButton = view.findViewById(R.id.push_unregister_qa_run_button)

        setupSpinners()
        loadPersistedConfig()
        refreshStatus()

        sdkToggleButton.setOnClickListener {
            toggleSdkEnabledState()
        }
        view.findViewById<Button>(R.id.push_unregister_primitive_button).setOnClickListener {
            runPrimitiveUnregisterPush()
        }
        view.findViewById<Button>(R.id.push_unregister_primitive_logout_button).setOnClickListener {
            runPrimitiveLogout()
        }
        view.findViewById<Button>(R.id.push_unregister_qa_run_button).setOnClickListener {
            runQaScenario()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun setupSpinners() {
        scenarioSpinner.preventScrollableParentFromStealingTouches()
        targetApiSpinner.preventScrollableParentFromStealingTouches()
        scenarioSpinner.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.push_unregister_qa_scenario_entries),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        targetApiSpinner.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.push_unregister_qa_target_api_entries),
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        scenarioSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (suppressSpinnerCallbacks) return
                    persistCurrentConfig()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        targetApiSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (suppressSpinnerCallbacks) return
                    persistCurrentConfig()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun loadPersistedConfig() {
        val config = PushUnregisterQaConfig.load(requireContext())
        suppressSpinnerCallbacks = true
        scenarioSpinner.setSelection(scenarioValueToIndex(config.scenario.name))
        targetApiSpinner.setSelection(targetApiValueToIndex(config.targetApi.name))
        suppressSpinnerCallbacks = false
        failuresBeforeSuccessField.setText(config.failuresBeforeSuccess.toString())
        maxRetriesField.setText(config.maxRetryAttempts.toString())
        retryDelayField.setText(config.retryDelayMs.toString())
        val savedLog =
            requireContext().readPrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_LAST_LOG)
        if (!savedLog.isNullOrBlank()) {
            attemptLogView.text = savedLog
        }
    }

    private fun persistCurrentConfig() {
        if (!isAdded) return
        readConfigFromUi().persist(requireContext())
    }

    private fun readConfigFromUi(): PushUnregisterQaConfig =
        PushUnregisterQaConfig(
            scenario =
                PushUnregisterQaScenario.fromStoredValue(
                    resources.getStringArray(R.array.push_unregister_qa_scenario_values)[scenarioSpinner.selectedItemPosition],
                ),
            failuresBeforeSuccess = failuresBeforeSuccessField.text.toString().toIntOrNull() ?: 0,
            maxRetryAttempts =
                maxRetriesField.text.toString().toIntOrNull()
                    ?: PushUnregisterQaConfig.DEFAULT_MAX_RETRY_ATTEMPTS,
            retryDelayMs =
                retryDelayField.text.toString().toIntOrNull()
                    ?: PushUnregisterQaConfig.DEFAULT_RETRY_DELAY_MS,
            targetApi =
                PushUnregisterQaTargetApi.fromStoredValue(
                    resources.getStringArray(R.array.push_unregister_qa_target_api_values)[targetApiSpinner.selectedItemPosition],
                ),
        )

    private fun refreshStatus() {
        if (!isAdded) return
        val context = requireContext().applicationContext
        val sdkEnabled = !Braze.isDisabled
        sdkEnabledStatusView.text =
            getString(
                R.string.push_unregister_status_sdk_enabled,
                getString(
                    if (sdkEnabled) {
                        R.string.push_unregister_status_sdk_enabled_value
                    } else {
                        R.string.push_unregister_status_sdk_disabled_value
                    },
                ),
            )
        val pushToken =
            if (sdkEnabled) {
                Braze.getInstance(context).registeredPushToken
            } else {
                null
            }
        pushTokenStatusView.text =
            getString(
                R.string.push_unregister_status_push_token,
                pushToken?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.push_unregister_status_no_token),
            )
        updateSdkToggleButton(sdkEnabled)
    }

    private fun updateSdkToggleButton(sdkEnabled: Boolean) {
        if (!isAdded) return
        sdkToggleButton.text =
            getString(
                if (sdkEnabled) {
                    R.string.push_unregister_disable_sdk_button
                } else {
                    R.string.push_unregister_reenable_sdk_button
                },
            )
        sdkToggleButton.isEnabled = !sdkToggleCooldownActive
    }

    private fun toggleSdkEnabledState() {
        if (sdkToggleCooldownActive) return
        val context = requireContext().applicationContext
        if (Braze.isDisabled) {
            Braze.enableSdk(context)
            Braze.getInstance(context)
            val userId = context.readPrefsString(DroidboyPreferenceKeys.USER_ID)
            if (!userId.isNullOrBlank()) {
                (context as DroidboyApplication).changeUserWithNewSdkAuthToken(userId)
                showToast(getString(R.string.push_unregister_reenable_sdk_success))
            } else {
                showToast(getString(R.string.push_unregister_reenable_sdk_no_user))
            }
            refreshStatus()
            lifecycleScope.launch {
                delay(PUSH_TOKEN_REFRESH_DELAY_MS)
                refreshStatus()
            }
        } else {
            Braze.disableSdk(context)
            showToast(getString(R.string.push_unregister_disable_sdk_success))
            refreshStatus()
        }
        startSdkToggleCooldown()
    }

    private fun startSdkToggleCooldown() {
        sdkToggleCooldownActive = true
        sdkToggleButton.isEnabled = false
        lifecycleScope.launch {
            delay(SDK_TOGGLE_COOLDOWN_MS)
            sdkToggleCooldownActive = false
            refreshStatus()
        }
    }

    private fun runPrimitiveUnregisterPush() {
        val context = requireContext()
        lifecycleScope.launch {
            runCatching { Braze.getInstance(context).unregisterPush() }
                .onSuccess {
                    showToast(getString(R.string.push_unregister_unregister_success))
                    refreshStatus()
                }.onFailure { error ->
                    val isRetriable = (error as? BrazePushUnregistrationException)?.isRetriable
                    showToast(
                        getString(
                            R.string.push_unregister_unregister_failed,
                            isRetriable.toString(),
                            error.message,
                        ),
                    )
                }
        }
    }

    private fun runPrimitiveLogout() {
        val context = requireContext()
        lifecycleScope.launch {
            runCatching { Braze.getInstance(context).logout() }
                .onSuccess {
                    showToast(getString(R.string.push_unregister_logout_success))
                    refreshStatus()
                }.onFailure { error ->
                    val isRetriable = (error as? BrazePushUnregistrationException)?.isRetriable
                    showToast(
                        getString(
                            R.string.push_unregister_logout_failed,
                            isRetriable.toString(),
                            error.message,
                        ),
                    )
                    refreshStatus()
                }
        }
    }

    private fun runQaScenario() {
        if (qaRunInProgress) return
        val context = requireContext()
        if (Braze.isDisabled) {
            showToast(getString(R.string.push_unregister_qa_precondition_sdk_disabled))
            return
        }
        val pushToken = Braze.getInstance(context).registeredPushToken
        if (pushToken.isNullOrBlank()) {
            showToast(getString(R.string.push_unregister_qa_precondition_no_token))
            return
        }

        val config = readConfigFromUi()
        config.persist(context)
        qaRunInProgress = true
        qaRunButton.isEnabled = false
        attemptLogView.text = getString(R.string.push_unregister_qa_running)

        lifecycleScope.launch {
            try {
                val result =
                    withContext(Dispatchers.Default) {
                        PushUnregisterQaRunner.run(
                            config = config,
                            braze = Braze.getInstance(context.applicationContext),
                            onProgress = { attempts ->
                                withContext(Dispatchers.Main) {
                                    if (!isAdded) return@withContext
                                    val nextAttemptNumber = attempts.size + 1
                                    val logText =
                                        PushUnregisterQaRunResult.formatAttemptsLog(
                                            attempts = attempts,
                                            runningSuffix =
                                                getString(
                                                    R.string.push_unregister_qa_running_attempt,
                                                    nextAttemptNumber,
                                                ),
                                        )
                                    attemptLogView.text = logText
                                }
                            },
                        )
                    }
                if (!isAdded) return@launch
                val logText = result.formatLog()
                attemptLogView.text = logText
                context.writePrefsString(DroidboyPreferenceKeys.PUSH_UNREGISTER_QA_LAST_LOG, logText)
                refreshStatus()
                AlertDialog
                    .Builder(requireContext())
                    .setTitle(R.string.push_unregister_qa_log_header)
                    .setMessage(logText)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isAdded) return@launch
                val message = getString(R.string.push_unregister_qa_run_failed, error.message)
                attemptLogView.text = message
                showToast(message)
            } finally {
                qaRunInProgress = false
                if (isAdded) {
                    qaRunButton.isEnabled = true
                }
            }
        }
    }

    private fun scenarioValueToIndex(value: String): Int {
        val values = resources.getStringArray(R.array.push_unregister_qa_scenario_values)
        val index = values.indexOf(value)
        return if (index >= 0) index else 0
    }

    private fun targetApiValueToIndex(value: String): Int {
        val values = resources.getStringArray(R.array.push_unregister_qa_target_api_values)
        val index = values.indexOf(value)
        return if (index >= 0) index else 0
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val PUSH_TOKEN_REFRESH_DELAY_MS = 2_000L
        private const val SDK_TOGGLE_COOLDOWN_MS = 3_000L

        private fun Spinner.preventScrollableParentFromStealingTouches() {
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    var parent = view.parent
                    while (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true)
                        parent = parent.parent
                    }
                }
                false
            }
        }
    }
}
