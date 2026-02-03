package com.spotdl.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spotdl.android.data.model.*
import com.spotdl.android.data.service.BinaryDownloadService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para la configuración inicial de binarios
 */
class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadService = BinaryDownloadService(application)
    
    // Estado de los binarios
    val binaryStates = downloadService.downloadStates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Fuentes de descarga disponibles
    private val _availableSources = MutableStateFlow(BinaryRepositories.getDefaultSources())
    val availableSources: StateFlow<List<DownloadSource>> = _availableSources.asStateFlow()

    // Fuente seleccionada
    private val _selectedSource = MutableStateFlow(BinaryRepositories.OFFICIAL_GITHUB)
    val selectedSource: StateFlow<DownloadSource> = _selectedSource.asStateFlow()

    // Estado del setup
    private val _setupState = MutableStateFlow(SetupState.CHECKING)
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    // Progreso general
    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    // Arquitectura del dispositivo
    private val _deviceArch = MutableStateFlow(getDeviceArchitecture())
    val deviceArch: StateFlow<String> = _deviceArch.asStateFlow()

    // Binarios a instalar
    private val _binariesToInstall = MutableStateFlow<List<BinaryInfo>>(emptyList())
    val binariesToInstall: StateFlow<List<BinaryInfo>> = _binariesToInstall.asStateFlow()

    // Modo de instalación (requeridos solamente o todos)
    private val _installMode = MutableStateFlow(InstallMode.REQUIRED_ONLY)
    val installMode: StateFlow<InstallMode> = _installMode.asStateFlow()

    init {
        checkBinaries()
    }

    /**
     * Verifica el estado de los binarios
     */
    fun checkBinaries() {
        viewModelScope.launch {
            _setupState.value = SetupState.CHECKING
            
            val arch = _deviceArch.value
            val allBinaries = BinaryCatalog.getAllBinaries(arch)
            
            val states = downloadService.checkInstalledBinaries(allBinaries)
            
            // Determinar qué binarios necesitan instalarse
            val needsInstall = states.values.any { 
                it.status == BinaryStatus.NOT_INSTALLED && it.info.required 
            }
            
            _setupState.value = if (needsInstall) {
                SetupState.NEEDS_SETUP
            } else {
                val hasUpdates = states.values.any { 
                    it.status == BinaryStatus.UPDATE_AVAILABLE 
                }
                if (hasUpdates) SetupState.UPDATES_AVAILABLE else SetupState.READY
            }
            
            // Preparar lista de binarios según el modo
            updateBinariesToInstall()
        }
    }

    /**
     * Actualiza la lista de binarios a instalar según el modo
     */
    private fun updateBinariesToInstall() {
        val arch = _deviceArch.value
        val allBinaries = BinaryCatalog.getAllBinaries(arch)
        
        _binariesToInstall.value = when (_installMode.value) {
            InstallMode.REQUIRED_ONLY -> allBinaries.filter { it.required }
            InstallMode.RECOMMENDED -> {
                // FFmpeg + (yt-dlp + Python) o yt-dlp-android
                allBinaries.filter { 
                    it.name == "ffmpeg" || 
                    it.name == "yt-dlp" || 
                    it.name == "python3.11" 
                }
            }
            InstallMode.ALL -> allBinaries
            InstallMode.CUSTOM -> {
                // El usuario seleccionará manualmente
                allBinaries
            }
        }
    }

    /**
     * Cambia el modo de instalación
     */
    fun setInstallMode(mode: InstallMode) {
        _installMode.value = mode
        updateBinariesToInstall()
    }

    /**
     * Selecciona/deselecciona un binario para instalación personalizada
     */
    fun toggleBinarySelection(binaryName: String) {
        if (_installMode.value != InstallMode.CUSTOM) return
        
        val current = _binariesToInstall.value.toMutableList()
        val binary = BinaryCatalog.getAllBinaries(_deviceArch.value)
            .find { it.name == binaryName } ?: return
        
        if (current.contains(binary)) {
            current.remove(binary)
        } else {
            current.add(binary)
        }
        
        _binariesToInstall.value = current
    }

    /**
     * Inicia la descarga e instalación
     */
    fun startSetup() {
        viewModelScope.launch {
            _setupState.value = SetupState.INSTALLING
            
            val binaries = _binariesToInstall.value
            val source = _selectedSource.value
            
            var completedCount = 0
            val totalCount = binaries.size
            
            for (binary in binaries) {
                val result = downloadService.downloadAndInstall(binary, source)
                
                if (result.isSuccess) {
                    completedCount++
                    _overallProgress.value = completedCount.toFloat() / totalCount.toFloat()
                } else {
                    // Si falla un binario requerido, detener
                    if (binary.required) {
                        _setupState.value = SetupState.ERROR
                        return@launch
                    }
                }
            }
            
            _setupState.value = SetupState.COMPLETED
            _overallProgress.value = 1f
        }
    }

    /**
     * Reintentar instalación
     */
    fun retrySetup() {
        checkBinaries()
    }

    /**
     * Omitir setup (solo si no hay binarios requeridos faltantes)
     */
    fun skipSetup() {
        val states = binaryStates.value
        val missingRequired = states.values.any { 
            it.status == BinaryStatus.NOT_INSTALLED && it.info.required 
        }
        
        if (!missingRequired) {
            _setupState.value = SetupState.READY
        }
    }

    /**
     * Selecciona una fuente de descarga
     */
    fun selectSource(source: DownloadSource) {
        _selectedSource.value = source
    }

    /**
     * Agrega una fuente personalizada
     */
    fun addCustomSource(url: String, name: String) {
        val customSource = DownloadSource(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            description = "Fuente personalizada",
            baseUrl = url,
            priority = 50
        )
        
        val current = _availableSources.value.toMutableList()
        current.add(customSource)
        _availableSources.value = current
    }

    /**
     * Obtiene la arquitectura del dispositivo
     */
    private fun getDeviceArchitecture(): String {
        val supportedAbis = android.os.Build.SUPPORTED_ABIS
        return when {
            supportedAbis.contains("arm64-v8a") -> "arm64-v8a"
            supportedAbis.contains("armeabi-v7a") -> "armeabi-v7a"
            supportedAbis.contains("x86_64") -> "x86_64"
            supportedAbis.contains("x86") -> "x86"
            else -> "arm64-v8a" // Default
        }
    }

    /**
     * Guarda la configuración de que el setup fue completado
     */
    fun markSetupCompleted() {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("setup_completed", true).apply()
    }

    /**
     * Verifica si el setup ya fue completado
     */
    fun isSetupCompleted(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("setup_completed", false)
    }
}

/**
 * Estado del setup
 */
enum class SetupState {
    CHECKING,           // Verificando binarios
    NEEDS_SETUP,        // Necesita configuración
    INSTALLING,         // Instalando binarios
    COMPLETED,          // Completado
    ERROR,              // Error
    UPDATES_AVAILABLE,  // Actualizaciones disponibles
    READY               // Todo listo
}

/**
 * Modo de instalación
 */
enum class InstallMode {
    REQUIRED_ONLY,  // Solo binarios requeridos
    RECOMMENDED,    // Configuración recomendada
    ALL,            // Todos los binarios
    CUSTOM          // Selección personalizada
}
