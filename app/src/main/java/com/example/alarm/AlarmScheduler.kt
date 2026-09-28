package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.alarm.data.AlarmEntity
import com.example.alarm.data.AlarmRepository
import com.example.alarm.data.RepeatMode
import com.example.alarm.data.toSchedule
import com.example.alarm.domain.AlarmScheduleCalculator
import com.example.alarm.domain.RepeatRule
import com.example.alarm.domain.RepeatRule
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlarmActions {
    const val FIRE = "com.example.alarm.FIRE"
    const val SNOOZE_FIRE = "com.example.alarm.SNOOZE_FIRE"
    const val SNOOZE = "com.example.alarm.SNOOZE"
    const val DISMISS = "com.example.alarm.DISMISS"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_IS_SNOOZE = "is_snooze"
}

object AlarmScheduler {
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return context.getSystemService(android.app.NotificationManager::class.java)
            .canUseFullScreenIntent()
    }

    fun schedule(context: Context, alarm: AlarmEntity): Boolean {
        cancelRegular(context, alarm.id)
        if (!alarm.enabled || !canScheduleExact(context)) return !alarm.enabled

        val schedule = alarm.toSchedule()
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val trigger = AlarmScheduleCalculator.nextOccurrence(schedule, now, zone)
            ?: if (schedule.repeatRule == RepeatRule.Once) {
                val anchor = schedule.anchorDate ?: return false
                val scheduledTime = anchor.atTime(schedule.hour, schedule.minute).atZone(zone).toInstant()
                if (scheduledTime.isAfter(now)) return false
                now.plusSeconds(1)
            } else {
                return false
            }
        val operation = broadcastIntent(context, AlarmActions.FIRE, alarm.id)
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode(),
            Intent(context, AlarmRingingActivity::class.java)
                .setAction("com.example.alarm.SHOW")
                .setData(Uri.parse("alarm-app://show/${alarm.id}"))
                .putExtra(AlarmActions.EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java).setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger.toEpochMilli(), showIntent),
            operation,
        )
        return true
    }

    fun scheduleSnooze(context: Context, alarm: AlarmEntity, trigger: Instant) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val operation = broadcastIntent(context, AlarmActions.SNOOZE_FIRE, alarm.id)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), operation)
        }
    }

    fun cancel(context: Context, id: Long) {
        cancelRegular(context, id)
        cancelSnooze(context, id)
    }

    fun cancelRegular(context: Context, id: Long) {
        cancelPendingIntent(context, AlarmActions.FIRE, id)
    }

    fun cancelSnooze(context: Context, id: Long) {
        cancelPendingIntent(context, AlarmActions.SNOOZE_FIRE, id)
    }

    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AlarmRepository.get(appContext)
            repository.all().forEach { alarm ->
                if (alarm.enabled) schedule(appContext, alarm) else cancelRegular(appContext, alarm.id)
                val snoozeAt = alarm.snoozeAtEpochMillis
                if (snoozeAt != null) scheduleSnooze(appContext, alarm, Instant.ofEpochMilli(snoozeAt))
            }
        }
    }

    private fun broadcastIntent(context: Context, action: String, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, AlarmReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("alarm-app://${action.substringAfterLast('.')}/$id"))
                .putExtra(AlarmActions.EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun cancelPendingIntent(context: Context, action: String, id: Long) {
        val intent = broadcastIntent(context, action, id)
        context.getSystemService(AlarmManager::class.java).cancel(intent)
        intent.cancel()
    }
}