package io.github.anshireminder.app.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Bedtime postponement. When the configured time lands somewhere the user
 * cannot take the pill, they can push today's reminder to [BEDTIME_START].
 * The nag chain then has to stop at [BEDTIME_DEADLINE] so a postponed dose
 * never spills into the next day.
 */
object ReminderTiming {
    val BEDTIME_START: LocalTime = LocalTime.of(23, 30)
    val BEDTIME_DEADLINE: LocalTime = LocalTime.of(23, 55)

    fun bedtimeStart(date: LocalDate): LocalDateTime = date.atTime(BEDTIME_START)

    fun bedtimeDeadline(date: LocalDate): LocalDateTime = date.atTime(BEDTIME_DEADLINE)

    /**
     * [deadline] of `null` means the chain is unbounded, which is the case for
     * the regular daily reminder.
     */
    fun fitsBefore(nextAttempt: LocalDateTime, deadline: LocalDateTime?): Boolean =
        deadline == null || !nextAttempt.isAfter(deadline)
}
