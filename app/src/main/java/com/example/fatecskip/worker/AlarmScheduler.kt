package com.example.fatecskip.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

import kotlinx.coroutines.flow.first
import com.example.fatecskip.FatecSkipApp
import java.time.format.DateTimeFormatter

object AlarmScheduler {

    const val EXTRA_ALARM_TYPE = "com.example.fatecskip.extra.ALARM_TYPE"
    const val TYPE_DAILY = "daily"
    const val TYPE_SNOOZE = "snooze"
    const val TYPE_TEST = "test"
    const val TYPE_NIGHT_PREVIEW = "night_preview"
    const val TYPE_TEST_NIGHT_PREVIEW = "test_night_preview"

    private const val REQUEST_DAILY_ALARM = 5101
    private const val REQUEST_SNOOZE = 5102
    private const val REQUEST_TEST = 5103
    private const val REQUEST_NIGHT_PREVIEW = 5104
    private const val REQUEST_TEST_NIGHT_PREVIEW = 5105
    private const val TAG = "AlarmScheduler"

    suspend fun scheduleDailyAlarm(context: Context): Boolean {
        val app = context.applicationContext as FatecSkipApp
        val prefs = app.preferencesRepository
        val repo = app.repository
        
        val isAuto = prefs.alarmModeFlow.first() == "auto"
        var hour = prefs.alarmHourFlow.first()
        var minute = prefs.alarmMinuteFlow.first()
        
        val now = LocalDateTime.now()
        var nextAlarmDate = now.toLocalDate()
        if (now.hour > hour || (now.hour == hour && now.minute >= minute)) {
            nextAlarmDate = nextAlarmDate.plusDays(1)
        }
        
        if (isAuto) {
            val semester = prefs.selectedSemesterFlow.first()
            val tomorrowSubjects = repo.getSubjectsBySemesterList(semester).filter { sub ->
                sub.daysOfWeek.split(",").mapNotNull { d -> d.trim().toIntOrNull() }.contains(nextAlarmDate.dayOfWeek.value) 
                || sub.dayOfWeek == nextAlarmDate.dayOfWeek.value
            }
            if (tomorrowSubjects.isNotEmpty()) {
                val earliestTime = tomorrowSubjects.map { it.classTime }.minOrNull() ?: "07:40 - 11:20"
                val timePart = earliestTime.split("-")[0].trim().replace(Regex("[^0-9:]"), "")
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    val classHour = parts[0].toIntOrNull() ?: 7
                    val classMinute = parts[1].toIntOrNull() ?: 40
                    
                    val offset = prefs.alarmAutoOffsetMinutesFlow.first()
                    var totalMinutes = classHour * 60 + classMinute - offset
                    if (totalMinutes < 0) totalMinutes += 24 * 60
                    
                    hour = totalMinutes / 60
                    minute = totalMinutes % 60
                }
            } else {
                Log.d(TAG, "Nenhuma aula amanhã, mas agendando alarme padrão mesmo assim (pode ser cancelado via UI se desejado).")
            }
        }
        
        var nextAlarm = nextAlarmDate.atTime(hour, minute)
        if (!nextAlarm.isAfter(now)) nextAlarm = nextAlarm.plusDays(1)
        val triggerAtMillis = nextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return schedule(context, TYPE_DAILY, REQUEST_DAILY_ALARM, triggerAtMillis)
    }



    fun scheduleSnooze(context: Context, minutes: Int = 10): Boolean {
        require(minutes > 0) { "A duração da soneca deve ser maior que zero." }
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        return schedule(context, TYPE_SNOOZE, REQUEST_SNOOZE, triggerAtMillis)
    }

    fun scheduleTestAlarmInSeconds(context: Context, seconds: Int = 5): Boolean {
        require(seconds > 0) { "O atraso do teste deve ser maior que zero." }
        val triggerAtMillis = System.currentTimeMillis() + seconds * 1_000L
        return schedule(context, TYPE_TEST, REQUEST_TEST, triggerAtMillis)
    }

    fun scheduleNightPreview(context: Context): Boolean {
        val now = LocalDateTime.now()
        var nextPreview = now.toLocalDate().atTime(21, 0)
        if (!nextPreview.isAfter(now)) nextPreview = nextPreview.plusDays(1)
        val triggerAtMillis = nextPreview.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return schedule(context, TYPE_NIGHT_PREVIEW, REQUEST_NIGHT_PREVIEW, triggerAtMillis)
    }

    fun scheduleTestNightPreviewInSeconds(context: Context, seconds: Int = 5): Boolean {
        require(seconds > 0) { "O atraso do teste deve ser maior que zero." }
        val triggerAtMillis = System.currentTimeMillis() + seconds * 1_000L
        return schedule(
            context,
            TYPE_TEST_NIGHT_PREVIEW,
            REQUEST_TEST_NIGHT_PREVIEW,
            triggerAtMillis
        )
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            TYPE_DAILY to REQUEST_DAILY_ALARM,
            TYPE_SNOOZE to REQUEST_SNOOZE,
            TYPE_TEST to REQUEST_TEST
        ).forEach { (type, requestCode) ->
            val pendingIntent = createPendingIntent(context, type, requestCode)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelSnooze(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, TYPE_SNOOZE, REQUEST_SNOOZE)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelNightPreview(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            TYPE_NIGHT_PREVIEW to REQUEST_NIGHT_PREVIEW,
            TYPE_TEST_NIGHT_PREVIEW to REQUEST_TEST_NIGHT_PREVIEW
        ).forEach { (type, requestCode) ->
            val pendingIntent = createPendingIntent(context, type, requestCode)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun schedule(
        context: Context,
        type: String,
        requestCode: Int,
        triggerAtMillis: Long
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, type, requestCode)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return true
            } catch (exception: SecurityException) {
                Log.w(TAG, "Permissão para alarme exato indisponível; usando alarme inexato.", exception)
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.w(TAG, "Alarme '$type' agendado sem precisão exata.")
        return false
    }

    private fun createPendingIntent(
        context: Context,
        type: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction("com.example.fatecskip.action.ALARM.$type")
            .putExtra(EXTRA_ALARM_TYPE, type)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
