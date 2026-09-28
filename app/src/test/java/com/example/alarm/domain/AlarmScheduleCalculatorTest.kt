package com.example.alarm.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmScheduleCalculatorTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun oneTimeAlarmFiresOnce() {
        val schedule = AlarmSchedule(
            hour = 7,
            minute = 0,
            repeatRule = RepeatRule.Once,
            anchorDate = LocalDate.of(2025, 1, 2),
        )

        assertEquals(
            Instant.parse("2025-01-02T07:00:00Z"),
            AlarmScheduleCalculator.nextOccurrence(schedule, Instant.parse("2025-01-02T06:00:00Z"), utc),
        )
        assertNull(
            AlarmScheduleCalculator.nextOccurrence(schedule, Instant.parse("2025-01-02T07:00:00Z"), utc),
        )
    }

    @Test
    fun weekdayRuleFindsNextSelectedDay() {
        val schedule = AlarmSchedule(
            hour = 7,
            minute = 0,
            repeatRule = RepeatRule.Weekdays(setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)),
        )

        assertEquals(
            Instant.parse("2025-01-04T07:00:00Z"),
            AlarmScheduleCalculator.nextOccurrence(schedule, Instant.parse("2025-01-03T08:00:00Z"), utc),
        )
    }

    @Test
    fun intervalUsesItsAnchorDate() {
        val schedule = AlarmSchedule(
            hour = 6,
            minute = 30,
            repeatRule = RepeatRule.EveryDays(3),
            anchorDate = LocalDate.of(2025, 1, 1),
        )

        assertEquals(
            Instant.parse("2025-01-04T06:30:00Z"),
            AlarmScheduleCalculator.nextOccurrence(schedule, Instant.parse("2025-01-03T22:00:00Z"), utc),
        )
    }

    @Test
    fun nonexistentTimeMovesForwardAcrossDaylightSavingGap() {
        val newYork = ZoneId.of("America/New_York")
        val schedule = AlarmSchedule(2, 30, RepeatRule.Weekdays(setOf(DayOfWeek.SUNDAY)))

        assertEquals(
            Instant.parse("2025-03-09T07:30:00Z"),
            AlarmScheduleCalculator.nextOccurrence(schedule, Instant.parse("2025-03-09T06:00:00Z"), newYork),
        )
    }

    @Test
    fun snoozeAddsTheConfiguredDuration() {
        assertEquals(
            Instant.parse("2025-01-02T07:05:00Z"),
            AlarmScheduleCalculator.snoozeUntil(Instant.parse("2025-01-02T07:00:00Z"), 5),
        )
    }
}