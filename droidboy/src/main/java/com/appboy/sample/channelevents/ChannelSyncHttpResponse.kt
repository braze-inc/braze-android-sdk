package com.appboy.sample.channelevents

import androidx.annotation.StringRes
import com.appboy.sample.R

/**
 * HTTP result the Events tab substitutes for a Content Cards, Banners, or Feature Flags sync.
 * [REAL] leaves the request on the network.
 *
 * The production retry cap and backoff are unchanged. HTTP 500, HTTP 429, and no response
 * stay on the 15-step ladder. HTTP 400 and invalid JSON are terminal in the SDK.
 */
internal enum class ChannelSyncHttpResponse(
    @StringRes val label: Int,
) {
    REAL(R.string.channel_events_http_real),
    HTTP_500(R.string.channel_events_http_500),
    NO_RESPONSE(R.string.channel_events_http_no_response),
    HTTP_400(R.string.channel_events_http_400),
    INVALID_JSON(R.string.channel_events_http_invalid_json),
    HTTP_429(R.string.channel_events_http_429),
}
