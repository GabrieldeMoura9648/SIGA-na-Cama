package com.example.fatecskip.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fatecskip.data.local.entities.SubjectEntity
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.repository.AcademicRepository
import com.example.fatecskip.domain.model.SkipVerdict
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val repository: AcademicRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val studentNickname: StateFlow<String> = preferencesRepository.studentNicknameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Guerreiro da Fatec")

    val selectedSemester: StateFlow<Int> = preferencesRepository.selectedSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val courageLevel: StateFlow<Float> = preferencesRepository.courageLevelFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.75f)

    // Lista reativa de matérias para o semestre atual
    val subjects: StateFlow<List<SubjectEntity>> = selectedSemester
        .flatMapLatest { semester ->
            repository.getSubjectsForSemester(semester)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _verdict = MutableStateFlow<SkipVerdict?>(null)
    val verdict: StateFlow<SkipVerdict?> = _verdict.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            // Se a grade estiver vazia no banco, força a carga silenciosamente
            val sem = selectedSemester.value
            val currentList = repository.getSubjectsForSemester(sem).first()
            if (currentList.isEmpty()) {
                repository.syncFromApi(sem)
            }
        }
    }

    fun consultOracle() {
        viewModelScope.launch {
            _isEvaluating.value = true
            // Suspense cômico do Oráculo calculando
            delay(500)
            val result = repository.evaluateSkipVerdict(
                date = LocalDate.now(),
                semester = selectedSemester.value,
                courageLevel = courageLevel.value
            )
            _verdict.value = result
            _isEvaluating.value = false
        }
    }

    fun addCustomSubject(
        code: String,
        name: String,
        semester: Int,
        dayOfWeek: Int,
        classesPerDay: Int,
        maxAbsences: Int
    ) {
        viewModelScope.launch {
            repository.addCustomSubject(
                code = code,
                name = name,
                semester = semester,
                dayOfWeek = dayOfWeek,
                classesPerDay = classesPerDay,
                maxAbsences = maxAbsences
            )
            _userFeedback.value = "Matéria de DP adicionada com sucesso no ${semester}º semestre!"
            if (_verdict.value != null) {
                consultOracle()
            }
        }
    }

    fun saveSubjectEdits(
        subjectId: Long,
        professor: String,
        classTime: String,
        daysOfWeek: String,
        currentAbsences: Int
    ) {
        viewModelScope.launch {
            repository.updateSubjectDetails(
                subjectId = subjectId,
                professor = professor,
                classTime = classTime,
                daysOfWeek = daysOfWeek,
                currentAbsences = currentAbsences
            )
            _userFeedback.value = "Matéria atualizada!"
            if (_verdict.value != null) {
                consultOracle()
            }
        }
    }

    fun getCatalogSubjectsBySemester(semester: Int): List<SubjectEntity> {
        return repository.getCatalogSubjectsBySemester(semester)
    }

    fun addSubjectFromOtherSemester(subject: SubjectEntity) {
        viewModelScope.launch {
            val resultId = repository.addExtraSubject(
                code = subject.code,
                name = subject.name,
                targetSemester = selectedSemester.value, // Adiciona na grade atual do aluno
                dayOfWeek = subject.dayOfWeek,
                classesPerDay = subject.classesPerDay,
                maxAbsences = subject.maxAbsences,
                isExtension = subject.isExtension
            )
            
            if (resultId != -1L) {
                _userFeedback.value = "Matéria '${subject.name}' adicionada à sua grade!"
            } else {
                _userFeedback.value = "Você já possui '${subject.name}' na sua grade."
            }
        }
    }

    fun incrementAbsence(subject: SubjectEntity, count: Int = 1) {
        viewModelScope.launch {
            repository.incrementAbsence(subject, count)
            // Se já tiver veredito na tela, atualiza automaticamente
            if (_verdict.value != null) {
                consultOracle()
            }
        }
    }

    fun decrementAbsence(subject: SubjectEntity, count: Int = 1) {
        viewModelScope.launch {
            repository.decrementAbsence(subject, count)
            if (_verdict.value != null) {
                consultOracle()
            }
        }
    }

    fun rerollExcuse() {
        viewModelScope.launch {
            val currentVerdict = _verdict.value ?: return@launch
            val newExcuse = repository.getRandomExcuse()
            when (currentVerdict) {
                is SkipVerdict.Allowed -> {
                    _verdict.value = currentVerdict.copy(excuse = newExcuse)
                }
                is SkipVerdict.WarningSafetyMargin -> {
                    _verdict.value = currentVerdict.copy(excuse = newExcuse)
                }
                else -> {}
            }
        }
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    fun showFeedback(message: String) {
        _userFeedback.value = message
    }

    class Factory(
        private val repository: AcademicRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, preferencesRepository) as T
        }
    }
}
