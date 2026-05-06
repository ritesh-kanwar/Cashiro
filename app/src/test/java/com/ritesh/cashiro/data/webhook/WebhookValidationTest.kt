package com.ritesh.cashiro.data.webhook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebhookValidationTest {

    @Test
    fun `validateName accepts any non-blank string`() {
        assertNull(WebhookValidation.validateName("My webhook"))
        assertNull(WebhookValidation.validateName("a"))
        assertNull(WebhookValidation.validateName(" leading-space-ok"))
    }

    @Test
    fun `validateName rejects empty and whitespace-only input`() {
        assertEquals("Webhook name is required", WebhookValidation.validateName(""))
        assertEquals("Webhook name is required", WebhookValidation.validateName("   "))
        assertEquals("Webhook name is required", WebhookValidation.validateName("\t\n"))
    }

    @Test
    fun `validateUrl accepts http and https URLs`() {
        assertNull(WebhookValidation.validateUrl("http://example.com"))
        assertNull(WebhookValidation.validateUrl("https://example.com/path?q=1"))
        assertNull(WebhookValidation.validateUrl("https://localhost:3000/hooks"))
    }

    @Test
    fun `validateUrl rejects empty input`() {
        assertEquals("Webhook URL is required", WebhookValidation.validateUrl(""))
        assertEquals("Webhook URL is required", WebhookValidation.validateUrl("   "))
    }

    @Test
    fun `validateUrl rejects URLs without http or https scheme`() {
        val expected = "Must start with http:// or https://"
        assertEquals(expected, WebhookValidation.validateUrl("example.com"))
        assertEquals(expected, WebhookValidation.validateUrl("ftp://example.com"))
        assertEquals(expected, WebhookValidation.validateUrl("ws://example.com"))
        assertEquals(expected, WebhookValidation.validateUrl("//example.com"))
    }

    @Test
    fun `validateIntervalHours accepts every value in the supported range`() {
        for (hours in WebhookValidation.MIN_INTERVAL_HOURS..WebhookValidation.MAX_INTERVAL_HOURS) {
            assertNull("$hours hours should be valid", WebhookValidation.validateIntervalHours(hours.toString()))
        }
    }

    @Test
    fun `validateIntervalHours rejects empty input`() {
        val msg = WebhookValidation.validateIntervalHours("")
        assertEquals("Enter a number between 1 and 24", msg)
    }

    @Test
    fun `validateIntervalHours rejects non-numeric input`() {
        val msg = WebhookValidation.validateIntervalHours("abc")
        assertEquals("Enter a number between 1 and 24", msg)
    }

    @Test
    fun `validateIntervalHours rejects values outside 1 to 24`() {
        val expected = "Must be between 1 and 24 hours"
        assertEquals(expected, WebhookValidation.validateIntervalHours("0"))
        assertEquals(expected, WebhookValidation.validateIntervalHours("25"))
        assertEquals(expected, WebhookValidation.validateIntervalHours("99"))
    }
}
