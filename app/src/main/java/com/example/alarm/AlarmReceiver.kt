package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.alarm.data.AlarmRepository
import com.example.alarm.data.RepeatMode
import com.example.alarm.domain.AlarmScheduleCalculator
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmActions.EXTRA_ALARM_ID, -1L)
        if (id < 0) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AlarmRepository.get(appContext)
                val alarm = repository.find(id) ?: return@launch
                when (intent.action) {
                    AlarmActions.SNOOZE -> {
                        val trigger = AlarmScheduleCalculator.snoozeUntil(Instant.now(), alarm.snoozeMinutes)
                        val updated = repository.save(alarm.copy(snoozeAtEpochMillis = trigger.toEpochMilli()))
                        AlarmScheduler.scheduleSnooze(appContext, updated, trigger)
                        stopRinging(appContext, id)
                    }
                    AlarmActions.DISMISS -> {
                        if (alarm.snoozeAtEpochMillis != null) {
                            repository.save(alarm.copy(snoozeAtEpochMillis = null))
                            AlarmScheduler.cancelSnooze(appContext, id)
                        }
                        stopRinging(appContext, id)
                    }
                    AlarmActions.SNOOZE_FIRE -> {
                        if (alarm.snoozeAtEpochMillis == null) return@launch
                        repository.save(alarm.copy(snoozeAtEpochMillis = null))
                        ring(appContext, id, alarm.label, alarm.snoozeMinutes, isSnooze = true)
                    }
                    AlarmActions.FIRE -> {
                        if (!alarm.enabled) return@launch
                        val recurring = alarm.repeatMode != RepeatMode.ONCE
                        repository.save(alarm.copy(enabled = recurring))
                        if (recurring) {
                            AlarmScheduler.schedule(appContext, alarm)
                        } else {
                            AlarmScheduler.cancelRegular(appContext, id)
                        }
                        ring(appContext, id, alarm.label, alarm.snoozeMinutes, isSnooze = false)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ring(context: Context, id: Long, label: String, snoozeMinutes: Int, isSnooze: Boolean) {
        val serviceIntent = Intent(context, AlarmVibrationService::class.java)
            .putExtra(AlarmActions.EXTRA_ALARM_ID, id)
            .putExtra(AlarmActions.EXTRA_IS_SNOOZE, isSnooze)
            .putExtra(AlarmVibrationService.EXTRA_LABEL, label)
            .putExtra(AlarmVibrationService.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun stopRinging(context: Context, id: Long) {
        context.stopService(Intent(context, AlarmVibrationService::class.java))
        context.getSystemService(android.app.NotificationManager::class.java).cancel(id.hashCode())
    }
}

class AlarmLifecycleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED &&
            !AlarmScheduler.canScheduleExact(context)
        ) return
        AlarmScheduler.rescheduleAll(context)
    }
}