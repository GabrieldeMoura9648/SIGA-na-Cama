package com.example.fatecskip.ui.screens.settings

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
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
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setOnErrorListener { failedPlayer, what, extra ->
                if (mediaPlayer === failedPlayer) mediaPlayer = null
                Log.e("ExternalSoundViewModel", "Erro na prévia do áudio ($what/$extra).")
                failedPlayer.release()
                true
            }
            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) mediaPlayer = null
                completedPlayer.release()
            }
            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer === preparedPlayer) {
                    preparedPlayer.start()
                } else {
                    preparedPlayer.release()
                }
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(mp3Url)
            player.prepareAsync()
        } catch (e: Exception) {
            if (mediaPlayer === player) stopPreview() else player.release()
            _statusMessage.value = "Erro ao reproduzir a prévia do som."
        }
    }

    fun stopPreview() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (e: IllegalStateException) {
            Log.w("ExternalSoundViewModel", "Prévia encerrada durante uma transição do player.", e)
        } finally {
            player.release()
        }
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
