package com.braze.glideimageintegration

import android.app.Application
import com.braze.Braze
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.support.BrazeLogger

class GlideIntegrationApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        BrazeLogger.logLevel = BrazeLogger.VERBOSE
        registerActivityLifecycleCallbacks(BrazeActivityLifecycleCallbackListener())
        Braze.getInstance(this).imageLoader = GlideBrazeImageLoader()
    }
}
