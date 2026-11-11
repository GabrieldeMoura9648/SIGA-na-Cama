package com.example.fatecskip.ui.screens.settings

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.remote.dto.MyInstantSoundDto
import com.example.fatecskip.data.repository.ExternalSoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExternalSoundViewModel(
    private val repository: ExternalSoundRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _sounds = MutableStateFlow<List<MyInstantSoundDto>>(emptyList())
    val sounds: StateFlow<List<MyInstantSoundDto>> = _sounds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedSoundName = MutableStateFlow<String?>("Som Padrão")
    val selectedSoundName: StateFlow<String?> = _selectedSoundName.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            _selectedSoundName.value = preferencesRepository.customAlarmNameFlow.first() ?: "Som Padrão"
            search("bom dia")
        }
    }

    private suspend fun loadTrending() {
        _isLoading.value = true
        _statusMessage.value = "Carregando em alta..."
        val result = repository.getTrendingSoundsBr()
        _sounds.value = result
        _isLoading.value = false
        if (result.isEmpty()) {
            _statusMessage.value = "Nenhum som encontrado."
        } else {
            _statusMessage.value = null
        }
    }

    fun getTrendingSoundsBr() {
        viewModelScope.launch {
            loadTrending()
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Buscando '$query'..."
            val result = repository.searchSoundsPtBr(query)
            _sounds.value = result
            _isLoading.value = false
            if (result.isEmpty()) {
                _statusMessage.value = "Nenhum som encontrado."
            } else {
                _statusMessage.value = null
            }
        }
    }

    fun previewSound(mp3Url: String) {
        stopPreview()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(mp3Url)
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    fun selectAndDownloadSound(item: MyInstantSoundDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Baixando áudio..."
            stopPreview()

            val localPath = repository.downloadAndCacheSound(item.id, item.mp3)
            if (localPath != null) {
                preferencesRepository.saveCustomAlarm(localPath, item.title)
                _selectedSoundName.value = item.title
                _statusMessage.value = "Som salvo com sucesso!"
            } else {
                _statusMessage.value = "Erro ao baixar o som."
            }
            _isLoading.value = false
        }
    }

    fun restoreDefaultSound() {
        viewModelScope.launch {
            preferencesRepository.saveCustomAlarm(null, null)
            _selectedSoundName.value = "Som Padrão"
            _statusMessage.value = "Restaurado para o som nativo."
            stopPreview()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }

    class Factory(
        private val repository: ExternalSoundRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExternalSoundViewModel(repository, preferencesRepository) as T
        }
    }
}
