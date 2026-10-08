package com.ritesh.cashiro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubscriptionPaymentCycleTest {
    @Test fun endOfMonthUsesCalendarCyclesAndLeapYears() {
        assertEquals(LocalDate.of(2028, 2, 29), advancePaymentCycle(LocalDate.of(2028, 1, 31), "Monthly"))
        assertEquals(LocalDate.of(2027, 2, 28), advancePaymentCycle(LocalDate.of(2027, 1, 31), "Monthly"))
    }

    @Test fun customDailyRepeatGuardDoesNotSkipPastTheNextEligibleDay() {
        val paid = LocalDate.of(2020, 1, 1)
        assertEquals(paid.plusDays(2), advancePaymentCycle(paid, "custom_2_day_forever"))
        assertEquals(paid.plusWeeks(3), advancePaymentCycle(paid, "custom_3_week_2021-01-01"))
    }

    @Test fun malformedCustomIntervalUsesMonthlyFallback() {
        val paid = LocalDate.of(2026, 1, 1)
        assertEquals(paid.plusMonths(1), advancePaymentCycle(paid, "custom_0_day_forever"))
        assertEquals(paid.plusMonths(1), advancePaymentCycle(paid, "custom_2_unknown_forever"))
    }
}
