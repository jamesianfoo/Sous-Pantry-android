package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TrialReminderTest {
    private val zone = ZoneId.of("Australia/Sydney")
    private val purchase = ZonedDateTime.of(2026, 10, 9, 15, 30, 0, 0, zone)   // 3:30pm

    @Test fun `one day before fires 10am six days later`() {
        val expected = ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, zone)
        assertEquals(expected.toInstant().toEpochMilli() - purchase.toInstant().toEpochMilli(),
            TrialReminder.fireDelayMillis(purchase, 1))
    }

    @Test fun `two days before fires 10am five days later`() {
        val expected = ZonedDateTime.of(2026, 10, 14, 10, 0, 0, 0, zone)
        assertEquals(expected.toInstant().toEpochMilli() - purchase.toInstant().toEpochMilli(),
            TrialReminder.fireDelayMillis(purchase, 2))
    }

    @Test fun `never negative`() {
        assertEquals(0L, TrialReminder.fireDelayMillis(purchase.withHour(11), TrialReminder.TRIAL_DAYS))
    }
}
