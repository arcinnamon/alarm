package com.example.alarm.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlarmRepository private constructor(private val dao: AlarmDao) {
    val alarms: Flow<List<AlarmEntity>> = dao.observeAll()

    suspend fun find(id: Long): AlarmEntity? = dao.findById(id)

    suspend fun all(): List<AlarmEntity> = dao.getAll()

    suspend fun save(alarm: AlarmEntity): AlarmEntity {
        if (alarm.id == 0L) return alarm.copy(id = dao.insert(alarm))
        dao.update(alarm)
        return alarm
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    companion object {
        @Volatile
        private var instance: AlarmRepository? = null

        fun get(context: Context): AlarmRepository = instance ?: synchronized(this) {
            instance ?: AlarmRepository(AlarmDatabase.get(context).alarmDao()).also { instance = it }
        }
    }
}