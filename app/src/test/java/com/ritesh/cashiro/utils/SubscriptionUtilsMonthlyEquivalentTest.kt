package com.ritesh.cashiro.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class SubscriptionUtilsMonthlyEquivalentTest {

    private fun bd(value: String) = BigDecimal(value)

    private fun assertEqualsMoney(expected: BigDecimal, actual: BigDecimal, message: String = "") {
        val e = expected.setScale(2, RoundingMode.HALF_UP)
        val a = actual.setScale(2, RoundingMode.HALF_UP)
        assertEquals(message, e, a)
    }

    @Test
    fun `monthly cycle returns amount unchanged`() {
        assertEqualsMoney(bd("5000"), SubscriptionUtils.monthlyEquivalent(bd("5000"), "monthly"))
    }

    @Test
    fun `null cycle defaults to monthly`() {
        assertEqualsMoney(bd("427"), SubscriptionUtils.monthlyEquivalent(bd("427"), null))
    }

    @Test
    fun `unknown cycle defaults to monthly`() {
        assertEqualsMoney(bd("427"), SubscriptionUtils.monthlyEquivalent(bd("427"), "fortnightly-ish"))
    }

    @Test
    fun `cycle string is case-insensitive`() {
        assertEqualsMoney(bd("5000"), SubscriptionUtils.monthlyEquivalent(bd("5000"), "MONTHLY"))
    }

    @Test
    fun `weekly cycle annualizes by 52 then divides by 12`() {
        assertEqualsMoney(bd("433.33"), SubscriptionUtils.monthlyEquivalent(bd("100"), "weekly"))
    }

    @Test
    fun `quarterly cycle divides amount by 3`() {
        assertEqualsMoney(bd("100"), SubscriptionUtils.monthlyEquivalent(bd("300"), "quarterly"))
    }

    @Test
    fun `semi-annual cycle divides amount by 6`() {
        assertEqualsMoney(bd("100"), SubscriptionUtils.monthlyEquivalent(bd("600"), "semi-annual"))
    }

    @Test
    fun `annual cycle divides amount by 12`() {
        assertEqualsMoney(bd("115.83"), SubscriptionUtils.monthlyEquivalent(bd("1390"), "annual"))
    }

    @Test
    fun `custom monthly cycle divides by count`() {
        assertEqualsMoney(bd("100"), SubscriptionUtils.monthlyEquivalent(bd("200"), "custom_2_month"))
    }

    @Test
    fun `custom monthly cycle ignores end date suffix`() {
        assertEqualsMoney(bd("100"), SubscriptionUtils.monthlyEquivalent(bd("200"), "custom_2_month_2026-12-31"))
    }

    @Test
    fun `custom day cycle uses average month length`() {
        assertEqualsMoney(bd("304.38"), SubscriptionUtils.monthlyEquivalent(bd("10"), "custom_1_day"))
    }

    @Test
    fun `custom week cycle annualizes then divides`() {
        assertEqualsMoney(bd("216.67"), SubscriptionUtils.monthlyEquivalent(bd("100"), "custom_2_week"))
    }

    @Test
    fun `custom year cycle divides by 12 times count`() {
        assertEqualsMoney(bd("100"), SubscriptionUtils.monthlyEquivalent(bd("2400"), "custom_2_year"))
    }

    @Test
    fun `malformed custom cycle defaults count to 1 and unit to month`() {
        assertEqualsMoney(bd("250"), SubscriptionUtils.monthlyEquivalent(bd("250"), "custom_"))
        assertEqualsMoney(bd("250"), SubscriptionUtils.monthlyEquivalent(bd("250"), "custom_abc_xyz"))
    }

    @Test
    fun `zero amount stays zero across cycles`() {
        assertEqualsMoney(BigDecimal.ZERO, SubscriptionUtils.monthlyEquivalent(BigDecimal.ZERO, "annual"))
        assertEqualsMoney(BigDecimal.ZERO, SubscriptionUtils.monthlyEquivalent(BigDecimal.ZERO, "custom_3_week"))
    }

    @Test
    fun `regression for issue 84 mixed monthly and yearly normalizes to expected total`() {
        val items = listOf(
            bd("5000")  to "monthly",
            bd("5000")  to "monthly",
            bd("427")   to "monthly",
            bd("1390")  to "annual",
            bd("299.5") to "annual",
        )

        val total = items.fold(BigDecimal.ZERO) { acc, (amount, cycle) ->
            acc.add(SubscriptionUtils.monthlyEquivalent(amount, cycle))
        }

        assertEqualsMoney(bd("10567.79"), total)
    }
}
