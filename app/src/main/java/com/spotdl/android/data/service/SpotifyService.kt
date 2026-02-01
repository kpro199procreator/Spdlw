package com.spotdl.android.data.service

import android.util.Log
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * Servicio para interactuar con Spotify (sin API oficial)
 * Usa web scraping para obtener información básica
 */
class SpotifyService {

    companion object {
        private const val TAG = "SpotifyService"
        private const val SPOTIFY_BASE_URL = "https://open.spotify.com"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    /**
     * Obtiene información de una canción de Spotify mediante su URL
     */
    suspend fun getSongFromUrl(spotifyUrl: String): Song? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo información de: $spotifyUrl")
            
            val doc = Jsoup.connect(spotifyUrl)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            // Extraer metadatos de las meta tags
            val title = doc.select("meta[property=og:title]").attr("content")
            val description = doc.select("meta[property=og:description]").attr("content")
            val image = doc.select("meta[property=og:image]").attr("content")
            
            // Parsear descripción para obtener artista
            // Formato típico: "Artist · Song"
            val artist = description.split("·").firstOrNull()?.trim() ?: "Unknown Artist"
            
            // Extraer ID de Spotify
            val trackId = extractTrackId(spotifyUrl)
            
            if (title.isNotEmpty()) {
                return@withContext Song(
                    id = trackId ?: "",
                    title = title,
                    artist = artist,
                    artworkUrl = image,
                    spotifyUrl = spotifyUrl
                )
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo canción de Spotify: ${e.message}", e)
            null
        }
    }

    /**
     * Busca canciones en Spotify
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando: $query")
            
            // Nota: Sin API oficial, la búsqueda es limitada
            // En un entorno de producción, usar la API oficial de Spotify
            
            val songs = mutableListOf<Song>()
            
            // Por ahora, retornamos lista vacía
            // Implementación completa requeriría API key de Spotify
            
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando en Spotify: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Extrae el ID de track de una URL de Spotify
     */
    private fun extractTrackId(url: String): String? {
        val pattern = Regex("track/([a-zA-Z0-9]+)")
        val match = pattern.find(url)
        return match?.groupValues?.get(1)
    }

    /**
     * Obtiene metadatos detallados de una canción
     */
    suspend fun getDetailedMetadata(trackId: String): SongMetadata? = withContext(Dispatchers.IO) {
        try {
            val url = "$SPOTIFY_BASE_URL/track/$trackId"
            
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get()

            // Extraer scripts que contienen datos JSON
            val scripts = doc.select("script[type=application/ld+json]")
            
            for (script in scripts) {
                try {
                    val json = JSONObject(script.html())
                    
                    if (json.has("@type") && json.getString("@type") == "MusicRecording") {
                        val name = json.optString("name", "")
                        val duration = json.optString("duration", "")
                        
                        val byArtist = json.optJSONObject("byArtist")
                        val artist = byArtist?.optString("name", "") ?: ""
                        
                        val inAlbum = json.optJSONObject("inAlbum")
                        val album = inAlbum?.optString("name", "")
                        
                        return@withContext SongMetadata(
                            title = name,
                            artist = artist,
                            album = album,
                            duration = parseDuration(duration)
                        )
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo metadatos: ${e.message}", e)
            null
        }
    }

    /**
     * Parsea duración ISO 8601 (PT3M45S) a segundos
     */
    private fun parseDuration(duration: String): Int {
        return try {
            var seconds = 0
            
            // Formato: PT3M45S
            val minutesPattern = Regex("(\\d+)M")
            val secondsPattern = Regex("(\\d+)S")
            
            minutesPattern.find(duration)?.let {
                seconds += it.groupValues[1].toInt() * 60
            }
            
            secondsPattern.find(duration)?.let {
                seconds += it.groupValues[1].toInt()
            }
            
            seconds
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Descarga la portada del álbum
     */
    suspend fun downloadArtwork(
        artworkUrl: String,
        outputFile: java.io.File
    ): Result<java.io.File> = withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(artworkUrl).openConnection()
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connect()

            connection.getInputStream().use { input ->
                java.io.FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando artwork: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Metadatos detallados de una canción
 */
data class SongMetadata(
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Int?,
    val year: String? = null,
    val genre: String? = null,
    val isrc: String? = null
)
