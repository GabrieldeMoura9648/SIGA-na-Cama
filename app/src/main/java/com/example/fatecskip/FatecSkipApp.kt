package com.example.fatecskip

import android.app.Application
import com.example.fatecskip.data.local.AppDatabase
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.remote.FatecApiService
import com.example.fatecskip.data.repository.AcademicRepository
import com.example.fatecskip.data.remote.HolidayApiService
import com.example.fatecskip.data.repository.HolidayRepository
import com.example.fatecskip.worker.DailyCheckWorker
import com.example.fatecskip.worker.AlarmScheduler
import com.example.fatecskip.worker.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FatecSkipApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var apiService: FatecApiService
        private set

    lateinit var repository: AcademicRepository
        private set

    lateinit var holidayRepository: HolidayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        apiService = FatecApiService.create()
        holidayRepository = HolidayRepository(HolidayApiService.create())

        repository = AcademicRepository(
            subjectDao = database.subjectDao(),
            eventDao = database.eventDao(),
            excuseDao = database.excuseDao(),
            apiService = apiService,
            preferencesRepository = preferencesRepository,
            holidayRepository = holidayRepository
        )

        NotificationHelper.createNotificationChannel(this)
        DailyCheckWorker.schedulePeriodicCheck(this)
        scheduleSavedAlarms()
    }

    private fun scheduleSavedAlarms() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            preferencesRepository.alarmEnabledFlow
                .combine(preferencesRepository.isOnboardingCompletedFlow) { enabled, onboardingCompleted ->
                    enabled && onboardingCompleted
                }
                .collect { shouldSchedule ->
                    if (shouldSchedule) {
                        AlarmScheduler.scheduleDailyAlarm(this@FatecSkipApp)
                    } else {
                        AlarmScheduler.cancelAlarm(this@FatecSkipApp)
                    }
                }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            preferencesRepository.nightPreviewEnabledFlow
                .combine(preferencesRepository.isOnboardingCompletedFlow) { enabled, onboardingCompleted ->
                    enabled && onboardingCompleted
                }
                .collect { shouldSchedule ->
                    if (shouldSchedule) {
                        AlarmScheduler.scheduleNightPreview(this@FatecSkipApp)
                    } else {
                        AlarmScheduler.cancelNightPreview(this@FatecSkipApp)
                    }
                }
        }
    }

    companion object {
        lateinit var instance: FatecSkipApp
            private set
    }
}
