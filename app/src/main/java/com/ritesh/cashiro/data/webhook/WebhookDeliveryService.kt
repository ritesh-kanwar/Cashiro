package com.ritesh.cashiro.data.webhook

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
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
        // Manual redirect handling below: 302/303 must downgrade POST→GET (Apps Script
        // relies on this). Ktor's HttpRedirect{checkHttpMethod=false} preserves POST and
        // breaks the /macros/s/.../exec → /macros/echo chain.
        install(HttpRedirect) {
            checkHttpMethod = true
        }
    }

    suspend fun deliver(
        url: String,
        headers: List<WebhookHeader>,
        payload: WebhookEnvelope
    ): WebhookAttemptResult {
        val trimmedHeaders = headers.map { it.copy(key = it.key.trim()) }
        var lastError: WebhookAttemptResult? = null
        repeat(3) { attempt ->
            try {
                val result = sendWithRedirects(url, trimmedHeaders, payload)
                if (result.success) return result
                lastError = result
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

    /**
     * Manually follows up to [MAX_REDIRECTS] hops, downgrading POST→GET on 302/303
     * (mirrors browser/OkHttp/curl) and stripping custom headers on cross-origin hops.
     */
    private suspend fun sendWithRedirects(
        initialUrl: String,
        headers: List<WebhookHeader>,
        payload: WebhookEnvelope
    ): WebhookAttemptResult {
        val originAuthority = authorityOf(initialUrl)
        var currentUrl = initialUrl
        var method = HttpMethod.Post
        var includeBody = true

        repeat(MAX_REDIRECTS + 1) {
            val response: HttpResponse = client.request(currentUrl) {
                this.method = method
                header(HttpHeaders.ContentType, "application/json")
                if (sameOrigin(currentUrl, originAuthority)) {
                    headers.forEach { header(it.key, it.value) }
                }
                if (includeBody) setBody(payload)
            }
            val status = response.status.value
            when {
                status in 200..299 -> return WebhookAttemptResult(
                    success = true,
                    httpStatus = status,
                    message = "Delivered (HTTP $status)"
                )
                status in 300..399 -> {
                    val location = response.headers[HttpHeaders.Location]
                        ?: return WebhookAttemptResult(
                            success = false,
                            httpStatus = status,
                            message = "HTTP $status without Location header",
                            retryable = false
                        )
                    when (status) {
                        HttpStatusCode.MovedPermanently.value,
                        HttpStatusCode.Found.value,
                        HttpStatusCode.SeeOther.value -> {
                            method = HttpMethod.Get
                            includeBody = false
                        }
                        HttpStatusCode.TemporaryRedirect.value,
                        HttpStatusCode.PermanentRedirect.value -> Unit // preserve method + body
                        else -> return WebhookAttemptResult(
                            success = false,
                            httpStatus = status,
                            message = "Unsupported redirect HTTP $status",
                            retryable = false
                        )
                    }
                    currentUrl = URLBuilder(currentUrl).takeFrom(location).buildString()
                }
                else -> return WebhookAttemptResult(
                    success = false,
                    httpStatus = status,
                    message = "HTTP $status",
                    retryable = status >= HttpStatusCode.InternalServerError.value ||
                        status == HttpStatusCode.TooManyRequests.value
                )
            }
        }
        return WebhookAttemptResult(
            success = false,
            message = "Too many redirects (>$MAX_REDIRECTS)",
            retryable = false
        )
    }

    private fun sameOrigin(currentUrl: String, originAuthority: String?): Boolean {
        if (originAuthority == null) return true
        val currentAuthority = authorityOf(currentUrl) ?: return false
        return currentAuthority.equals(originAuthority, ignoreCase = true)
    }

    private fun authorityOf(url: String): String? =
        runCatching { Url(url).let { "${it.host}:${it.port}" } }.getOrNull()

    private companion object {
        // Matches OkHttp's default redirect cap.
        const val MAX_REDIRECTS = 5
    }
}
