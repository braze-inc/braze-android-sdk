package com.appboy.sample.channelevents

import android.content.Context
import com.appboy.sample.util.ContentCardsTestingUtil
import com.braze.Braze
import com.braze.BrazeInternal
import com.braze.enums.CardKey
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Inserts a local Content Card so click and dismiss can be exercised without a server payload.
 * [bannerJson] is the Banner fixture payload. Storing that Banner is not part of this build.
 */
object LocalChannelFixtures {
    const val CARD_ID: String = "local-events-card"
    const val BANNER_PLACEMENT_ID: String = "local-events-banner"
    const val BANNER_BUTTON_ID: String = "local-test-button"
    const val CARD_URL: String = "https://www.braze.com"

    /**
     * Writes the fixtures into the current user's cache.
     */
    fun insert(context: Context) {
        val userId = Braze.getInstance(context).currentUser?.userId
        BrazeInternal.addSerializedContentCardToStorage(context, contentCardJson().toString(), userId)
    }

    /**
     * Captioned-image card whose click URL opens in the in-app WebView.
     */
    fun contentCardJson(): JSONObject {
        val card =
            ContentCardsTestingUtil.createCaptionedImageCardJson(
                id = CARD_ID,
                title = "Local events test card",
                description = "Tap opens an in-app WebView.",
                imageUrl = "https://picsum.photos/seed/local-events-card/800/400",
                altImageText = "Local events test card",
            )
        card.put(CardKey.CAPTIONED_IMAGE_URL.key, CARD_URL)
        card.put(CardKey.OPEN_URI_IN_WEBVIEW.key, true)
        card.put(CardKey.PINNED.key, false)
        card.put(CardKey.DISMISSIBLE.key, true)
        return card
    }

    /**
     * Banner whose HTML calls [com.braze.ui.banners.jsinterface.BannerJavascriptInterface.logButtonClick]
     * and [com.braze.ui.banners.jsinterface.BannerJavascriptInterface.beforeMessageClosed].
     */
    fun bannerJson(): JSONObject {
        val expiresAtSeconds = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)) / 1000L
        val html =
            """
            <div style="font-family:sans-serif;padding:12px;">
              <p>Local events banner</p>
              <button onclick="brazeInternalBridge.logButtonClick('$BANNER_BUTTON_ID')">logButtonClick</button>
              <button onclick="brazeInternalBridge.beforeMessageClosed()">Dismiss</button>
            </div>
            """.trimIndent()
        val banner =
            JSONObject()
                .put("id", "local-events-banner-tracking")
                .put("placement_id", BANNER_PLACEMENT_ID)
                .put("html", html)
                .put("is_control", false)
                .put("expires_at", expiresAtSeconds)
                .put("is_test_send", true)
                .put("properties", JSONObject())
        return JSONObject().put("banner", banner)
    }
}
