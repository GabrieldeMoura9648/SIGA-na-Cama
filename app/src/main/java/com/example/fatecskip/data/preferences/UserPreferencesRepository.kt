package com.example.fatecskip.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_SELECTED_SEMESTER = intPreferencesKey("selected_semester")
        val KEY_COURAGE_LEVEL = floatPreferencesKey("courage_level")
        val KEY_STUDENT_NICKNAME = stringPreferencesKey("student_nickname")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_API_URL = stringPreferencesKey("api_url")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val KEY_ALARM_HOUR = intPreferencesKey("alarm_hour")
        val KEY_ALARM_MINUTE = intPreferencesKey("alarm_minute")
        val KEY_NIGHT_PREVIEW_ENABLED = booleanPreferencesKey("night_preview_enabled")

        const val DEFAULT_SEMESTER = 1
        const val DEFAULT_COURAGE_LEVEL = 0.75f // Estudante Médio
        const val DEFAULT_NICKNAME = "Guerreiro da Fatec"
        const val DEFAULT_API_URL = "https://raw.githubusercontent.com/GabrieldeMoura9648/SIGA-na-Cama/feat/rebrand-siga-na-cama/fatec_api.json"
    }

    val selectedSemesterFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SELECTED_SEMESTER] ?: DEFAULT_SEMESTER
        }

    val courageLevelFlow: Flow<Float> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_COURAGE_LEVEL] ?: DEFAULT_COURAGE_LEVEL
        }

    val studentNicknameFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_STUDENT_NICKNAME] ?: DEFAULT_NICKNAME
        }

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    val apiUrlFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val saved = preferences[KEY_API_URL]
            if (saved.isNullOrBlank() || saved.contains("antigravity/fatecskip")) {
                DEFAULT_API_URL
            } else {
                saved
            }
        }

    val isOnboardingCompletedFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    val alarmEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_ALARM_ENABLED] ?: false }

    val alarmHourFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_ALARM_HOUR] ?: 6 }

    val alarmMinuteFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_ALARM_MINUTE] ?: 30 }

    val nightPreviewEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_NIGHT_PREVIEW_ENABLED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSelectedSemester(semester: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_SEMESTER] = semester
        }
    }

    suspend fun setCourageLevel(level: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_COURAGE_LEVEL] = level
        }
    }

    suspend fun setStudentNickname(nickname: String) {
        dataStore.edit { preferences ->
            preferences[KEY_STUDENT_NICKNAME] = nickname.ifBlank { DEFAULT_NICKNAME }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setApiUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_API_URL] = url
        }
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ALARM_ENABLED] = enabled
        }
    }

    suspend fun setAlarmHour(hour: Int) {
        require(hour in 0..23) { "A hora do alarme deve estar entre 0 e 23." }
        dataStore.edit { preferences ->
            preferences[KEY_ALARM_HOUR] = hour
        }
    }

    suspend fun setAlarmMinute(minute: Int) {
        require(minute in 0..59) { "O minuto do alarme deve estar entre 0 e 59." }
        dataStore.edit { preferences ->
            preferences[KEY_ALARM_MINUTE] = minute
        }
    }

    suspend fun setNightPreviewEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NIGHT_PREVIEW_ENABLED] = enabled
        }
    }
}
