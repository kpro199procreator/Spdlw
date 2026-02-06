package com.spotdl.android.data.service

import android.util.Base64
import android.util.Log
import com.spotdl.android.data.api.*
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Servicio mejorado de Spotify usando API oficial
 * Requiere Client ID y Client Secret de Spotify Developer Dashboard
 */
class SpotifyService {

    companion object {
        private const val TAG = "SpotifyService"
        private const val BASE_URL = "https://api.spotify.com/v1/"
        private const val ACCOUNTS_URL = "https://accounts.spotify.com/"
        
        // IMPORTANTE: En producción, usa variables de entorno o almacenamiento seguro
        // Obtén tus credenciales en: https://developer.spotify.com/dashboard
        private const val CLIENT_ID = "YOUR_CLIENT_ID_HERE" 
        private const val CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE"
    }

    private val api: SpotifyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApi::class.java)
    }

    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0

    /**
     * Obtener o renovar token de acceso
     */
    private suspend fun getValidToken(): String? = withContext(Dispatchers.IO) {
        try {
            // Si el token aún es válido, retornarlo
            if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
                return@withContext accessToken
            }

            // Generar nuevo token
            val credentials = "$CLIENT_ID:$CLIENT_SECRET"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val response = api.getAccessToken(
                authorization = "Basic $encodedCredentials"
            )

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                accessToken = tokenResponse?.accessToken
                tokenExpirationTime = System.currentTimeMillis() + 
                    TimeUnit.SECONDS.toMillis(tokenResponse?.expiresIn?.toLong() ?: 3600)
                
                Log.d(TAG, "Token obtenido exitosamente")
                accessToken
            } else {
                Log.e(TAG, "Error obteniendo token: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getValidToken: ${e.message}", e)
            null
        }
    }

    /**
     * Buscar canciones en Spotify
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val token = getValidToken() ?: return@withContext emptyList()

            val response = api.searchTracks(
                authorization = "Bearer $token",
                query = query,
                limit = 20
            )

            if (response.isSuccessful) {
                val tracks = response.body()?.tracks?.items ?: emptyList()
                tracks.map { it.toSong() }
            } else {
                Log.e(TAG, "Error en búsqueda: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando canciones: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtener información de canción por URL de Spotify
     */
    suspend fun getSongFromUrl(url: String): Song? = withContext(Dispatchers.IO) {
        try {
            val trackId = extractTrackId(url) ?: return@withContext null
            val token = getValidToken() ?: return@withContext null

            val response = api.getTrack(
                authorization = "Bearer $token",
                trackId = trackId
            )

            if (response.isSuccessful) {
                response.body()?.toSong()
            } else {
                Log.e(TAG, "Error obteniendo track: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getSongFromUrl: ${e.message}", e)
            null
        }
    }

    /**
     * Obtener tracks de una playlist
     */
    suspend fun getPlaylistTracks(playlistUrl: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractPlaylistId(playlistUrl) ?: return@withContext emptyList()
            val token = getValidToken() ?: return@withContext emptyList()

            val response = api.getPlaylistTracks(
                authorization = "Bearer $token",
                playlistId = playlistId
            )

            if (response.isSuccessful) {
                val items = response.body()?.items ?: emptyList()
                items.map { it.track.toSong() }
            } else {
                Log.e(TAG, "Error obteniendo playlist: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getPlaylistTracks: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtener tracks de un álbum
     */
    suspend fun getAlbumTracks(albumUrl: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val albumId = extractAlbumId(albumUrl) ?: return@withContext emptyList()
            val token = getValidToken() ?: return@withContext emptyList()

            val response = api.getAlbum(
                authorization = "Bearer $token",
                albumId = albumId
            )

            if (response.isSuccessful) {
                val album = response.body()
                // Nota: Album tracks requiere endpoint diferente
                // Por ahora retornamos info del álbum
                emptyList<Song>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getAlbumTracks: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Extraer ID de track de URL de Spotify
     */
    private fun extractTrackId(url: String): String? {
        val patterns = listOf(
            "track/([a-zA-Z0-9]+)",
            "spotify:track:([a-zA-Z0-9]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    /**
     * Extraer ID de playlist de URL
     */
    private fun extractPlaylistId(url: String): String? {
        val patterns = listOf(
            "playlist/([a-zA-Z0-9]+)",
            "spotify:playlist:([a-zA-Z0-9]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    /**
     * Extraer ID de álbum de URL
     */
    private fun extractAlbumId(url: String): String? {
        val patterns = listOf(
            "album/([a-zA-Z0-9]+)",
            "spotify:album:([a-zA-Z0-9]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    /**
     * Convertir SpotifyTrack a Song
     */
    private fun SpotifyTrack.toSong(): Song {
        return Song(
            id = this.id,
            title = this.name,
            artist = this.artists.joinToString(", ") { it.name },
            album = this.album.name,
            artworkUrl = this.album.images.firstOrNull()?.url,
            spotifyUrl = this.externalUrls.spotify,
            duration = this.durationMs / 1000,
            year = this.album.releaseDate.take(4),
            genre = null // Requiere endpoint adicional
        )
    }
}
