package com.spotdl.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotdl.android.data.model.*
import com.spotdl.android.data.repository.DownloadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel principal de la aplicación
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    // Estado de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Estado de descargas
    val downloads = repository.downloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Configuración de descarga
    private val _downloadConfig = MutableStateFlow(
        DownloadConfig(
            format = AudioFormat.MP3,
            quality = AudioQuality.HIGH,
            embedArtwork = true,
            embedMetadata = true,
            outputDirectory = "",
            filenameTemplate = "{artist} - {title}"
        )
    )
    val downloadConfig: StateFlow<DownloadConfig> = _downloadConfig.asStateFlow()

    // Estado de errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Estado de mensajes
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Actualiza la consulta de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Realiza una búsqueda
     */
    fun search(query: String = _searchQuery.value) {
        if (query.isBlank()) return

        viewModelScope.launch {
            try {
                _isSearching.value = true
                _error.value = null

                // Detectar si es una URL de Spotify
                val result = if (query.contains("spotify.com/track/")) {
                    repository.getSongFromSpotifyUrl(query)?.let { listOf(it) } ?: emptyList()
                } else {
                    // Buscar en YouTube
                    repository.searchSong(query)?.let { listOf(it) } ?: emptyList()
                }

                _searchResults.value = result

                if (result.isEmpty()) {
                    _message.value = "No se encontraron resultados"
                }
            } catch (e: Exception) {
                _error.value = "Error en la búsqueda: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Descarga una canción
     */
    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                _error.value = null
                
                val result = repository.downloadSong(song, _downloadConfig.value)
                
                if (result.isSuccess) {
                    _message.value = "Descarga completada: ${song.title}"
                } else {
                    _error.value = "Error descargando: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Descarga múltiples canciones
     */
    fun downloadMultipleSongs(songs: List<Song>) {
        songs.forEach { song ->
            downloadSong(song)
        }
    }

    /**
     * Procesa una URL compartida (Spotify o YouTube)
     */
    fun processSharedUrl(url: String) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _error.value = null

                val song = when {
                    url.contains("spotify.com/track/") -> {
                        repository.getSongFromSpotifyUrl(url)
                    }
                    url.contains("youtube.com/watch") || url.contains("youtu.be/") -> {
                        // Extraer título de YouTube y buscar
                        repository.searchSong(url)
                    }
                    else -> null
                }

                if (song != null) {
                    _searchResults.value = listOf(song)
                    _message.value = "Listo para descargar: ${song.title}"
                } else {
                    _error.value = "No se pudo procesar la URL"
                }
            } catch (e: Exception) {
                _error.value = "Error procesando URL: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Actualiza la configuración de descarga
     */
    fun updateDownloadConfig(config: DownloadConfig) {
        _downloadConfig.value = config
    }

    /**
     * Actualiza el formato de audio
     */
    fun updateAudioFormat(format: AudioFormat) {
        _downloadConfig.value = _downloadConfig.value.copy(format = format)
    }

    /**
     * Actualiza la calidad de audio
     */
    fun updateAudioQuality(quality: AudioQuality) {
        _downloadConfig.value = _downloadConfig.value.copy(quality = quality)
    }

    /**
     * Actualiza si se debe incrustar artwork
     */
    fun updateEmbedArtwork(embed: Boolean) {
        _downloadConfig.value = _downloadConfig.value.copy(embedArtwork = embed)
    }

    /**
     * Actualiza si se deben incrustar metadatos
     */
    fun updateEmbedMetadata(embed: Boolean) {
        _downloadConfig.value = _downloadConfig.value.copy(embedMetadata = embed)
    }

    /**
     * Actualiza el directorio de salida
     */
    fun updateOutputDirectory(directory: String) {
        _downloadConfig.value = _downloadConfig.value.copy(outputDirectory = directory)
    }

    /**
     * Actualiza la plantilla de nombre de archivo
     */
    fun updateFilenameTemplate(template: String) {
        _downloadConfig.value = _downloadConfig.value.copy(filenameTemplate = template)
    }

    /**
     * Cancela una descarga
     */
    fun cancelDownload(downloadId: String) {
        repository.cancelDownload(downloadId)
    }

    /**
     * Limpia las descargas completadas
     */
    fun clearCompletedDownloads() {
        repository.clearCompleted()
    }

    /**
     * Limpia el error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Limpia el mensaje
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Limpia los resultados de búsqueda
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchQuery.value = ""
    }
}
