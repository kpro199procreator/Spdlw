package com.spotdl.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotdl.android.data.model.*
import com.spotdl.android.data.repository.DownloadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel principal con toda la lógica de negocio
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
    
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()
    
    // Estado de descargas
    private val _downloads = MutableStateFlow<List<DownloadProgress>>(emptyList())
    val downloads: StateFlow<List<DownloadProgress>> = _downloads.asStateFlow()
    
    private val _activeDownloads = MutableStateFlow(0)
    val activeDownloads: StateFlow<Int> = _activeDownloads.asStateFlow()
    
    // Configuración actual
    private val _currentConfig = MutableStateFlow(DownloadConfig())
    val currentConfig: StateFlow<DownloadConfig> = _currentConfig.asStateFlow()
    
    // Estadísticas
    private val _totalDownloaded = MutableStateFlow(0)
    val totalDownloaded: StateFlow<Int> = _totalDownloaded.asStateFlow()
    
    private val _totalFailed = MutableStateFlow(0)
    val totalFailed: StateFlow<Int> = _totalFailed.asStateFlow()
    
    init {
        // Inicializar repository
        viewModelScope.launch {
            repository.initialize()
        }
        
        // Observar progreso de descargas
        viewModelScope.launch {
            repository.downloadProgress.collect { progresses ->
                _downloads.value = progresses
                _activeDownloads.value = progresses.count { 
                    it.status == DownloadStatus.DOWNLOADING || 
                    it.status == DownloadStatus.PROCESSING 
                }
                _totalDownloaded.value = progresses.count { it.status == DownloadStatus.COMPLETED }
                _totalFailed.value = progresses.count { it.status == DownloadStatus.FAILED }
            }
        }
    }
    
    /**
     * Buscar canciones en Spotify
     */
    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchError.value = null
                _searchQuery.value = query
                
                val results = if (query.startsWith("http")) {
                    // Es una URL - obtener canción específica
                    val song = repository.getSongFromSpotifyUrl(query)
                    if (song != null) listOf(song) else emptyList()
                } else {
                    // Es una búsqueda - buscar en Spotify
                    repository.searchSongs(query)
                }
                
                _searchResults.value = results
                
                if (results.isEmpty()) {
                    _searchError.value = "No se encontraron resultados"
                }
            } catch (e: Exception) {
                _searchError.value = "Error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    /**
     * Descargar una canción
     */
    fun downloadSong(song: Song) {
        viewModelScope.launch {
            try {
                val result = repository.downloadSong(song, _currentConfig.value)
                
                if (result.isSuccess) {
                    // Descarga iniciada exitosamente
                } else {
                    _searchError.value = "Error al iniciar descarga: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _searchError.value = "Error: ${e.message}"
            }
        }
    }
    
    /**
     * Cancelar descarga
     */
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            repository.cancelDownload(downloadId)
        }
    }
    
    /**
     * Reintentar descarga fallida
     */
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            val download = _downloads.value.find { it.downloadId == downloadId }
            if (download != null) {
                downloadSong(download.song)
            }
        }
    }
    
    /**
     * Limpiar descargas completadas
     */
    fun clearCompletedDownloads() {
        viewModelScope.launch {
            val current = _downloads.value
            val filtered = current.filter { it.status != DownloadStatus.COMPLETED }
            _downloads.value = filtered
        }
    }
    
    /**
     * Actualizar configuración
     */
    fun updateConfig(config: DownloadConfig) {
        _currentConfig.value = config
    }
    
    /**
     * Actualizar formato de audio
     */
    fun updateAudioFormat(format: String) {
        _currentConfig.value = _currentConfig.value.copy(format = format)
    }
    
    /**
     * Actualizar calidad
     */
    fun updateQuality(quality: Int) {
        _currentConfig.value = _currentConfig.value.copy(quality = quality)
    }
    
    /**
     * Actualizar si embeber metadatos
     */
    fun updateEmbedMetadata(embed: Boolean) {
        _currentConfig.value = _currentConfig.value.copy(embedMetadata = embed)
    }
    
    /**
     * Actualizar si embeber artwork
     */
    fun updateEmbedArtwork(embed: Boolean) {
        _currentConfig.value = _currentConfig.value.copy(embedArtwork = embed)
    }
    
    /**
     * Actualizar plantilla de nombre
     */
    fun updateFilenameTemplate(template: String) {
        _currentConfig.value = _currentConfig.value.copy(filenameTemplate = template)
    }
    
    /**
     * Limpiar error de búsqueda
     */
    fun clearSearchError() {
        _searchError.value = null
    }
    
    /**
     * Obtener download por ID
     */
    fun getDownload(downloadId: String): DownloadProgress? {
        return _downloads.value.find { it.downloadId == downloadId }
    }
    
    /**
     * Obtener estadísticas
     */
    fun getStats(): DownloadStats {
        val current = _downloads.value
        return DownloadStats(
            total = current.size,
            completed = current.count { it.status == DownloadStatus.COMPLETED },
            failed = current.count { it.status == DownloadStatus.FAILED },
            downloading = current.count { 
                it.status == DownloadStatus.DOWNLOADING || 
                it.status == DownloadStatus.PROCESSING 
            },
            pending = current.count { it.status == DownloadStatus.PENDING }
        )
    }
}

/**
 * Estadísticas de descargas
 */
data class DownloadStats(
    val total: Int,
    val completed: Int,
    val failed: Int,
    val downloading: Int,
    val pending: Int
)
