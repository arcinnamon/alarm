package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmVibrationService : Service() {
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmActions.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId < 0) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Alarm" }
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
        startForeground(notificationId(alarmId), buildNotification(alarmId, label, snoozeMinutes))
        startVibration()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        vibrator?.cancel()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startVibration() {
        val targetVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator = targetVibrator
        val pattern = longArrayOf(0, 700, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            targetVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            targetVibrator.vibrate(pattern, 0)
        }
    }

    private fun buildNotification(id: Long, label: String, snoozeMinutes: Int): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            notificationId(id),
            Intent(this, AlarmRingingActivity::class.java)
                .setAction("com.example.alarm.RINGING")
                .setData(Uri.parse("alarm-app://ringing/$id"))
                .putExtra(AlarmActions.EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = actionPendingIntent(id, AlarmActions.SNOOZE)
        val dismissIntent = actionPendingIntent(id, AlarmActions.DISMISS)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("Vibrating alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .addAction(0, "Snooze $snoozeMinutes min", snoozeIntent)
            .addAction(0, "Dismiss", dismissIntent)
            .build()
    }

    private fun actionPendingIntent(id: Long, action: String): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("alarm-app://${action.substringAfterLast('.')}/$id"))
            .putExtra(AlarmActions.EXTRA_ALARM_ID, id)
        return PendingIntent.getBroadcast(
            this,
            notificationId(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Vibration-only alarm alerts"
            setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        private const val CHANNEL_ID = "vibration_alarms"

        fun notificationId(id: Long): Int = ((id xor (id ushr 32)).toInt() and 0x7fffffff).coerceAtLeast(1)
    }
}