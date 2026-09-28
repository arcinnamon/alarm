package com.example.alarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class AlarmConverters {
    @TypeConverter
    fun repeatModeToString(value: RepeatMode): String = value.name

    @TypeConverter
    fun stringToRepeatMode(value: String): RepeatMode = RepeatMode.valueOf(value)
}

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
@TypeConverters(AlarmConverters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var instance: AlarmDatabase? = null

        fun get(context: Context): AlarmDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AlarmDatabase::class.java,
                "alarms.db",
            ).build().also { instance = it }
        }
    }
}