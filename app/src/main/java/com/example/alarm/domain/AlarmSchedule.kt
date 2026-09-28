package com.example.alarm.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed interface RepeatRule {
    data object Once : RepeatRule
    data class Weekdays(val days: Set<DayOfWeek>) : RepeatRule
    data class EveryDays(val interval: Int) : RepeatRule
}

data class AlarmSchedule(
    val hour: Int,
    val minute: Int,
    val repeatRule: RepeatRule,
    val anchorDate: LocalDate? = null,
)

object AlarmScheduleCalculator {
    fun initialDate(hour: Int, minute: Int, now: Instant, zone: ZoneId): LocalDate {
        val current = now.atZone(zone)
        val alarmTime = LocalTime.of(hour, minute)
        return if (alarmTime.isAfter(current.toLocalTime())) {
            current.toLocalDate()
        } else {
            current.toLocalDate().plusDays(1)
        }
    }

    fun nextOccurrence(schedule: AlarmSchedule, after: Instant, zone: ZoneId): Instant? {
        val alarmTime = LocalTime.of(schedule.hour, schedule.minute)

        if (schedule.repeatRule == RepeatRule.Once) {
            val date = schedule.anchorDate ?: return null
            return date.atTime(alarmTime).atZone(zone).toInstant().takeIf { it.isAfter(after) }
        }

        val intervalAnchor = when (val rule = schedule.repeatRule) {
            is RepeatRule.EveryDays -> schedule.anchorDate.also {
                require(rule.interval in 1..365) { "Repeat interval must be between 1 and 365 days" }
            }
            else -> null
        }
        val weekdays = (schedule.repeatRule as? RepeatRule.Weekdays)?.days.orEmpty()
        if (schedule.repeatRule is RepeatRule.Weekdays && weekdays.isEmpty()) return null

        val firstDate = after.atZone(zone).toLocalDate()
        for (offset in 0..365) {
            val date = firstDate.plusDays(offset.toLong())
            val repeatsOnDate = when (val rule = schedule.repeatRule) {
                is RepeatRule.Weekdays -> date.dayOfWeek in weekdays
                is RepeatRule.EveryDays -> {
                    val anchor = intervalAnchor ?: return null
                    !date.isBefore(anchor) && ChronoUnit.DAYS.between(anchor, date) % rule.interval == 0L
                }
                RepeatRule.Once -> false
            }
            if (!repeatsOnDate) continue

            // atZone shifts nonexistent local times forward across a daylight-saving gap.
            val candidate = date.atTime(alarmTime).atZone(zone).toInstant()
            if (candidate.isAfter(after)) return candidate
        }
        return null
    }

    fun snoozeUntil(after: Instant, minutes: Int): Instant {
        require(minutes in 1..60) { "Snooze must be between 1 and 60 minutes" }
        return after.plusSeconds(minutes * 60L)
    }
}