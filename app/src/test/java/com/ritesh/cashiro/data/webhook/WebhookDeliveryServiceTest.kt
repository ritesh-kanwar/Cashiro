package com.ritesh.cashiro.data.webhook

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebhookDeliveryServiceTest {

    private val sampleEnvelope = WebhookEnvelope(
        generatedAt = "2026-01-01T00:00:00",
        app = WebhookAppInfo(name = "Cashiro", version = "test"),
        profile = WebhookProfileInfo(id = "profile-1", name = "Test"),
        request = WebhookRequestInfo(
            range = "today",
            start = "2026-01-01T00:00:00",
            end = "2026-01-01T23:59:59",
            currency = "INR",
            dataTypes = listOf("transactions")
        ),
        batch = WebhookBatchInfo(id = "batch-1", index = 1, count = 1)
    )

    private fun service(
        recordedRequests: MutableList<HttpRequestData> = mutableListOf(),
        responder: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): WebhookDeliveryService = WebhookDeliveryService(
        engine = MockEngine { request ->
            recordedRequests += request
            responder(request)
        },
        retryDelay = { /* skip backoff in tests */ }
    )

    private fun MockRequestHandleScope.jsonOk(body: String = "{}"): io.ktor.client.request.HttpResponseData =
        respond(content = body, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `200 OK is treated as success regardless of body`() = runTest {
        val service = service { jsonOk(body = "<html>not json</html>") }

        val result = service.deliver(
            url = "https://example.com/hook",
            headers = emptyList(),
            payload = sampleEnvelope
        )

        assertTrue("expected success on 2xx", result.success)
        assertEquals(200, result.httpStatus)
        assertFalse(result.retryable)
    }

    @Test
    fun `204 No Content is treated as success`() = runTest {
        val service = service {
            respond(content = ByteReadChannel.Empty, status = HttpStatusCode.NoContent)
        }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertTrue(result.success)
        assertEquals(204, result.httpStatus)
    }

    @Test
    fun `body without ok=true no longer fails the delivery`() = runTest {
        val service = service { jsonOk(body = """{"received":true}""") }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertTrue("HTTP-status-only contract: any 2xx counts as success", result.success)
    }

    @Test
    fun `explicit ok=false in response body is still success when status is 2xx`() = runTest {
        // Under the old contract this was a failure. Under the new HTTP-status contract it's success.
        val service = service { jsonOk(body = """{"ok":false,"reason":"ignored"}""") }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertTrue(result.success)
        assertEquals(200, result.httpStatus)
    }

    @Test
    fun `400 Bad Request fails and is not retryable`() = runTest {
        val attempts = mutableListOf<HttpRequestData>()
        val service = service(attempts) {
            respondError(HttpStatusCode.BadRequest)
        }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertFalse(result.success)
        assertEquals(400, result.httpStatus)
        assertFalse("4xx (non-429) is not retryable", result.retryable)
    }

    @Test
    fun `500 server error is retryable and final result reflects last attempt`() = runTest {
        val attempts = mutableListOf<HttpRequestData>()
        val service = service(attempts) { respondError(HttpStatusCode.InternalServerError) }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertEquals("retried 3 times before giving up", 3, attempts.size)
        assertFalse(result.success)
        assertEquals(500, result.httpStatus)
        assertTrue(result.retryable)
    }

    @Test
    fun `429 Too Many Requests is retryable`() = runTest {
        val attempts = mutableListOf<HttpRequestData>()
        val service = service(attempts) { respondError(HttpStatusCode.TooManyRequests) }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertEquals(3, attempts.size)
        assertTrue(result.retryable)
        assertEquals(429, result.httpStatus)
    }

    @Test
    fun `succeeds after transient 503 then 200`() = runTest {
        val attempts = mutableListOf<HttpRequestData>()
        var callCount = 0
        val service = service(attempts) {
            callCount += 1
            if (callCount == 1) respondError(HttpStatusCode.ServiceUnavailable) else jsonOk()
        }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertTrue(result.success)
        assertEquals(200, result.httpStatus)
        assertEquals("recovered on second attempt", 2, attempts.size)
    }

    @Test
    fun `network exception is reported as retryable failure with message`() = runTest {
        val service = service { throw java.net.ConnectException("connection refused") }

        val result = service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertFalse(result.success)
        assertNull("no http response was received", result.httpStatus)
        assertTrue(result.retryable)
        assertNotNull(result.message)
        assertTrue(
            "message should surface the underlying error",
            result.message.contains("connection refused", ignoreCase = true)
        )
    }

    @Test
    fun `custom headers are forwarded to the request`() = runTest {
        val attempts = mutableListOf<HttpRequestData>()
        val service = service(attempts) { jsonOk() }

        service.deliver(
            url = "https://example.com/hook",
            headers = listOf(
                WebhookHeader(key = "X-Auth-Token", value = "secret-123"),
                WebhookHeader(key = "  X-Trim  ", value = "padded")
            ),
            payload = sampleEnvelope
        )

        val sent = attempts.single().headers
        assertEquals("secret-123", sent["X-Auth-Token"])
        assertEquals("trims whitespace from header keys", "padded", sent["X-Trim"])
    }

    @Test
    fun `request body is JSON-serialized envelope`() = runTest {
        var capturedBody: String? = null
        val service = service {
            capturedBody = (it.body as io.ktor.http.content.OutgoingContent.ByteArrayContent)
                .bytes()
                .toString(Charsets.UTF_8)
            jsonOk()
        }

        service.deliver("https://example.com/hook", emptyList(), sampleEnvelope)

        assertNotNull(capturedBody)
        val body = capturedBody!!
        assertTrue("envelope contains profile id", body.contains("\"id\":\"profile-1\""))
        assertTrue("envelope contains app name", body.contains("\"name\":\"Cashiro\""))
        assertTrue("snake_case SerialName aliases are honored", body.contains("\"data_types\""))
    }
}
