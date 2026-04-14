package com.braze.glideimageintegration

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import com.braze.enums.BrazeViewBounds
import com.braze.images.IBrazeImageLoader
import com.braze.models.cards.Card
import com.braze.models.inappmessage.IInAppMessage
import com.braze.support.BrazeLogger
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class GlideBrazeImageLoader : IBrazeImageLoader {

    private var requestOptions = RequestOptions()

    override fun renderUrlIntoCardView(
        context: Context,
        card: Card,
        imageUrl: String,
        imageView: ImageView,
        viewBounds: BrazeViewBounds?
    ) {
        renderUrlIntoView(context, imageUrl, imageView)
    }

    override fun renderUrlIntoInAppMessageView(
        context: Context,
        inAppMessage: IInAppMessage,
        imageUrl: String,
        imageView: ImageView,
        viewBounds: BrazeViewBounds?
    ) {
        renderUrlIntoView(context, imageUrl, imageView)
    }

    override fun getPushBitmapFromUrl(
        context: Context,
        extras: Bundle?,
        imageUrl: String,
        viewBounds: BrazeViewBounds?
    ): Bitmap? {
        return getBitmapFromUrl(context, imageUrl, viewBounds)
    }

    override fun getInAppMessageBitmapFromUrl(
        context: Context,
        inAppMessage: IInAppMessage,
        imageUrl: String,
        viewBounds: BrazeViewBounds?
    ): Bitmap? {
        return getBitmapFromUrl(context, imageUrl, viewBounds)
    }

    private fun renderUrlIntoView(
        context: Context,
        imageUrl: String,
        imageView: ImageView
    ) {
        imageView.post {
            try {
                Glide.with(context)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(imageView)
            } catch (e: Exception) {
                BrazeLogger.e(TAG, "Failed URL into view: $imageUrl", e)
            }
        }
    }

    private fun getBitmapFromUrl(
        context: Context,
        imageUrl: String,
        viewBounds: BrazeViewBounds?
    ): Bitmap? {
        return try {
            Glide.with(context)
                .asBitmap()
                .apply(requestOptions)
                .load(imageUrl)
                .submit()
                .get()
        } catch (e: Exception) {
            BrazeLogger.e(TAG, "Failed to retrieve bitmap at url: $imageUrl", e)
            null
        }
    }

    override fun setOffline(isOffline: Boolean) {
        requestOptions = requestOptions.onlyRetrieveFromCache(isOffline)
    }

    companion object {
        private val TAG = GlideBrazeImageLoader::class.java.name
    }
}
