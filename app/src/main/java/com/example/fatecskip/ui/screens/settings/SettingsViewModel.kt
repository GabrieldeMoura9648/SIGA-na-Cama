package com.example.fatecskip.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.repository.AcademicRepository
import com.example.fatecskip.worker.AlarmScheduler
import com.example.fatecskip.worker.DailyCheckWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AcademicRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val studentNickname: StateFlow<String> = preferencesRepository.studentNicknameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guerreiro da Fatec")

    val selectedSemester: StateFlow<Int> = preferencesRepository.selectedSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val apiUrl: StateFlow<String> = preferencesRepository.apiUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.DEFAULT_API_URL)

    val alarmEnabled: StateFlow<Boolean> = preferencesRepository.alarmEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val alarmHour: StateFlow<Int> = preferencesRepository.alarmHourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val alarmMinute: StateFlow<Int> = preferencesRepository.alarmMinuteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val alarmMode: StateFlow<String> = preferencesRepository.alarmModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "manual")

    val alarmAutoOffsetMinutes: StateFlow<Int> = preferencesRepository.alarmAutoOffsetMinutesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val isAlarmFallbackWarning: StateFlow<Boolean> = preferencesRepository.isAlarmFallbackWarningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nightPreviewEnabled: StateFlow<Boolean> = preferencesRepository.nightPreviewEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nightPreviewHour: StateFlow<Int> = preferencesRepository.nightPreviewHourFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)

    val nightPreviewMinute: StateFlow<Int> = preferencesRepository.nightPreviewMinuteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun updateNickname(name: String) {
        viewModelScope.launch {
            preferencesRepository.setStudentNickname(name)
            _feedbackMessage.value = "Apelido atualizado para '$name'!"
        }
    }

    fun updateSemester(semester: Int) {
        viewModelScope.launch {
            preferencesRepository.setSelectedSemester(semester)
            _feedbackMessage.value = "Semestre alterado para o ${semester}º semestre!"
            // Aciona automaticamente a sincronização/carga do novo semestre
            syncFromApi()
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            _feedbackMessage.value = if (enabled) {
                "Notificações ativadas! Você receberá lembretes acadêmicos."
            } else {
                "Notificações desativadas. Durma por sua conta e risco."
            }
        }
    }

    fun updateApiUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setApiUrl(url)
        }
    }

    fun syncFromApi() {
        viewModelScope.launch {
            _isSyncing.value = true
            val currentSemester = selectedSemester.value
            val currentUrl = apiUrl.value
            val result = repository.syncFromApi(currentSemester, currentUrl)
            _isSyncing.value = false

            result.fold(
                onSuccess = { msg -> _feedbackMessage.value = msg },
                onFailure = { err -> _feedbackMessage.value = "Erro na sincronização: ${err.localizedMessage}" }
            )
        }
    }

    fun triggerTestNotification(context: Context, currentNicknameInput: String) {
        viewModelScope.launch {
            // Garante que a última versão do nome na tela seja salva no DataStore
            preferencesRepository.setStudentNickname(currentNicknameInput)
            
            DailyCheckWorker.scheduleTestNotification(context, delaySeconds = 5)
            _feedbackMessage.value = "⚡ Teste agendado! Em 5 segundos você receberá uma notificação..."
        }
    }

    fun updateAlarmEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAlarmEnabled(enabled)
            if (enabled) {
                val exact = AlarmScheduler.scheduleDailyAlarm(context)
                _feedbackMessage.value = if (exact) {
                    "⏰ Despertador ativado com horário exato."
                } else {
                    "⏰ Despertador ativado; conceda permissão para maior precisão."
                }
            } else {
                AlarmScheduler.cancelAlarm(context)
                _feedbackMessage.value = "Despertador matinal desativado."
            }
        }
    }

    fun updateAlarmTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setAlarmHour(hour)
            preferencesRepository.setAlarmMinute(minute)
            if (alarmEnabled.value) {
                AlarmScheduler.scheduleDailyAlarm(context)
                _feedbackMessage.value = "⏰ Horário do alarme atualizado."
            }
        }
    }

    fun updateAlarmMode(context: Context, mode: String) {
        viewModelScope.launch {
            preferencesRepository.setAlarmMode(mode)
            if (alarmEnabled.value) {
                AlarmScheduler.scheduleDailyAlarm(context)
                _feedbackMessage.value = "⏰ Modo do alarme atualizado."
            }
        }
    }

    fun updateAlarmAutoOffset(context: Context, offsetMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setAlarmAutoOffsetMinutes(offsetMinutes)
            if (alarmEnabled.value) {
                AlarmScheduler.scheduleDailyAlarm(context)
                _feedbackMessage.value = "⏰ Antecedência atualizada."
            }
        }
    }

    fun updateNightPreviewEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNightPreviewEnabled(enabled)
            if (enabled) {
                val exact = AlarmScheduler.scheduleNightPreview(context)
                _feedbackMessage.value = if (exact) {
                    "🌙 Preview noturno ativado para ${formatNightPreviewTime()}."
                } else {
                    "🌙 Preview ativado para ${formatNightPreviewTime()}, sem precisão exata."
                }
            } else {
                AlarmScheduler.cancelNightPreview(context)
                _feedbackMessage.value = "Preview noturno desativado."
            }
        }
    }

    fun updateNightPreviewTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setNightPreviewTime(hour, minute)
            if (nightPreviewEnabled.value) {
                AlarmScheduler.scheduleNightPreview(context)
            }
            _feedbackMessage.value = "Horário do preview atualizado para ${"%02d:%02d".format(hour, minute)}."
        }
    }

    private fun formatNightPreviewTime(): String =
        "%02d:%02d".format(nightPreviewHour.value, nightPreviewMinute.value)

    fun triggerTestAlarm(context: Context) {
        viewModelScope.launch {
            val exact = AlarmScheduler.scheduleTestAlarmInSeconds(context, 5)
            _feedbackMessage.value = if (exact) {
                "⚡ Alarme real agendado para tocar em 5 segundos."
            } else {
                "⚡ Alarme de teste agendado sem precisão exata."
            }
        }
    }

    fun triggerTestNightPreview(context: Context) {
        val exact = AlarmScheduler.scheduleTestNightPreviewInSeconds(context, 5)
        _feedbackMessage.value = if (exact) {
            "🌙 Preview de amanhã agendado para daqui a 5 segundos."
        } else {
            "🌙 Preview de teste agendado sem precisão exata."
        }
    }

    fun refreshAlarmSchedule(context: Context) {
        viewModelScope.launch {
            if (alarmEnabled.value) {
                val exact = AlarmScheduler.scheduleDailyAlarm(context)
                _feedbackMessage.value = if (exact) {
                    "Permissão concedida. Despertador exato ativado."
                } else {
                    "O sistema ainda não concedeu permissão para alarmes exatos."
                }
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
            _feedbackMessage.value = "Configuração inicial redefinida! Abra o app novamente para vê-la."
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    class Factory(
        private val repository: AcademicRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository, preferencesRepository) as T
        }
    }
}
