package com.braze.ui.banners.jsinterface

import android.content.Context
import com.braze.ui.UserJavascriptInterfaceBase

/**
 * JavaScript bridge interface exposed to Banner HTML WebViews.
 * Delegates user-level operations (attribute setting, event logging, etc.)
 * to [UserJavascriptInterfaceBase].
 *
 * @param context The Android [Context] for Braze SDK access.
 */
@Suppress("TooManyFunctions")
class BannerUserJavascriptInterface(private val context: Context) : UserJavascriptInterfaceBase(context)
