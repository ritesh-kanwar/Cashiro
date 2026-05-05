package com.ritesh.cashiro.data.webhook

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class WebhookDeliveryService @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun deliver(
        url: String,
        headers: List<WebhookHeader>,
        payload: WebhookEnvelope
    ): WebhookAttemptResult {
        var lastError: WebhookAttemptResult? = null
        repeat(3) { attempt ->
            try {
                val response = client.post(url) {
                    header("Content-Type", "application/json")
                    headers.forEach { header(it.key.trim(), it.value) }
                    setBody(payload)
                }
                val body = response.bodyAsText()
                if (response.status.value !in 200..299) {
                    lastError = WebhookAttemptResult(
                        success = false,
                        httpStatus = response.status.value,
                        message = "HTTP ${response.status.value}",
                        retryable = response.status.value >= 500 || response.status.value == 429
                    )
                } else {
                    val ok = json.parseToJsonElement(body).jsonObject["ok"]?.jsonPrimitive?.booleanOrNull == true
                    if (ok) {
                        return WebhookAttemptResult(
                            success = true,
                            httpStatus = response.status.value,
                            message = "Delivered"
                        )
                    }
                    lastError = WebhookAttemptResult(
                        success = false,
                        httpStatus = response.status.value,
                        message = "Receiver did not acknowledge with ok=true",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                lastError = WebhookAttemptResult(
                    success = false,
                    message = e.message ?: "Network error",
                    retryable = true
                )
            }

            if (attempt < 2 && lastError?.retryable == true) {
                delay(1_000L shl attempt)
            }
        }
        return lastError ?: WebhookAttemptResult(success = false, message = "Unknown delivery error", retryable = true)
    }
}
