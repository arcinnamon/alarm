package com.example.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alarm.domain.AlarmSchedule
import com.example.alarm.domain.RepeatRule
import java.time.DayOfWeek
import java.time.LocalDate

enum class RepeatMode {
    ONCE,
    WEEKDAYS,
    EVERY_DAYS,
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val repeatMode: RepeatMode,
    val repeatDaysMask: Int,
    val intervalDays: Int,
    val anchorDateEpochDay: Long,
    val snoozeMinutes: Int,
    val snoozeAtEpochMillis: Long? = null,
)

fun AlarmEntity.toSchedule(): AlarmSchedule {
    val rule = when (repeatMode) {
        RepeatMode.ONCE -> RepeatRule.Once
        RepeatMode.WEEKDAYS -> RepeatRule.Weekdays(
            DayOfWeek.values().filterTo(mutableSetOf()) { day ->
                repeatDaysMask and (1 shl (day.value - 1)) != 0
            },
        )
        RepeatMode.EVERY_DAYS -> RepeatRule.EveryDays(intervalDays)
    }
    return AlarmSchedule(
        hour = hour,
        minute = minute,
        repeatRule = rule,
        anchorDate = LocalDate.ofEpochDay(anchorDateEpochDay),
    )
}