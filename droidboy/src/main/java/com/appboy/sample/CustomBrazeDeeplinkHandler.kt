package com.appboy.sample

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.appboy.sample.activity.DroidBoyActivity
import com.braze.Constants
import com.braze.IBrazeDeeplinkHandler
import com.braze.IBrazeDeeplinkHandler.IntentFlagPurpose
import com.braze.enums.Channel
import com.braze.support.BrazeLogger.Priority.E
import com.braze.support.BrazeLogger.brazelog
import com.braze.ui.BrazeDeeplinkHandler
import com.braze.ui.actions.NewsfeedAction
import com.braze.ui.actions.UriAction

class CustomBrazeDeeplinkHandler : IBrazeDeeplinkHandler {
    override fun gotoNewsFeed(context: Context, newsfeedAction: NewsfeedAction) {
        val intent = Intent(context, DroidBoyActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        newsfeedAction.extras?.let { intent.putExtras(it) }
        intent.putExtra(context.resources.getString(R.string.source_key), Constants.BRAZE)
        intent.putExtra(
            context.resources.getString(R.string.destination_view),
            context.resources.getString(
                R.string.feed_key
            )
        )
        context.startActivity(intent)
    }

    override fun gotoUri(context: Context, uriAction: UriAction) {
        val uri = uriAction.uri.toString()
        if (uri.isNotBlank() && uri.matches(
                context.getString(R.string.youtube_regex).toRegex()
            )
        ) {
            uriAction.useWebView = false
        }

        val customUriAction = CustomUriAction(uriAction)
        customUriAction.execute(context)
    }

    override fun getIntentFlags(intentFlagPurpose: IntentFlagPurpose): Int =
        BrazeDeeplinkHandler().getIntentFlags(intentFlagPurpose)

    override fun createUriActionFromUrlString(
        url: String,
        extras: Bundle?,
        openInWebView: Boolean,
        channel: Channel
    ): UriAction? =
        BrazeDeeplinkHandler().createUriActionFromUrlString(url, extras, openInWebView, channel)

    override fun createUriActionFromUri(
        uri: Uri,
        extras: Bundle?,
        openInWebView: Boolean,
        channel: Channel
    ): UriAction =
        BrazeDeeplinkHandler().createUriActionFromUri(uri, extras, openInWebView, channel)

    class CustomUriAction(uriAction: UriAction) : UriAction(uriAction) {
        override fun openUriWithActionView(context: Context, uri: Uri, extras: Bundle?) {
            val intent = getActionViewIntent(context, uri, extras)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                brazelog(E) { "Failed to handle uri $uri with extras: $extras. Exception: $e" }
            }
        }
    }
}
