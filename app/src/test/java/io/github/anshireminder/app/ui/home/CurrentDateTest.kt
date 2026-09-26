package io.github.anshireminder.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CurrentDateTest {

    @Test
    fun `waits until midnight on a normal day`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 9, 26),
            LocalTime.NOON,
            ZoneId.of("Europe/Paris"),
        )

        assertEquals(12 * 60 * 60 * 1000L, millisUntilNextDate(now))
    }

    @Test
    fun `keeps a positive delay immediately before midnight`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 9, 26),
            LocalTime.of(23, 59, 59, 999_000_000),
            ZoneId.of("Europe/Paris"),
        )

        assertEquals(1L, millisUntilNextDate(now))
    }

    @Test
    fun `uses the actual day length across daylight saving time`() {
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 3, 29),
            LocalTime.MIDNIGHT,
            ZoneId.of("Europe/Paris"),
        )

        assertEquals(23 * 60 * 60 * 1000L, millisUntilNextDate(now))
    }
}
