# Alarm Architecture

## Alarm Storage

Room stores alarms in `alarms.db`. Each record contains a local time, label,
enabled state, repeat mode, repeat days or interval, interval anchor date, and
per-alarm snooze length. A pending snooze trigger is stored separately so it can
be restored after reboot. `AlarmRepository` exposes the observable alarm list
used by the Compose screen.

## Scheduling

`AlarmScheduleCalculator` is platform-independent and computes the next local
occurrence for one-time, selected-weekday, and every-N-days alarms. Day intervals
are anchored to the first scheduled date. A nonexistent local time during a
daylight-saving gap moves forward by the gap; an overlapping time fires once.

`AlarmScheduler` uses `AlarmManager.setAlarmClock` when exact-alarm access is
available. Recurring alarms are re-armed at each firing; one-time alarms are
disabled. Snooze is a separate trigger and does not move the recurrence.
Disabling or deleting an alarm cancels its regular and snooze triggers.

## Ringing

`AlarmReceiver` handles alarm, snooze, and notification actions. It starts
`AlarmVibrationService`, which owns the repeating vibration and an ongoing,
soundless alarm notification. The notification and lock-screen activity both
offer snooze and dismiss controls.

`AlarmLifecycleReceiver` rebuilds scheduled triggers after reboot, app updates,
time or timezone changes, and exact-alarm access being granted. Exact delivery,
notifications, and full-screen presentation depend on Android permissions and
device settings; the main screen reports missing access.
