package com.ritesh.cashiro.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionUtilsFormatBillingCycleTest {

    @Test
    fun `null cycle is labelled Monthly`() {
        assertEquals("Monthly", SubscriptionUtils.formatBillingCycle(null))
    }

    @Test
    fun `unknown cycle is labelled Monthly`() {
        assertEquals("Monthly", SubscriptionUtils.formatBillingCycle("fortnightly-ish"))
    }

    @Test
    fun `standard cycles use title case`() {
        assertEquals("Weekly", SubscriptionUtils.formatBillingCycle("weekly"))
        assertEquals("Monthly", SubscriptionUtils.formatBillingCycle("monthly"))
        assertEquals("Quarterly", SubscriptionUtils.formatBillingCycle("quarterly"))
        assertEquals("Semi-annual", SubscriptionUtils.formatBillingCycle("semi-annual"))
        assertEquals("Annual", SubscriptionUtils.formatBillingCycle("annual"))
    }

    @Test
    fun `cycle string is case-insensitive`() {
        assertEquals("Annual", SubscriptionUtils.formatBillingCycle("ANNUAL"))
        assertEquals("Quarterly", SubscriptionUtils.formatBillingCycle("Quarterly"))
    }

    @Test
    fun `custom cycle with count of one omits the number`() {
        assertEquals("Every day", SubscriptionUtils.formatBillingCycle("custom_1_day"))
        assertEquals("Every week", SubscriptionUtils.formatBillingCycle("custom_1_week"))
        assertEquals("Every month", SubscriptionUtils.formatBillingCycle("custom_1_month"))
        assertEquals("Every year", SubscriptionUtils.formatBillingCycle("custom_1_year"))
    }

    @Test
    fun `custom cycle with count greater than one pluralises the unit`() {
        assertEquals("Every 2 weeks", SubscriptionUtils.formatBillingCycle("custom_2_week"))
        assertEquals("Every 3 months", SubscriptionUtils.formatBillingCycle("custom_3_month"))
        assertEquals("Every 5 days", SubscriptionUtils.formatBillingCycle("custom_5_day"))
        assertEquals("Every 2 years", SubscriptionUtils.formatBillingCycle("custom_2_year"))
    }

    @Test
    fun `custom cycle ignores end date suffix`() {
        assertEquals("Every 2 months", SubscriptionUtils.formatBillingCycle("custom_2_month_2026-12-31"))
        assertEquals("Every month", SubscriptionUtils.formatBillingCycle("custom_1_month_forever"))
    }

    @Test
    fun `custom cycle with malformed parts falls back to monthly`() {
        assertEquals("Every month", SubscriptionUtils.formatBillingCycle("custom_"))
        assertEquals("Every month", SubscriptionUtils.formatBillingCycle("custom_abc_xyz"))
    }
}
