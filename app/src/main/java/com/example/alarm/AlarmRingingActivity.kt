package com.example.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.alarm.data.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmRingingActivity : ComponentActivity() {
    private var alarmId by mutableLongStateOf(-1L)
    private var label by mutableStateOf("Alarm")
    private var snoozeMinutes by mutableIntStateOf(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        alarmId = intent.getLongExtra(AlarmActions.EXTRA_ALARM_ID, -1L)
        lifecycleScope.launch {
            val alarm = withContext(Dispatchers.IO) {
            AlarmRepository.get(applicationContext).find(alarmId)
            }
            if (alarm != null) {
                label = alarm.label.ifBlank { "Alarm" }
                snoozeMinutes = alarm.snoozeMinutes
            }
        }
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.headlineMedium)
                    Text("Alarm", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { sendAction(AlarmActions.SNOOZE) }) {
                        Text("Snooze $snoozeMinutes minutes")
                    }
                    TextButton(onClick = { sendAction(AlarmActions.DISMISS) }) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    private fun sendAction(action: String) {
        sendBroadcast(
            Intent(this, AlarmReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmActions.EXTRA_ALARM_ID, alarmId),
        )
        finish()
    }
}