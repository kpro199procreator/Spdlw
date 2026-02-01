package com.spotdl.android.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.spotdl.android.data.model.*
import com.spotdl.android.data.service.FFmpegService
import com.spotdl.android.data.service.SpotifyService
import com.spotdl.android.data.service.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repositorio principal para gestionar descargas
 */
class DownloadRepository(private val context: Context) {

    companion object {
        private const val TAG = "DownloadRepository"
    }

    private val youtubeService = YouTubeService(context)
    private val spotifyService = SpotifyService()
    private val ffmpegService = FFmpegService(context)

    private val _downloads = MutableStateFlow<List<DownloadProgress>>(emptyList())
    val downloads: Flow<List<DownloadProgress>> = _downloads.asStateFlow()

    private val activeDownloads = mutableMapOf<String, DownloadProgress>()

    /**
     * Busca una canción
     */
    suspend fun searchSong(query: String, source: SearchSource = SearchSource.YOUTUBE): Song? {
        return when (source) {
            SearchSource.YOUTUBE -> youtubeService.searchSong(query)
            SearchSource.SPOTIFY -> spotifyService.searchSongs(query).firstOrNull()
            SearchSource.LOCAL -> null
        }
    }

    /**
     * Obtiene información de una canción desde una URL de Spotify
     */
    suspend fun getSongFromSpotifyUrl(url: String): Song? {
        return spotifyService.getSongFromUrl(url)
    }

    /**
     * Descarga una canción
     */
    suspend fun downloadSong(
        song: Song,
        config: DownloadConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val downloadId = song.id
            
            // Crear progreso inicial
            val progress = DownloadProgress(
                song = song,
                status = DownloadStatus.PENDING,
                currentStep = "Iniciando descarga..."
            )
            updateProgress(downloadId, progress)

            // Paso 1: Buscar en YouTube si no tenemos URL
            val youtubeUrl = song.youtubeUrl ?: run {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.DOWNLOADING,
                        currentStep = "Buscando en YouTube..."
                    )
                )
                
                val query = "${song.artist} ${song.title}"
                val foundSong = youtubeService.searchSong(query)
                foundSong?.youtubeUrl ?: return@withContext Result.failure(
                    Exception("No se encontró la canción en YouTube")
                )
            }

            // Paso 2: Descargar audio
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.DOWNLOADING,
                    currentStep = "Descargando audio..."
                )
            )

            val tempDir = File(context.cacheDir, "downloads")
            tempDir.mkdirs()

            val tempAudioFile = File(tempDir, "${song.id}_temp.webm")
            
            val downloadResult = youtubeService.downloadAudio(
                youtubeUrl = youtubeUrl,
                outputFile = tempAudioFile,
                onProgress = { downloadProgress ->
                    updateProgress(
                        downloadId,
                        progress.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progress = downloadProgress * 0.5f, // 50% del progreso total
                            currentStep = "Descargando audio... ${(downloadProgress * 100).toInt()}%"
                        )
                    )
                }
            )

            if (downloadResult.isFailure) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = downloadResult.exceptionOrNull()?.message
                    )
                )
                return@withContext downloadResult
            }

            // Paso 3: Descargar artwork si está disponible
            var artworkFile: File? = null
            if (config.embedArtwork && song.artworkUrl != null) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.PROCESSING,
                        progress = 0.5f,
                        currentStep = "Descargando portada..."
                    )
                )

                artworkFile = File(tempDir, "${song.id}_artwork.jpg")
                spotifyService.downloadArtwork(song.artworkUrl, artworkFile)
            }

            // Paso 4: Convertir a formato deseado
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.PROCESSING,
                    progress = 0.6f,
                    currentStep = "Convirtiendo audio..."
                )
            )

            val outputDir = getOutputDirectory(config.outputDirectory)
            outputDir.mkdirs()

            val filename = generateFilename(song, config.filenameTemplate, config.format)
            val outputFile = File(outputDir, filename)

            val convertedFile = File(tempDir, "${song.id}_converted.${config.format.extension}")
            
            val conversionResult = ffmpegService.convertAudio(
                inputFile = tempAudioFile,
                outputFile = convertedFile,
                format = config.format,
                quality = config.quality,
                onProgress = { conversionProgress ->
                    updateProgress(
                        downloadId,
                        progress.copy(
                            status = DownloadStatus.PROCESSING,
                            progress = 0.6f + (conversionProgress * 0.2f),
                            currentStep = "Convirtiendo audio... ${(conversionProgress * 100).toInt()}%"
                        )
                    )
                }
            )

            if (conversionResult.isFailure) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = conversionResult.exceptionOrNull()?.message
                    )
                )
                cleanup(tempAudioFile, artworkFile, convertedFile)
                return@withContext conversionResult
            }

            // Paso 5: Insertar metadatos
            if (config.embedMetadata) {
                updateProgress(
                    downloadId,
                    progress.copy(
                        status = DownloadStatus.PROCESSING,
                        progress = 0.8f,
                        currentStep = "Insertando metadatos..."
                    )
                )

                val metadataResult = ffmpegService.embedMetadata(
                    inputFile = convertedFile,
                    outputFile = outputFile,
                    song = song,
                    artworkFile = artworkFile
                )

                if (metadataResult.isFailure) {
                    // Si falla, al menos copiar el archivo sin metadatos
                    convertedFile.copyTo(outputFile, overwrite = true)
                }
            } else {
                convertedFile.copyTo(outputFile, overwrite = true)
            }

            // Paso 6: Registrar en MediaStore
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.PROCESSING,
                    progress = 0.95f,
                    currentStep = "Finalizando..."
                )
            )

            scanMediaFile(outputFile)

            // Limpiar archivos temporales
            cleanup(tempAudioFile, artworkFile, convertedFile)

            // Marcar como completado
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1f,
                    currentStep = "Descarga completada",
                    filePath = outputFile.absolutePath
                )
            )

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando canción: ${e.message}", e)
            
            updateProgress(
                song.id,
                DownloadProgress(
                    song = song,
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message
                )
            )
            
            Result.failure(e)
        }
    }

    /**
     * Actualiza el progreso de una descarga
     */
    private fun updateProgress(downloadId: String, progress: DownloadProgress) {
        activeDownloads[downloadId] = progress
        _downloads.value = activeDownloads.values.toList()
    }

    /**
     * Obtiene el directorio de salida
     */
    private fun getOutputDirectory(customPath: String): File {
        return if (customPath.isNotEmpty()) {
            File(customPath)
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "SpotDL"
            )
        }
    }

    /**
     * Genera el nombre del archivo
     */
    private fun generateFilename(song: Song, template: String, format: AudioFormat): String {
        var filename = template
            .replace("{artist}", sanitizeFilename(song.artist))
            .replace("{title}", sanitizeFilename(song.title))
            .replace("{album}", sanitizeFilename(song.album ?: "Unknown"))
            .replace("{year}", song.year ?: "")
        
        // Agregar timestamp si el archivo ya existe
        filename += ".${format.extension}"
        
        return filename
    }

    /**
     * Sanitiza el nombre del archivo
     */
    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    /**
     * Escanea el archivo para que aparezca en la galería de música
     */
    private suspend fun scanMediaFile(file: File) = withContext(Dispatchers.Main) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("audio/*"),
            null
        )
    }

    /**
     * Limpia archivos temporales
     */
    private fun cleanup(vararg files: File?) {
        files.forEach { file ->
            try {
                file?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo eliminar archivo temporal: ${file?.name}")
            }
        }
    }

    /**
     * Cancela una descarga
     */
    fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.let { progress ->
            updateProgress(
                downloadId,
                progress.copy(
                    status = DownloadStatus.CANCELLED,
                    currentStep = "Cancelado"
                )
            )
        }
    }

    /**
     * Limpia la lista de descargas completadas
     */
    fun clearCompleted() {
        val active = activeDownloads.filterValues { 
            it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED
        }
        activeDownloads.clear()
        activeDownloads.putAll(active)
        _downloads.value = activeDownloads.values.toList()
    }
}
