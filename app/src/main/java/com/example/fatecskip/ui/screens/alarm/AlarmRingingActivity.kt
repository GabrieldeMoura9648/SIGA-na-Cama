package com.example.fatecskip.ui.screens.alarm

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.example.fatecskip.FatecSkipApp
import com.example.fatecskip.domain.model.SkipVerdict
import com.example.fatecskip.worker.AlarmReceiver
import com.example.fatecskip.worker.AlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.io.File
import android.media.AudioAttributes
import android.media.MediaPlayer

class AlarmRingingActivity : ComponentActivity() {

    private var isAlarmHandled = false
    private var hasClassesToday by mutableStateOf(false)
    private var isRegisteringAbsences by mutableStateOf(false)
    private var oracleMessage by mutableStateOf("O Oráculo está consultando seu dia...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAlarmHandled = savedInstanceState?.getBoolean(KEY_ALARM_HANDLED) ?: false
        showOverLockScreen()
        if (!isAlarmHandled) {
            startAlarmSound()
            startVibration()
        }

        window.statusBarColor = Color(0xFF0D0E1A).toArgb()
        window.navigationBarColor = Color(0xFF0D0E1A).toArgb()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF0D0E1A),
                surface = Color(0xFF0D0E1A),
                primary = Color(0xFF8B5CF6),
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D0E1A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏰", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SIGA na cama",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = oracleMessage,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFFFD166)
                        )
                        Spacer(modifier = Modifier.height(40.dp))
                        Button(
                            onClick = ::snoozeAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            enabled = !isAlarmHandled && !isRegisteringAbsences
                        ) {
                            Text("Soneca (+10 min)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (hasClassesToday) {
                            Button(
                                onClick = ::registerAbsenceAndClose,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                enabled = !isAlarmHandled && !isRegisteringAbsences
                            ) {
                                Text(
                                    if (isRegisteringAbsences) "Registrando falta..."
                                    else "🛌 Vou Faltar Hoje (+Falta)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Button(
                            onClick = ::dismissAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            enabled = !isAlarmHandled && !isRegisteringAbsences
                        ) {
                            Text("Dispensar (Vou pra Aula)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        loadOracleVerdict()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmSound() {
        lifecycleScope.launch {
            if (activeRingtone?.isPlaying == true || activeMediaPlayer?.isPlaying == true) return@launch

            var customPlayer: MediaPlayer? = null
            try {
                val app = application as FatecSkipApp
                val customPath = app.preferencesRepository.customAlarmPathFlow.first()
                if (!customPath.isNullOrBlank()) {
                    val file = File(customPath)
                    if (file.exists() && file.length() > 0) {
                        val player = MediaPlayer()
                        customPlayer = player
                        activeMediaPlayer = player
                        player.setOnErrorListener { failedPlayer, what, extra ->
                            Log.e(TAG, "Erro assíncrono no som customizado ($what/$extra); usando ringtone nativo.")
                            startNativeAlarmSound(failedPlayer)
                            true
                        }
                        player.apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .build()
                            )
                            setDataSource(file.absolutePath)
                            isLooping = true
                            prepare()
                            start()
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao tocar som customizado, caindo para fallback.", e)
                startNativeAlarmSound(customPlayer)
                return@launch
            }

            startNativeAlarmSound()
        }
    }

    private fun startNativeAlarmSound(failedPlayer: MediaPlayer? = null) {
        failedPlayer?.let { player ->
            if (activeMediaPlayer === player) activeMediaPlayer = null
            try {
                player.release()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "O player customizado já estava encerrado.", e)
            }
        }
        if (activeRingtone?.isPlaying == true) return

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                Log.w(TAG, "O dispositivo não possui um toque de alarme padrão.")
                return
            }
            activeRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            activeRingtone?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLooping = true
                it.play()
            } ?: Log.w(TAG, "Não foi possível iniciar o toque de alarme.")
        } catch (e: Exception) {
            Log.e(TAG, "Não foi possível iniciar o toque de alarme nativo.", e)
        }
    }

    private fun startVibration() {
        if (isVibrating) return
        val deviceVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (deviceVibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 700, 500, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                deviceVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                deviceVibrator.vibrate(pattern, 0)
            }
            activeVibrator = deviceVibrator
            isVibrating = true
        }
    }

    private fun loadOracleVerdict() {
        lifecycleScope.launch {
            try {
                val preferences = (application as FatecSkipApp).preferencesRepository
                val semester = preferences.selectedSemesterFlow.first()
                val verdict = (application as FatecSkipApp).repository.evaluateSkipVerdict(
                    date = LocalDate.now(),
                    semester = semester,
                    courageLevel = preferences.courageLevelFlow.first()
                )
                hasClassesToday = (application as FatecSkipApp).repository
                    .getSubjectsForSemester(semester)
                    .first()
                    .any { it.isClassOnDay(LocalDate.now().dayOfWeek.value) }
                oracleMessage = when (verdict) {
                    is SkipVerdict.ForbiddenCriticalEvent ->
                        "🚨 DIA DE PROVA! Nem pense em ativar o modo soneca!"
                    is SkipVerdict.ForbiddenAbsenceLimit ->
                        "💀 PROIBIDO FALTAR! Levanta ou perde até o Exame Final!"
                    is SkipVerdict.Allowed, is SkipVerdict.NoClassOrHoliday ->
                        "🛌 CAMA AUTORIZADA! O Oráculo liberou seu sono hoje."
                    is SkipVerdict.WarningSafetyMargin ->
                        "⚠️ CUIDADO! Você está perto do limite de faltas. Consulte o Oráculo antes de decidir."
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível consultar o veredito do Oráculo.", exception)
                oracleMessage = "⚠️ Não foi possível consultar sua grade. Confira o Oráculo no app."
            }
        }
    }

    private fun snoozeAlarm() {
        if (isAlarmHandled) return
        val exact = AlarmScheduler.scheduleSnooze(this, 10)
        if (!exact) {
            Toast.makeText(this, "Soneca agendada; permissão de alarme exato indisponível.", Toast.LENGTH_LONG).show()
        }
        stopAndCloseAlarm()
    }

    private fun dismissAlarm() {
        if (isAlarmHandled) return
        AlarmScheduler.cancelSnooze(this)
        lifecycleScope.launch {
            try {
                val preferences = (application as FatecSkipApp).preferencesRepository
                if (preferences.alarmEnabledFlow.first()) {
                    val exact = AlarmScheduler.scheduleDailyAlarm(this@AlarmRingingActivity)
                    if (!exact) {
                        Toast.makeText(
                            this@AlarmRingingActivity,
                            "Próximo alarme agendado sem precisão exata.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível reagendar o alarme diário.", exception)
                Toast.makeText(
                    this@AlarmRingingActivity,
                    "Não foi possível reagendar o alarme diário.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                stopAndCloseAlarm()
            }
        }
    }

    private fun registerAbsenceAndClose() {
        if (isAlarmHandled || isRegisteringAbsences) return
        isRegisteringAbsences = true
        lifecycleScope.launch {
            try {
                val app = application as FatecSkipApp
                val preferences = app.preferencesRepository
                val semester = preferences.selectedSemesterFlow.first()
                val count = app.repository.registerAbsenceForToday(semester)
                if (count > 0) {
                    Toast.makeText(
                        this@AlarmRingingActivity,
                        "Falta registrada no SIGA na cama! Bons sonhos 🛌",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@AlarmRingingActivity,
                        "Nenhuma aula de hoje encontrada na sua grade.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (preferences.alarmEnabledFlow.first()) {
                    val exact = AlarmScheduler.scheduleDailyAlarm(this@AlarmRingingActivity)
                    if (!exact) {
                        Toast.makeText(
                            this@AlarmRingingActivity,
                            "Próximo alarme agendado sem precisão exata.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Não foi possível registrar as faltas de hoje.", exception)
                Toast.makeText(
                    this@AlarmRingingActivity,
                    "Não foi possível registrar a falta. Tente novamente.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                stopAndCloseAlarm()
            }
        }
    }

    private fun stopAndCloseAlarm() {
        if (isAlarmHandled) return
        isAlarmHandled = true
        stopAlarmSound()
        finishAndRemoveTask()
    }

    private fun stopAlarmSound() {
        activeMediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        activeMediaPlayer = null
        activeRingtone?.stop()
        activeRingtone = null
        activeVibrator?.cancel()
        activeVibrator = null
        isVibrating = false
        NotificationManagerCompat.from(this).cancel(AlarmReceiver.ALARM_NOTIFICATION_ID)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_ALARM_HANDLED, isAlarmHandled)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) stopAlarmSound()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmRingingActivity"
        private const val KEY_ALARM_HANDLED = "alarm_handled"
        private var activeMediaPlayer: MediaPlayer? = null
        private var activeRingtone: Ringtone? = null
        private var activeVibrator: Vibrator? = null
        private var isVibrating = false
    }
}
