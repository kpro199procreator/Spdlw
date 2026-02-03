package com.spotdl.android.data.model

/**
 * Información de un binario descargable
 */
data class BinaryInfo(
    val name: String,
    val displayName: String,
    val description: String,
    val version: String,
    val required: Boolean,
    val downloadUrl: String,
    val alternativeUrls: List<String> = emptyList(),
    val checksum: String? = null,
    val checksumAlgorithm: String = "SHA256",
    val size: Long,
    val architecture: String = "arm64-v8a"
)

/**
 * Estado de un binario
 */
data class BinaryState(
    val info: BinaryInfo,
    val status: BinaryStatus,
    val installedVersion: String? = null,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Estados posibles de un binario
 */
enum class BinaryStatus {
    NOT_INSTALLED,      // No está instalado
    DOWNLOADING,        // Descargando
    EXTRACTING,         // Extrayendo/Instalando
    VERIFYING,          // Verificando integridad
    INSTALLED,          // Instalado correctamente
    UPDATE_AVAILABLE,   // Actualización disponible
    FAILED,             // Error
    OUTDATED            // Versión antigua
}

/**
 * Configuración de fuentes de descarga
 */
data class DownloadSource(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val priority: Int = 0,
    val enabled: Boolean = true
)

/**
 * Repositorios predefinidos
 */
object BinaryRepositories {
    
    val OFFICIAL_GITHUB = DownloadSource(
        id = "github_official",
        name = "GitHub Official",
        description = "Repositorios oficiales de GitHub",
        baseUrl = "https://github.com",
        priority = 100
    )
    
    val JSDELIVR_CDN = DownloadSource(
        id = "jsdelivr",
        name = "jsDelivr CDN",
        description = "CDN rápido y confiable",
        baseUrl = "https://cdn.jsdelivr.net/gh",
        priority = 90
    )
    
    val GITHUB_PROXY = DownloadSource(
        id = "ghproxy",
        name = "GitHub Proxy",
        description = "Proxy para GitHub (mejor en China)",
        baseUrl = "https://ghproxy.com",
        priority = 80
    )
    
    val CUSTOM = DownloadSource(
        id = "custom",
        name = "Custom URL",
        description = "URL personalizada",
        baseUrl = "",
        priority = 0,
        enabled = false
    )
    
    fun getDefaultSources(): List<DownloadSource> {
        return listOf(OFFICIAL_GITHUB, JSDELIVR_CDN, GITHUB_PROXY)
    }
}

/**
 * Catálogo de binarios disponibles
 */
object BinaryCatalog {
    
    fun getFFmpegInfo(arch: String = "arm64-v8a"): BinaryInfo {
        return BinaryInfo(
            name = "ffmpeg",
            displayName = "FFmpeg",
            description = "Herramienta de conversión de audio/video",
            version = "6.0",
            required = true,
            downloadUrl = "https://github.com/arthenica/ffmpeg-kit/releases/download/v6.0/ffmpeg-kit-full-6.0-$arch.aar",
            alternativeUrls = listOf(
                "https://cdn.jsdelivr.net/gh/arthenica/ffmpeg-kit@v6.0/prebuilt/$arch/ffmpeg",
                "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-arm64-static.tar.xz"
            ),
            checksum = null, // Se calculará después de descargar
            size = 45_000_000, // ~45 MB
            architecture = arch
        )
    }
    
    fun getYtDlpInfo(): BinaryInfo {
        return BinaryInfo(
            name = "yt-dlp",
            displayName = "yt-dlp",
            description = "Descargador de videos de YouTube y otros sitios",
            version = "2024.01.01",
            required = true,
            downloadUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp",
            alternativeUrls = listOf(
                "https://cdn.jsdelivr.net/gh/yt-dlp/yt-dlp@master/yt-dlp",
                "https://ghproxy.com/https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
            ),
            checksum = null,
            size = 3_500_000, // ~3.5 MB
            architecture = "all"
        )
    }
    
    fun getPythonInfo(arch: String = "arm64-v8a"): BinaryInfo {
        return BinaryInfo(
            name = "python3.11",
            displayName = "Python 3.11",
            description = "Intérprete Python (necesario para yt-dlp)",
            version = "3.11.7",
            required = false, // Opcional si se usa yt-dlp compilado
            downloadUrl = "https://www.python.org/ftp/python/3.11.7/Python-3.11.7.tar.xz",
            alternativeUrls = listOf(
                "https://github.com/termux/termux-packages/releases/download/python/python_3.11.7_$arch.deb"
            ),
            checksum = null,
            size = 18_000_000, // ~18 MB
            architecture = arch
        )
    }
    
    fun getYtDlpAndroidInfo(arch: String = "arm64-v8a"): BinaryInfo {
        return BinaryInfo(
            name = "yt-dlp-android",
            displayName = "yt-dlp Android (Compilado)",
            description = "Versión compilada de yt-dlp (no requiere Python)",
            version = "2024.01.01",
            required = false, // Alternativa a yt-dlp + Python
            downloadUrl = "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp_android_$arch",
            alternativeUrls = emptyList(),
            checksum = null,
            size = 12_000_000, // ~12 MB
            architecture = arch
        )
    }
    
    fun getAllBinaries(arch: String = "arm64-v8a"): List<BinaryInfo> {
        return listOf(
            getFFmpegInfo(arch),
            getYtDlpInfo(),
            // Usuario puede elegir entre Python + yt-dlp O yt-dlp compilado
            getPythonInfo(arch),
            getYtDlpAndroidInfo(arch)
        )
    }
    
    fun getRequiredBinaries(arch: String = "arm64-v8a"): List<BinaryInfo> {
        return getAllBinaries(arch).filter { it.required }
    }
}
