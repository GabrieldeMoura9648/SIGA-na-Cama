package com.example.fatecskip.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.fatecskip.FatecSkipApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern

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
        val today = now.toLocalDate()
        val fixedAlarmToday = today.atTime(hour, minute)
        var nextAlarmDate = if (fixedAlarmToday.isAfter(now)) today else today.plusDays(1)

        if (isAuto) {
            val semester = prefs.selectedSemesterFlow.first()
            val subjects = repo.getSubjectsBySemesterList(semester)
            val offset = prefs.alarmAutoOffsetMinutesFlow.first()
            val startTimePattern = Pattern.compile("""(\d{1,2}):(\d{2})""")

            val firstClassMinutesByDay = (1..7).mapNotNull { day ->
                val earliestStart = subjects.asSequence()
                    .filter { it.isClassOnDay(day) }
                    .mapNotNull { subject ->
                        val match = startTimePattern.matcher(subject.classTime.substringBefore("-"))
                        if (!match.find()) return@mapNotNull null
                        val hourText = match.group(1) ?: return@mapNotNull null
                        val minuteText = match.group(2) ?: return@mapNotNull null
                        val classHour = hourText.toIntOrNull() ?: return@mapNotNull null
                        val classMinute = minuteText.toIntOrNull() ?: return@mapNotNull null
                        if (classHour !in 0..23 || classMinute !in 0..59) return@mapNotNull null
                        classHour * 60 + classMinute
                    }
                    .minOrNull()
                earliestStart?.let { day to it }
            }.toMap()

            var selectedAutoAlarm: LocalDateTime? = null
            try {
                for (dayOffset in 0..366) {
                    val candidateDate = today.plusDays(dayOffset.toLong())
                    val firstClassMinutes = firstClassMinutesByDay[candidateDate.dayOfWeek.value] ?: continue
                    val candidateAlarm = candidateDate.atStartOfDay()
                        .plusMinutes((firstClassMinutes - offset).toLong())
                    if (!candidateAlarm.isAfter(now)) continue

                    if (repo.isAcademicHoliday(candidateDate)) {
                        Log.i(TAG, "Alarme automático ignorado em $candidateDate: feriado.")
                        continue
                    }

                    selectedAutoAlarm = candidateAlarm
                    break
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível validar feriados; alarme automático desativado por segurança.", exception)
                cancelDailyAlarm(context)
                return true
            }

            if (selectedAutoAlarm != null) {
                nextAlarmDate = selectedAutoAlarm.toLocalDate()
                hour = selectedAutoAlarm.hour
                minute = selectedAutoAlarm.minute
            } else {
                Log.i(TAG, "Nenhum dia letivo futuro encontrado; alarme automático desativado.")
                cancelDailyAlarm(context)
                return true
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

    suspend fun scheduleNightPreview(context: Context): Boolean {
        val preferences = (context.applicationContext as FatecSkipApp).preferencesRepository
        val hour = preferences.nightPreviewHourFlow.first()
        val minute = preferences.nightPreviewMinuteFlow.first()
        val now = LocalDateTime.now()
        var nextPreview = now.toLocalDate().atTime(hour, minute)
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

    private fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, TYPE_DAILY, REQUEST_DAILY_ALARM)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
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
