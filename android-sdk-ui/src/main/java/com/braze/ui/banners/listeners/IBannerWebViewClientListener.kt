package com.braze.ui.banners.listeners

import android.content.Context
import android.os.Bundle

interface IBannerWebViewClientListener {
    /**
     * Called when a close URL (appboy://close) is followed in an HTML Banner.
     *
     * @param context the current context
     * @param url          the url that triggered the close
     * @param queryBundle a bundle of the query part of url
     */
    fun onCloseAction(context: Context, url: String, queryBundle: Bundle)

    /**
     * Called when the window location is set to a Custom Event URL (appboy://customEvent) in an HTML Banner.
     *
     * @param context the current context
     * @param url          the url that triggered the action
     * @param queryBundle a bundle of the query part of url
     */
    fun onCustomEventAction(context: Context, url: String, queryBundle: Bundle)

    /**
     * Called when a non `appboy` scheme url is encountered.
     *
     * @param context the current context
     * @param url          the url pressed
     * @param queryBundle a bundle of the query part of url
     */
    fun onOtherUrlAction(context: Context, url: String, queryBundle: Bundle)
}
