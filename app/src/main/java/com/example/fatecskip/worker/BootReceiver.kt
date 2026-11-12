package com.example.fatecskip.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val applicationContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = UserPreferencesRepository(applicationContext)
                if (preferences.alarmEnabledFlow.first()) {
                    AlarmScheduler.scheduleDailyAlarm(applicationContext)
                }
                if (preferences.nightPreviewEnabledFlow.first()) {
                    AlarmScheduler.scheduleNightPreview(applicationContext)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível restaurar os agendamentos após a inicialização.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
