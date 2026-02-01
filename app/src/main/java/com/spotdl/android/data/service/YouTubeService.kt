package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Servicio para interactuar con YouTube
 */
class YouTubeService(private val context: Context) {

    companion object {
        private const val TAG = "YouTubeService"
        private const val YOUTUBE_SEARCH_URL = "https://www.youtube.com/results?search_query="
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    /**
     * Busca una canción en YouTube
     */
    suspend fun searchSong(query: String): Song? = withContext(Dispatchers.IO) {
        try {
            val searchQuery = URLEncoder.encode(query, "UTF-8")
            val url = YOUTUBE_SEARCH_URL + searchQuery
            
            Log.d(TAG, "Buscando: $query")
            
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()
            
            // Extraer datos del primer resultado de video
            val scriptTags = doc.select("script")
            
            for (script in scriptTags) {
                val scriptContent = script.html()
                
                if (scriptContent.contains("var ytInitialData")) {
                    val jsonStart = scriptContent.indexOf("{")
                    val jsonEnd = scriptContent.lastIndexOf("}") + 1
                    
                    if (jsonStart != -1 && jsonEnd > jsonStart) {
                        val jsonStr = scriptContent.substring(jsonStart, jsonEnd)
                        return@withContext parseYouTubeSearchResult(jsonStr, query)
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando canción: ${e.message}", e)
            null
        }
    }

    /**
     * Parsea el resultado de búsqueda de YouTube
     */
    private fun parseYouTubeSearchResult(jsonStr: String, originalQuery: String): Song? {
        try {
            val json = JSONObject(jsonStr)
            val contents = json
                .getJSONObject("contents")
                .getJSONObject("twoColumnSearchResultsRenderer")
                .getJSONObject("primaryContents")
                .getJSONObject("sectionListRenderer")
                .getJSONArray("contents")
                .getJSONObject(0)
                .getJSONObject("itemSectionRenderer")
                .getJSONArray("contents")

            for (i in 0 until contents.length()) {
                val item = contents.getJSONObject(i)
                
                if (item.has("videoRenderer")) {
                    val video = item.getJSONObject("videoRenderer")
                    val videoId = video.getString("videoId")
                    
                    val title = video.getJSONObject("title")
                        .getJSONArray("runs")
                        .getJSONObject(0)
                        .getString("text")
                    
                    val thumbnail = video.getJSONObject("thumbnail")
                        .getJSONArray("thumbnails")
                        .getJSONObject(0)
                        .getString("url")
                    
                    // Extraer artista y título del título del video
                    val (artist, songTitle) = parseTitle(title)
                    
                    val lengthText = if (video.has("lengthText")) {
                        video.getJSONObject("lengthText").getString("simpleText")
                    } else null
                    
                    val duration = lengthText?.let { parseDuration(it) }
                    
                    return Song(
                        id = videoId,
                        title = songTitle,
                        artist = artist,
                        artworkUrl = thumbnail.replace("hqdefault", "maxresdefault"),
                        youtubeUrl = "https://www.youtube.com/watch?v=$videoId",
                        duration = duration
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando resultado: ${e.message}", e)
        }
        
        return null
    }

    /**
     * Parsea el título del video para extraer artista y título
     */
    private fun parseTitle(fullTitle: String): Pair<String, String> {
        // Patrones comunes: "Artist - Title" o "Artist: Title"
        val separators = listOf(" - ", ": ", " – ")
        
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
     * Convierte duración "MM:SS" a segundos
     */
    private fun parseDuration(duration: String): Int {
        return try {
            val parts = duration.split(":")
            when (parts.size) {
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Descarga el audio de YouTube
     */
    suspend fun downloadAudio(
        youtubeUrl: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Descargando audio de: $youtubeUrl")
            
            // Aquí usarías youtube-dl o yt-dlp en un entorno real
            // Para esta implementación, simulamos la descarga
            // En producción, integrarías con un extractor real
            
            val videoId = extractVideoId(youtubeUrl) ?: return@withContext Result.failure(
                Exception("No se pudo extraer el ID del video")
            )
            
            // Obtener URL de descarga usando youtube-extractor o API similar
            val downloadUrl = getDownloadUrl(videoId) ?: return@withContext Result.failure(
                Exception("No se pudo obtener URL de descarga")
            )
            
            // Descargar el archivo
            downloadFile(downloadUrl, outputFile, onProgress)
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extrae el ID del video de una URL de YouTube
     */
    private fun extractVideoId(url: String): String? {
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
     * Obtiene la URL de descarga del audio
     * Nota: Implementación simplificada. En producción usar youtube-dl/yt-dlp
     */
    private suspend fun getDownloadUrl(videoId: String): String? {
        // Aquí integrarías con youtube-extractor o similar
        // Por ahora retornamos null para indicar que se necesita implementación real
        return null
    }

    /**
     * Descarga un archivo desde una URL
     */
    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connect()

        val totalBytes = connection.contentLength.toLong()
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    if (totalBytes > 0) {
                        val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                        onProgress(progress)
                    }
                }
            }
        }
    }
}
