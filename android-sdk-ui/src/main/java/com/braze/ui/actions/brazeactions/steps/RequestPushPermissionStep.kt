package com.braze.ui.actions.brazeactions.steps

import android.content.Context
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.support.requestPushPermissionPrompt

internal object RequestPushPermissionStep : BaseBrazeActionStep() {
    /**
     * This step does not require any arguments.
     */
    override fun isValid(data: StepData): Boolean = true

    override fun run(context: Context, data: StepData) {
        BrazeActivityLifecycleCallbackListener.activity.requestPushPermissionPrompt()
    }
}
