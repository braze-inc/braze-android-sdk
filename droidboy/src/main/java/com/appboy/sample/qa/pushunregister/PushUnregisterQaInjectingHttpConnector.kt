package com.appboy.sample.qa.pushunregister

import com.braze.communication.IHttpConnector
import com.braze.communication.IHttpConnector.HttpConnectorResult
import com.braze.requests.util.RequestTarget
import org.json.JSONObject

internal class PushUnregisterQaInjectingHttpConnector(
    private val config: PushUnregisterQaInjectionConfig,
    private val delegate: IHttpConnector,
) : IHttpConnector {
    override fun post(
        requestTarget: RequestTarget,
        requestHeaders: Map<String, String?>,
        payload: JSONObject,
    ): HttpConnectorResult {
        if (!requestTarget.urlString.endsWith(PUSH_UNREGISTER_URL_SUFFIX)) {
            return delegate.post(requestTarget, requestHeaders, payload)
        }

        val attempt = config.attemptCounter.incrementAndGet()
        return if (attempt <= config.failuresBeforeSuccess) {
            HttpConnectorResult(
                config.failureResponseCode,
                emptyMap(),
                config.failureJsonResponse?.let { JSONObject(it) },
            )
        } else {
            delegate.post(requestTarget, requestHeaders, payload)
        }
    }

    companion object {
        private const val PUSH_UNREGISTER_URL_SUFFIX = "push/unregister"
    }
}
