package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Servicio para interactuar con YouTube usando yt-dlp
 */
class YouTubeService(private val context: Context) {

    companion object {
        private const val TAG = "YouTubeService"
    }

    private val binaryManager = BinaryManager(context)

    /**
     * Inicializa yt-dlp
     */
    suspend fun initialize(): Result<Unit> {
        return binaryManager.initializeBinaries()
    }

    /**
     * Busca una canción en YouTube y obtiene información
     */
    suspend fun searchSong(query: String): Song? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando: $query")
            
            // Usar yt-dlp para buscar y obtener info
            val searchCommand = buildString {
                append("ytsearch1:\"$query\"")
                append(" --dump-json")
                append(" --no-playlist")
                append(" --skip-download")
            }
            
            val result = binaryManager.executeYtDlp(
                url = searchCommand,
                outputPath = "",
                format = "bestaudio"
            )
            
            if (result.isSuccess) {
                val jsonOutput = result.getOrNull() ?: return@withContext null
                parseYouTubeSongInfo(jsonOutput)
            } else {
                Log.e(TAG, "Error en búsqueda: ${result.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando canción: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene información de un video de YouTube
     */
    suspend fun getVideoInfo(url: String): Song? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo info de: $url")
            
            val command = buildString {
                append("\"$url\"")
                append(" --dump-json")
                append(" --no-playlist")
                append(" --skip-download")
            }
            
            val result = binaryManager.executeYtDlp(
                url = command,
                outputPath = "",
                format = "bestaudio"
            )
            
            if (result.isSuccess) {
                val jsonOutput = result.getOrNull() ?: return@withContext null
                parseYouTubeSongInfo(jsonOutput)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo info: ${e.message}", e)
            null
        }
    }

    /**
     * Parsea la información de yt-dlp JSON
     */
    private fun parseYouTubeSongInfo(jsonOutput: String): Song? {
        return try {
            // yt-dlp puede retornar múltiples líneas JSON, tomamos la primera válida
            val jsonLine = jsonOutput.lines().firstOrNull { it.trim().startsWith("{") }
                ?: return null
            
            val json = JSONObject(jsonLine)
            
            val videoId = json.optString("id", "")
            val title = json.optString("title", "Unknown")
            val duration = json.optInt("duration", 0)
            val thumbnail = json.optString("thumbnail", "")
            
            // Intentar extraer artista y título del título del video
            val (artist, songTitle) = parseTitle(title)
            
            Song(
                id = videoId,
                title = songTitle,
                artist = artist,
                artworkUrl = thumbnail,
                youtubeUrl = "https://www.youtube.com/watch?v=$videoId",
                duration = duration
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando JSON: ${e.message}", e)
            null
        }
    }

    /**
     * Parsea el título del video para extraer artista y título
     */
    private fun parseTitle(fullTitle: String): Pair<String, String> {
        // Patrones comunes: "Artist - Title" o "Artist: Title"
        val separators = listOf(" - ", ": ", " – ", " | ")
        
        for (separator in separators) {
            if (fullTitle.contains(separator)) {
                val parts = fullTitle.split(separator, limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }
        
        // Si no hay separador, asumir que todo es el título
        return Pair("Unknown Artist", fullTitle.trim())
    }

    /**
     * Descarga el audio de YouTube usando yt-dlp
     */
    suspend fun downloadAudio(
        youtubeUrl: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Descargando audio de: $youtubeUrl")
            
            // yt-dlp descarga directamente a la ubicación especificada
            val outputPath = outputFile.absolutePath.removeSuffix(".${outputFile.extension}")
            
            val result = binaryManager.executeYtDlp(
                url = youtubeUrl,
                outputPath = outputPath,
                format = "bestaudio",
                onProgress = onProgress
            )
            
            if (result.isSuccess) {
                // yt-dlp agrega la extensión automáticamente
                // Buscar el archivo descargado
                val downloadedFile = findDownloadedFile(outputPath)
                if (downloadedFile != null && downloadedFile.exists()) {
                    // Si el archivo no está en la ubicación esperada, moverlo
                    if (downloadedFile != outputFile) {
                        downloadedFile.copyTo(outputFile, overwrite = true)
                        downloadedFile.delete()
                    }
                    Log.d(TAG, "Descarga exitosa: ${outputFile.absolutePath}")
                    Result.success(outputFile)
                } else {
                    Result.failure(Exception("Archivo descargado no encontrado"))
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                Log.e(TAG, "Error en descarga: $error")
                Result.failure(Exception("Error descargando: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca el archivo descargado por yt-dlp
     */
    private fun findDownloadedFile(basePath: String): File? {
        // yt-dlp puede agregar diferentes extensiones
        val possibleExtensions = listOf(".webm", ".m4a", ".opus", ".mp3", ".ogg")
        
        for (ext in possibleExtensions) {
            val file = File("$basePath$ext")
            if (file.exists()) {
                return file
            }
        }
        
        return null
    }

    /**
     * Extrae el ID del video de una URL de YouTube
     */
    fun extractVideoId(url: String): String? {
        val patterns = listOf(
            "(?<=watch\\?v=)[^&]+",
            "(?<=youtu.be/)[^?]+",
            "(?<=embed/)[^?]+"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(url)
            if (match != null) {
                return match.value
            }
        }
        
        return null
    }

    /**
     * Obtiene la versión de yt-dlp
     */
    suspend fun getVersion(): String? {
        return binaryManager.getYtDlpVersion()
    }
}
