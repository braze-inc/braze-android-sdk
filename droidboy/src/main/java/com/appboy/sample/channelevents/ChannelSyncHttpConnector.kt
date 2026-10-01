package com.appboy.sample.channelevents

import androidx.annotation.VisibleForTesting
import com.braze.BrazeInternal
import com.braze.communication.IHttpConnector
import com.braze.communication.IHttpConnector.HttpConnectorResult
import com.braze.requests.util.RequestTarget
import org.json.JSONObject

/**
 * Returns a canned HTTP result for Content Cards, Banners, and Feature Flags syncs.
 * Every other request is posted through [delegate].
 */
internal class ChannelSyncHttpConnector(
    private val response: ChannelSyncHttpResponse,
    private val delegate: IHttpConnector,
) : IHttpConnector {
    override fun post(
        requestTarget: RequestTarget,
        requestHeaders: Map<String, String?>,
        payload: JSONObject,
    ): HttpConnectorResult {
        if (response == ChannelSyncHttpResponse.REAL || !isChannelSync(requestTarget.urlString)) {
            return delegate.post(requestTarget, requestHeaders, payload)
        }
        return cannedResult()
    }

    private fun cannedResult(): HttpConnectorResult =
        when (response) {
            ChannelSyncHttpResponse.REAL -> error("A real response is posted, not canned.")
            ChannelSyncHttpResponse.HTTP_500 -> HttpConnectorResult(HTTP_500)
            ChannelSyncHttpResponse.NO_RESPONSE -> HttpConnectorResult(NO_RESPONSE_CODE)
            ChannelSyncHttpResponse.HTTP_400 -> HttpConnectorResult(HTTP_400)
            ChannelSyncHttpResponse.INVALID_JSON -> HttpConnectorResult(HTTP_SUCCESS_WITHOUT_BODY)
            ChannelSyncHttpResponse.HTTP_429 -> HttpConnectorResult(HTTP_429)
        }

    companion object {
        private const val HTTP_400 = 400
        private const val HTTP_429 = 429
        private const val HTTP_500 = 500
        private const val HTTP_SUCCESS_WITHOUT_BODY = 200

        /** Same code the default connector uses when the connection fails. */
        private const val NO_RESPONSE_CODE = -1

        private val SYNC_SUFFIXES = listOf("content_cards/sync", "banners/sync", "feature_flags/sync")

        /**
         * Connector last passed to [BrazeInternal.setHttpConnectorOverride].
         * Null when [ChannelSyncHttpResponse.REAL] restored the default connector.
         * Another owner of that slot can clear it without updating this reference.
         */
        @VisibleForTesting
        internal var installedConnector: IHttpConnector? = null
            private set

        /**
         * Installs [response] for later syncs.
         * [ChannelSyncHttpResponse.REAL] restores the default connector.
         * The override outlives [com.braze.Braze.wipeData] because it is not stored on a Braze instance.
         */
        fun install(response: ChannelSyncHttpResponse) {
            val connector =
                if (response == ChannelSyncHttpResponse.REAL) {
                    null
                } else {
                    ChannelSyncHttpConnector(response, BrazeInternal.getHttpConnector())
                }
            installedConnector = connector
            BrazeInternal.setHttpConnectorOverride(connector)
        }

        private fun isChannelSync(url: String): Boolean {
            val path = url.substringBefore('?')
            return SYNC_SUFFIXES.any { suffix -> path.endsWith(suffix) }
        }
    }
}
