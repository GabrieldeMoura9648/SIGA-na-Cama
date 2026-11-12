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
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

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
    private const val HOLIDAY_CHECK_TIMEOUT_MILLIS = 2_000L

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
            try {
                val semester = prefs.selectedSemesterFlow.first()
                val subjects = repo.getSubjectsBySemesterList(semester)
                val offset = prefs.alarmAutoOffsetMinutesFlow.first()

                val firstClassMinutesByDay = (1..7).mapNotNull { day ->
                    val earliestStart = subjects.asSequence()
                        .filter { it.isClassOnDay(day) }
                        .mapNotNull { subject ->
                            val (classHour, classMinute) = extractStartTimeForDay(
                                subject.classTime,
                                day,
                                subject.daysOfWeek
                            ) ?: return@mapNotNull null
                            classHour * 60 + classMinute
                        }
                        .minOrNull()
                    earliestStart?.let { day to it }
                }.toMap()

                var selectedAutoAlarm: LocalDateTime? = null
                for (dayOffset in 0..366) {
                    val candidateDate = today.plusDays(dayOffset.toLong())
                    val firstClassMinutes = firstClassMinutesByDay[candidateDate.dayOfWeek.value] ?: continue
                    val candidateAlarm = candidateDate.atStartOfDay()
                        .plusMinutes((firstClassMinutes - offset).toLong())
                    if (!candidateAlarm.isAfter(now)) continue

                    val isHoliday = try {
                        withTimeoutOrNull(HOLIDAY_CHECK_TIMEOUT_MILLIS) {
                            repo.isAcademicHoliday(candidateDate)
                        } ?: false
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Log.w(TAG, "Não foi possível validar $candidateDate como feriado; seguindo com o alarme.", exception)
                        false
                    }
                    if (isHoliday) {
                        Log.i(TAG, "Alarme automático ignorado em $candidateDate: feriado.")
                        continue
                    }

                    selectedAutoAlarm = candidateAlarm
                    break
                }

                prefs.setAlarmFallbackWarning(false)
                if (selectedAutoAlarm != null) {
                    nextAlarmDate = selectedAutoAlarm.toLocalDate()
                    hour = selectedAutoAlarm.hour
                    minute = selectedAutoAlarm.minute
                } else {
                    Log.i(TAG, "Nenhum dia letivo futuro encontrado; alarme automático desativado.")
                    cancelDailyAlarm(context)
                    return true
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível calcular o próximo dia letivo; usando o horário manual configurado.", exception)
                prefs.setAlarmFallbackWarning(true)
            }
        }
        
        var nextAlarm = nextAlarmDate.atTime(hour, minute)
        if (!nextAlarm.isAfter(now)) nextAlarm = nextAlarm.plusDays(1)
        val triggerAtMillis = nextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return schedule(context, TYPE_DAILY, REQUEST_DAILY_ALARM, triggerAtMillis)
    }

    fun extractStartTimeForDay(classTime: String, dayOfWeek: Int): Pair<Int, Int>? =
        extractStartTimeForDay(classTime, dayOfWeek, "")

    private fun extractStartTimeForDay(
        classTime: String,
        dayOfWeek: Int,
        daysOfWeek: String
    ): Pair<Int, Int>? {
        val dayNames = mapOf(
            1 to Regex("""\bseg(?:unda)?\b""", RegexOption.IGNORE_CASE),
            2 to Regex("""\bter(?:ça|ca)?\b""", RegexOption.IGNORE_CASE),
            3 to Regex("""\bqua(?:rta)?\b""", RegexOption.IGNORE_CASE),
            4 to Regex("""\bqui(?:nta)?\b""", RegexOption.IGNORE_CASE),
            5 to Regex("""\bsex(?:ta)?\b""", RegexOption.IGNORE_CASE),
            6 to Regex("""\bs(?:á|a)b(?:ado)?\b""", RegexOption.IGNORE_CASE),
            7 to Regex("""\bdom(?:ingo)?\b""", RegexOption.IGNORE_CASE)
        )
        val segments = classTime.split(Regex("""[|/;\n]+""")).map(String::trim).filter(String::isNotEmpty)
        val labeledSegment = segments.firstOrNull { segment ->
            dayNames[dayOfWeek]?.containsMatchIn(segment) == true
        }
        if (labeledSegment != null) {
            return parseStartTime(labeledSegment)
        }

        val ranges = Regex("""(\d{1,2}):(\d{2})""").findAll(classTime).toList()
        val rangeCount = ranges.size / 2
        if (rangeCount == 1) return parseStartTime(classTime)

        val scheduledDays = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        val dayIndex = scheduledDays.indexOf(dayOfWeek)
        if (rangeCount > 1 && dayIndex in 0 until rangeCount) {
            val startMatch = ranges[dayIndex * 2]
            val hour = startMatch.groupValues[1].toIntOrNull() ?: return null
            val minute = startMatch.groupValues[2].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour to minute
        }
        return null
    }

    private fun parseStartTime(value: String): Pair<Int, Int>? {
        val match = Regex("""(\d{1,2}):(\d{2})""").find(value) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
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
        if (!preferences.isOnboardingCompletedFlow.first()) return false

        val hour = preferences.nightPreviewHourFlow.first()
        val minute = preferences.nightPreviewMinuteFlow.first()
        val now = Calendar.getInstance()
        val nextPreview = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!nextPreview.after(now)) {
            nextPreview.add(Calendar.DAY_OF_YEAR, 1)
        }
        return schedule(
            context,
            TYPE_NIGHT_PREVIEW,
            REQUEST_NIGHT_PREVIEW,
            nextPreview.timeInMillis
        )
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
