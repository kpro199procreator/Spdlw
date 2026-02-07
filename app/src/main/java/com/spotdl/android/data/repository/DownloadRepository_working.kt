package com.spotdl.android.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.spotdl.android.data.model.*
import com.spotdl.android.data.service.FFmpegService
import com.spotdl.android.data.service.SpotifyService
import com.spotdl.android.data.service.YouTubeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio con lógica REAL de descarga funcional
 */
class DownloadRepository(private val context: Context) {

    companion object {
        private const val TAG = "DownloadRepository"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val youtubeService = YouTubeService(context)
    private val spotifyService = SpotifyService()
    private val ffmpegService = FFmpegService(context)

    private val _downloads = MutableStateFlow<List<DownloadProgress>>(emptyList())
    val downloads: Flow<List<DownloadProgress>> = _downloads.asStateFlow()

    private val activeDownloads = mutableMapOf<String, DownloadProgress>()

    /**
     * Inicializar servicios
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            youtubeService.initialize()
            ffmpegService.initialize()
        }
    }

    /**
     * Buscar canciones (Spotify o YouTube)
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            if (query.startsWith("http") && query.contains("spotify")) {
                // Es URL de Spotify
                val song = spotifyService.getSongFromUrl(query)
                if (song != null) listOf(song) else emptyList()
            } else {
                // Buscar en Spotify
                spotifyService.searchSongs(query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtener canción desde URL de Spotify
     */
    suspend fun getSongFromSpotifyUrl(url: String): Song? {
        return spotifyService.getSongFromUrl(url)
    }

    /**
     * DESCARGA REAL de una canción
     * Flujo completo: Spotify → YouTube → FFmpeg → Archivo final
     */
    suspend fun downloadSong(
        song: Song,
        config: DownloadConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        val downloadId = UUID.randomUUID().toString()
        
        try {
            Log.d(TAG, "Iniciando descarga: ${song.title} - ${song.artist}")
            
            // 1. Crear progreso inicial
            val progress = DownloadProgress(
                downloadId = downloadId,
                song = song,
                status = DownloadStatus.PENDING,
                progress = 0f,
                currentStep = "Iniciando descarga..."
            )
            updateProgress(downloadId, progress)

            // 2. Buscar en YouTube
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0.1f,
                    currentStep = "Buscando en YouTube: ${song.artist} - ${song.title}"
                )
            )

            val youtubeUrl = if (song.youtubeUrl != null) {
                song.youtubeUrl
            } else {
                val searchQuery = "${song.artist} ${song.title}"
                val foundSong = youtubeService.searchSong(searchQuery)
                foundSong?.youtubeUrl ?: return@withContext Result.failure(
                    Exception("No se encontró la canción en YouTube")
                )
            }

            Log.d(TAG, "YouTube URL encontrada: $youtubeUrl")

            // 3. Descargar audio de YouTube
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.DOWNLOADING,
                    progress = 0.2f,
                    currentStep = "Descargando audio desde YouTube..."
                )
            )

            val tempDir = File(context.cacheDir, "downloads")
            tempDir.mkdirs()
            val tempAudioFile = File(tempDir, "${downloadId}_temp.webm")

            val downloadResult = youtubeService.downloadAudio(
                youtubeUrl = youtubeUrl,
                outputFile = tempAudioFile,
                onProgress = { downloadProgress ->
                    updateProgress(
                        downloadId,
                        progress.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = 0.2f + (downloadProgress * 0.4f), // 20%-60%
                            currentStep = "Descargando: ${(downloadProgress * 100).toInt()}%"
                        )
                    )
                }
            )

            if (downloadResult.isFailure) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Error descargando audio: ${downloadResult.exceptionOrNull()?.message}"
                    )
                )
                return@withContext Result.failure(
                    downloadResult.exceptionOrNull() ?: Exception("Error desconocido")
                )
            }

            val audioFile = downloadResult.getOrNull()!!
            Log.d(TAG, "Audio descargado: ${audioFile.absolutePath}")

            // 4. Convertir a formato deseado con FFmpeg
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.PROCESSING,
                    progress = 0.6f,
                    currentStep = "Convirtiendo a ${config.format}..."
                )
            )

            val outputDir = getOutputDirectory()
            val fileName = sanitizeFilename("${song.artist} - ${song.title}.${config.format}")
            val outputFile = File(outputDir, fileName)

            val conversionResult = ffmpegService.convertAudio(
                inputFile = audioFile,
                outputFile = outputFile,
                format = config.format,
                quality = config.quality,
                onProgress = { conversionProgress ->
                    updateProgress(
                        downloadId,
                        progress.copy(
                            status = DownloadStatus.PROCESSING,
                            progress = 0.6f + (conversionProgress * 0.3f), // 60%-90%
                            currentStep = "Convirtiendo: ${(conversionProgress * 100).toInt()}%"
                        )
                    )
                }
            )

            if (conversionResult.isFailure) {
                cleanup(audioFile, null, null)
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Error en conversión: ${conversionResult.exceptionOrNull()?.message}"
                    )
                )
                return@withContext Result.failure(
                    conversionResult.exceptionOrNull() ?: Exception("Error en conversión")
                )
            }

            val finalFile = conversionResult.getOrNull()!!
            Log.d(TAG, "Archivo convertido: ${finalFile.absolutePath}")

            // 5. Incrustar metadatos (opcional)
            if (config.embedMetadata) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.PROCESSING,
                        progress = 0.9f,
                        currentStep = "Agregando metadatos..."
                    )
                )

                embedMetadata(finalFile, song, config)
            }

            // 6. Limpiar archivos temporales
            cleanup(audioFile, null, null)

            // 7. Notificar al sistema de archivos
            MediaScannerConnection.scanFile(
                context,
                arrayOf(finalFile.absolutePath),
                null,
                null
            )

            // 8. Marcar como completado
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    currentStep = "Descarga completada",
                    outputFile = finalFile.absolutePath
                )
            )

            Log.d(TAG, "Descarga completada: ${finalFile.absolutePath}")
            Result.success(downloadId)

        } catch (e: Exception) {
            Log.e(TAG, "Error en descarga: ${e.message}", e)
            updateProgress(
                downloadId,
                DownloadProgress(
                    downloadId = downloadId,
                    song = song,
                    status = DownloadStatus.FAILED,
                    progress = 0f,
                    currentStep = "Error",
                    errorMessage = e.message ?: "Error desconocido"
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Cancelar descarga
     */
    suspend fun cancelDownload(downloadId: String) {
        updateProgress(
            downloadId,
            activeDownloads[downloadId]?.copy(
                status = DownloadStatus.CANCELLED,
                currentStep = "Cancelado por el usuario"
            ) ?: return
        )
    }

    /**
     * Actualizar progreso
     */
    private fun updateProgress(downloadId: String, progress: DownloadProgress) {
        activeDownloads[downloadId] = progress
        _downloads.value = activeDownloads.values.toList()
    }

    /**
     * Obtener directorio de salida
     */
    private fun getOutputDirectory(): File {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val outputDir = File(musicDir, "SpotDL")
        
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        return outputDir
    }

    /**
     * Sanitizar nombre de archivo
     */
    private fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Z0-9áéíóúñÁÉÍÓÚÑ ._-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Incrustar metadatos en el archivo de audio
     */
    private suspend fun embedMetadata(
        file: File,
        song: Song,
        config: DownloadConfig
    ) = withContext(Dispatchers.IO) {
        try {
            // Usar FFmpeg para incrustar metadatos
            ffmpegService.embedMetadata(
                file = file,
                title = song.title,
                artist = song.artist,
                album = song.album,
                year = song.year
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error incrustando metadatos: ${e.message}", e)
            // No fallar la descarga si los metadatos fallan
        }
    }

    /**
     * Limpiar archivos temporales
     */
    private fun cleanup(vararg files: File?) {
        files.forEach { file ->
            try {
                file?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error eliminando archivo temporal: ${e.message}")
            }
        }
    }

    /**
     * Obtener estadísticas de descargas
     */
    fun getDownloadStats(): DownloadStats {
        val downloads = _downloads.value
        return DownloadStats(
            total = downloads.size,
            completed = downloads.count { it.status == DownloadStatus.COMPLETED },
            failed = downloads.count { it.status == DownloadStatus.FAILED },
            downloading = downloads.count { 
                it.status == DownloadStatus.DOWNLOADING || 
                it.status == DownloadStatus.PROCESSING 
            },
            pending = downloads.count { it.status == DownloadStatus.PENDING }
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

enum class SearchSource {
    YOUTUBE,
    SPOTIFY,
    LOCAL
}
