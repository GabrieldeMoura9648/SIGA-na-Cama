package com.example.fatecskip.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.fatecskip.data.local.AppDatabase
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.FatecSkipApp
import com.example.fatecskip.domain.model.SkipVerdict
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferences = UserPreferencesRepository(context)
            val isOnboardingCompleted = preferences.isOnboardingCompletedFlow.first()
            val notificationsEnabled = preferences.notificationsEnabledFlow.first()
            if (!isOnboardingCompleted || !notificationsEnabled) {
                return Result.success()
            }

            val database = AppDatabase.getDatabase(context)
            val semester = preferences.selectedSemesterFlow.first()
            val nickname = preferences.studentNicknameFlow.first()

            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowStr = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val tomorrowDayOfWeek = tomorrow.dayOfWeek.value

            if (inputData.getBoolean(IS_NIGHT_PREVIEW, false)) {
                val verdict = FatecSkipApp.instance.repository.evaluateSkipVerdict(
                    date = tomorrow,
                    semester = semester
                )
                val verdictSummary = when (verdict) {
                    is SkipVerdict.ForbiddenCriticalEvent ->
                        "🚨 Dia de evento crítico: ${verdict.eventTitle}. Nada de faltar!"
                    is SkipVerdict.ForbiddenAbsenceLimit ->
                        "GAME OVER - Reprovação por frequência em ${verdict.subjectName}; sem direito ao Exame Final."
                    is SkipVerdict.WarningSafetyMargin ->
                        "⚠️ Atenção às faltas em ${verdict.subjectName}: você está na margem de risco."
                    is SkipVerdict.Allowed ->
                        "✅ Aulas sem eventos críticos e faltas dentro da margem de segurança."
                    is SkipVerdict.NoClassOrHoliday ->
                        "🛌 ${verdict.customMessage}"
                }
                val tomorrowSubjects = database.subjectDao()
                    .getSubjectsBySemesterList(semester)
                    .filter { it.isClassOnDay(tomorrowDayOfWeek) }
                val classesSummary = if (tomorrowSubjects.isEmpty()) {
                    "Sem aulas cadastradas."
                } else {
                    "Aulas: " + tomorrowSubjects.joinToString { "${it.name} (${it.classTime})" }
                }
                val alarmSummary = if (preferences.alarmEnabledFlow.first()) {
                    when (preferences.alarmModeFlow.first()) {
                        "auto" -> "Despertador automático ativo."
                        else -> "Despertador ativo às %02d:%02d.".format(
                            preferences.alarmHourFlow.first(),
                            preferences.alarmMinuteFlow.first()
                        )
                    }
                } else {
                    "Despertador desativado."
                }
                NotificationHelper.showNotification(
                    context = context,
                    title = "Agenda de Amanhã — SIGA na Cama",
                    message = "$classesSummary\n$verdictSummary\n$alarmSummary",
                    notificationId = 1010
                )
                return Result.success()
            }

            // 1. Checa eventos críticos amanhã
            val criticalEvents = database.eventDao().getCriticalEventsByDate(tomorrowStr)
            if (criticalEvents.isNotEmpty()) {
                val event = criticalEvents.first()
                NotificationHelper.showNotification(
                    context = context,
                    title = "🚨 Alerta de Sobrevivência!",
                    message = "Atenção $nickname! Amanhã tem '${event.title}'. Nem inventa de meter o louco, essa vale nota!",
                    notificationId = 1001
                )
                return Result.success()
            }

            // 2. Checa feriado amanhã
            val holidays = database.eventDao().getHolidaysByDate(tomorrowStr)
            if (holidays.isNotEmpty()) {
                val holiday = holidays.first()
                NotificationHelper.showNotification(
                    context = context,
                    title = "🍺 Glória a Deux!",
                    message = "Amanhã é feriado (${holiday.title}) na Fatec. O despertador tá oficialmente demitido!",
                    notificationId = 1002
                )
                return Result.success()
            }

            // 3. Checa matérias de amanhã com risco de limite
            val allSemesterSubjects = database.subjectDao().getSubjectsBySemesterList(semester)
            val tomorrowSubjects = allSemesterSubjects.filter { it.isClassOnDay(tomorrowDayOfWeek) }
            val dangerSubject = tomorrowSubjects.firstOrNull { subject ->
                val threshold = (subject.maxAbsences * 0.75f).toInt()
                (subject.currentAbsences + subject.classesPerDay) >= threshold
            }

            if (dangerSubject != null) {
                val remaining = (dangerSubject.maxAbsences - dangerSubject.currentAbsences).coerceAtLeast(0)
                NotificationHelper.showNotification(
                    context = context,
                    title = "💀 Ouvindo a música do Fantástico?",
                    message = "Ei $nickname! Você só tem mais $remaining falta(s) em '${dangerSubject.name}'. Se faltar amanhã, o SIGA vai te pegar!",
                    notificationId = 1003
                )
                return Result.success()
            }

            // 4. Teste ou aviso diário de tranquilidade
            NotificationHelper.showNotification(
                context = context,
                title = "📚 Resumo acadêmico:",
                message = "Salve $nickname! Amanhã o dia está favorável. Suas faltas estão dentro da margem de segurança. Durma em paz!",
                notificationId = 1004
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyCheckWorker", "Falha ao gerar a notificação acadêmica diária.", e)
            Result.failure()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "fatec_daily_check_work"
        const val IS_NIGHT_PREVIEW = "IS_NIGHT_PREVIEW"

        fun schedulePeriodicCheck(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(6, TimeUnit.HOURS)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun scheduleTestNotification(context: Context, delaySeconds: Long = 5) {
            val workRequest = OneTimeWorkRequestBuilder<DailyCheckWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .addTag("fatec_test_work")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

    }
}
