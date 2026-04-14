package com.braze.unity.prime31compatible

import android.content.Intent
import android.os.Bundle
import com.braze.unity.BrazeUnityActivityWrapper
import com.prime31.UnityPlayerActivity

/**
 * Classes in the com.braze.unity.prime31compatible package provide support for Prime31 plugins. If you
 * are using any Prime31 plugins, you must use the classes in this package INSTEAD of the classes used
 * in the com.braze.unity package.
 *
 * This is a wrapper subclass of the [UnityPlayerActivity] class. It calls the necessary Braze methods
 * to ensure that analytics are collected and that push notifications are properly forwarded to
 * the Unity application.
 */
open class BrazeUnityPlayerActivity : UnityPlayerActivity() {
    private lateinit var brazeUnityActivityWrapper: BrazeUnityActivityWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brazeUnityActivityWrapper = BrazeUnityActivityWrapper()
        brazeUnityActivityWrapper.onCreateCalled(this)
    }

    override fun onStart() {
        super.onStart()
        brazeUnityActivityWrapper.onStartCalled(this)
    }

    override fun onResume() {
        super.onResume()
        brazeUnityActivityWrapper.onResumeCalled(this)
    }

    override fun onPause() {
        brazeUnityActivityWrapper.onPauseCalled(this)
        super.onPause()
    }

    override fun onStop() {
        brazeUnityActivityWrapper.onStopCalled(this)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        brazeUnityActivityWrapper.onNewIntentCalled(intent, this)
    }

    /** Forwards the given [UnityInAppMessageManagerAction][com.braze.unity.enums.UnityInAppMessageManagerAction] value to the wrapper. */
    fun onNewUnityInAppMessageManagerAction(actionEnumValue: Int) {
        brazeUnityActivityWrapper.onNewUnityInAppMessageManagerAction(actionEnumValue)
    }

    /** Launches the Braze Content Cards screen. */
    fun launchContentCardsActivity() {
        brazeUnityActivityWrapper.launchContentCardsActivity(this)
    }

    /** Registers the custom in-app message manager listener for Unity callbacks. */
    fun setInAppMessageListener() {
        brazeUnityActivityWrapper.setInAppMessageListener()
    }
}
