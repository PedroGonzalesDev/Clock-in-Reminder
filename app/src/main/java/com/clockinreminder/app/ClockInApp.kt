package com.clockinreminder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.clockinreminder.app.data.local.AppDatabase
import com.clockinreminder.app.data.repository.ClockInRepository
import com.clockinreminder.app.data.repository.ClockInRepositoryImpl

class ClockInApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ClockInRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = ClockInRepositoryImpl(database)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "clockin_reminders",
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_reminders_desc)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
