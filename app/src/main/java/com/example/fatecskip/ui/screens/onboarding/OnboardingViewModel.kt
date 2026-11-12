package com.example.fatecskip.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.repository.AcademicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val repository: AcademicRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _selectedSemester = MutableStateFlow(1)
    val selectedSemester: StateFlow<Int> = _selectedSemester.asStateFlow()

    fun updateNickname(name: String) {
        _nickname.value = name
    }

    fun selectSemester(sem: Int) {
        _selectedSemester.value = sem
    }

    fun nextStep() {
        if (_currentStep.value < 2) {
            _currentStep.value += 1
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }

    fun finishOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val nameToSave = _nickname.value.ifBlank { "Guerreiro da Fatec" }
            preferencesRepository.setStudentNickname(nameToSave)
            preferencesRepository.setSelectedSemester(_selectedSemester.value)

            repository.syncFromApi(_selectedSemester.value)
            preferencesRepository.setOnboardingCompleted(true)

            onSuccess()
        }
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository,
        private val repository: AcademicRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(preferencesRepository, repository) as T
        }
    }
}
