package com.example.fatecskip.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fatecskip.R
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.ui.screens.alarm.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val alarmType = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE)
            ?: AlarmScheduler.TYPE_DAILY
        val applicationContext = context.applicationContext

        if (
            alarmType == AlarmScheduler.TYPE_NIGHT_PREVIEW ||
            alarmType == AlarmScheduler.TYPE_TEST_NIGHT_PREVIEW
        ) {
            val isTestPreview = alarmType == AlarmScheduler.TYPE_TEST_NIGHT_PREVIEW
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val preferences = UserPreferencesRepository(applicationContext)
                    if (isTestPreview || preferences.nightPreviewEnabledFlow.first()) {
                        if (!isTestPreview) {
                            AlarmScheduler.scheduleNightPreview(applicationContext)
                        }
                        val request = OneTimeWorkRequestBuilder<DailyCheckWorker>()
                            .setInputData(
                                Data.Builder()
                                    .putBoolean(DailyCheckWorker.IS_NIGHT_PREVIEW, true)
                                    .build()
                            )
                            .build()
                        WorkManager.getInstance(applicationContext).enqueue(request)
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Não foi possível iniciar o preview noturno.", exception)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (alarmType == AlarmScheduler.TYPE_DAILY) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val preferences = UserPreferencesRepository(applicationContext)
                    if (preferences.alarmEnabledFlow.first()) {
                        AlarmScheduler.scheduleDailyAlarm(applicationContext)
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Não foi possível reagendar o alarme diário.", exception)
                } finally {
                    pendingResult.finish()
                }
            }
        }

        showAlarmNotification(applicationContext, alarmType)

        val activityIntent = Intent(applicationContext, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, alarmType)
        }
        try {
            applicationContext.startActivity(activityIntent)
        } catch (exception: SecurityException) {
            Log.i(TAG, "A Activity será aberta pelo full-screen intent da notificação.", exception)
        }
    }

    private fun showAlarmNotification(context: Context, alarmType: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Despertador SIGA na Cama",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarmes matinais com o resumo da sua grade"
                    enableVibration(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
            )
        }

        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, alarmType)
        }
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            ALARM_ACTIVITY_REQUEST,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bed)
            .setContentTitle("⏰ Hora de conferir sua grade!")
            .setContentText("Seu despertador está tocando. Levante para conferir o dia.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ALARM_NOTIFICATION_ID, notification)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Permissão para exibir a notificação do alarme não concedida.", exception)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ALARM_CHANNEL_ID = "siga_na_cama_alarm"
        const val ALARM_NOTIFICATION_ID = 4210
        private const val ALARM_ACTIVITY_REQUEST = 5201
    }
}
