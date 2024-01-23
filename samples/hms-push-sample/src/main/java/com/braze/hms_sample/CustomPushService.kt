package com.braze.hms_sample

import android.util.Log
import com.braze.Braze
import com.braze.push.BrazeHuaweiPushHandler
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class CustomPushService : HmsMessageService() {

    companion object {
        val TAG: String = toString()
    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        val pushToken = HmsInstanceId.getInstance(applicationContext).getToken("102077259", "HCM")
        Log.i(TAG, "Got Huawei push token $pushToken")
        Braze.getInstance(applicationContext).registeredPushToken = token
    }

    override fun onMessageReceived(hmsRemoteMessage: RemoteMessage?) {
        super.onMessageReceived(hmsRemoteMessage)

        if (BrazeHuaweiPushHandler.handleHmsRemoteMessageData(applicationContext, hmsRemoteMessage?.dataOfMap)) {
            Log.i(TAG, "Braze has handled Huawei push notification.")
        }
    }
}
