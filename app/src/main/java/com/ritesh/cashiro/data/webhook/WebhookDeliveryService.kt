package com.ritesh.cashiro.data.webhook

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@Singleton
class WebhookDeliveryService internal constructor(
    engine: HttpClientEngine,
    private val retryDelay: suspend (attempt: Int) -> Unit
) {
    @Inject constructor() : this(
        engine = Android.create(),
        retryDelay = { attempt -> delay(1_000L shl attempt) }
    )


    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
        // Match OkHttp's default behaviour (which the reference health-connect-webhook impl
        // relies on): follow 30x even on non-safe methods. Google Apps Script's documented
        // POST protocol is "doPost runs, returns 302 to googleusercontent.com echo URL with
        // the response body" — without this, every successful Apps Script delivery surfaces
        // as the bare 302 and gets logged as failure, which prevents the cursor from
        // advancing and causes the receiver-side data to duplicate on every subsequent sync.
        install(HttpRedirect) {
            checkHttpMethod = false
        }
    }

    suspend fun deliver(
        url: String,
        headers: List<WebhookHeader>,
        payload: WebhookEnvelope
    ): WebhookAttemptResult {
        // Trim header keys once up-front; the retry loop runs up to 3 times per delivery.
        val trimmedHeaders = headers.map { it.copy(key = it.key.trim()) }
        var lastError: WebhookAttemptResult? = null
        repeat(3) { attempt ->
            try {
                val response = client.post(url) {
                    header("Content-Type", "application/json")
                    trimmedHeaders.forEach { header(it.key, it.value) }
                    setBody(payload)
                }
                val status = response.status.value
                if (status in 200..299) {
                    return WebhookAttemptResult(
                        success = true,
                        httpStatus = status,
                        message = "Delivered (HTTP $status)"
                    )
                }
                lastError = WebhookAttemptResult(
                    success = false,
                    httpStatus = status,
                    message = "HTTP $status",
                    retryable = status >= 500 || status == 429
                )
            } catch (e: Exception) {
                lastError = WebhookAttemptResult(
                    success = false,
                    message = e.message ?: "Network error",
                    retryable = true
                )
            }

            if (attempt < 2 && lastError.retryable) {
                retryDelay(attempt)
            }
        }
        return lastError ?: WebhookAttemptResult(success = false, message = "Unknown delivery error", retryable = true)
    }
}
