package com.clockinreminder.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.clockinreminder.app.data.local.entity.ScheduleDayEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    fun scheduleRemindersForSchedule(context: Context, scheduleDays: List<ScheduleDayEntity>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()

        for (day in scheduleDays) {
            if (!day.isWorkDay) continue

            val dayOfWeek = DayOfWeek.of(day.dayOfWeek)
            // Schedule for today or next occurrence
            var targetDate = now
            while (targetDate.dayOfWeek != dayOfWeek) {
                targetDate = targetDate.plusDays(1)
            }

            // Entry reminder
            day.startTime?.let { startTime ->
                scheduleAlarm(
                    context, alarmManager, targetDate, startTime.minusMinutes(5),
                    requestCode = 1000 + day.dayOfWeek,
                    title = "Início do expediente",
                    message = "Seu horário habitual de entrada é às $startTime. Lembre-se de registrar seu ponto!"
                )
            }

            // Lunch return reminder
            day.lunchEndTime?.let { lunchEnd ->
                scheduleAlarm(
                    context, alarmManager, targetDate, lunchEnd,
                    requestCode = 2000 + day.dayOfWeek,
                    title = "Fim do intervalo",
                    message = "Seu horário de retorno de almoço é às $lunchEnd. Bom retorno ao trabalho!"
                )
            }

            // Exit reminder
            day.endTime?.let { endTime ->
                scheduleAlarm(
                    context, alarmManager, targetDate, endTime,
                    requestCode = 3000 + day.dayOfWeek,
                    title = "Fim do expediente",
                    message = "Sua jornada prevista termina às $endTime. Não se esqueça de registrar sua saída!"
                )
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        date: LocalDate,
        time: LocalTime,
        requestCode: Int,
        title: String,
        message: String
    ) {
        val zone = ZoneId.systemDefault()
        val triggerEpochMs = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

        if (triggerEpochMs <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Alarm permission not granted on newer Android versions, degraded gracefully
        }
    }
}
