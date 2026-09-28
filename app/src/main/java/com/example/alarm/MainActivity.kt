package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.alarm.data.AlarmEntity
import com.example.alarm.data.AlarmRepository
import com.example.alarm.ui.AlarmScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val repository by lazy { AlarmRepository.get(applicationContext) }

    private var canScheduleExact by mutableStateOf(false)
    private var canUseFullScreenIntent by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshPermissionState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            val alarms by repository.alarms.collectAsStateWithLifecycle(emptyList())
            MaterialTheme {
                AlarmScreen(
                    alarms = alarms,
                    canScheduleExact = canScheduleExact,
                    canUseFullScreenIntent = canUseFullScreenIntent,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestExactAccess = ::requestExactAlarmAccess,
                    onRequestFullScreenAccess = ::requestFullScreenAccess,
                    onRequestNotificationPermission = ::requestNotificationPermission,
                    onSave = ::saveAlarm,
                    onToggle = ::toggleAlarm,
                    onDelete = ::deleteAlarm,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        AlarmScheduler.rescheduleAll(this)
    }

    private fun saveAlarm(alarm: AlarmEntity) {
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { repository.save(alarm) }
            if (!AlarmScheduler.schedule(this@MainActivity, saved)) refreshPermissionState()
        }
    }

    private fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        lifecycleScope.launch {
            val updated = withContext(Dispatchers.IO) {
                repository.save(
                    alarm.copy(
                        enabled = enabled,
                        snoozeAtEpochMillis = if (enabled) alarm.snoozeAtEpochMillis else null,
                    ),
                )
            }
            if (enabled) {
                if (!AlarmScheduler.schedule(this@MainActivity, updated)) refreshPermissionState()
            } else {
                AlarmScheduler.cancel(this@MainActivity, updated.id)
            }
        }
    }

    private fun deleteAlarm(alarm: AlarmEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { repository.delete(alarm.id) }
            AlarmScheduler.cancel(this@MainActivity, alarm.id)
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        startActivity(
            Intent(AlarmManager.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.parse("package:$packageName")),
        )
    }

    private fun requestFullScreenAccess() {
        if (Build.VERSION.SDK_INT < 34) return
        startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(Uri.parse("package:$packageName")),
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun refreshPermissionState() {
        canScheduleExact = AlarmScheduler.canScheduleExact(this)
        canUseFullScreenIntent = AlarmScheduler.canUseFullScreenIntent(this)
        hasNotificationPermission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}