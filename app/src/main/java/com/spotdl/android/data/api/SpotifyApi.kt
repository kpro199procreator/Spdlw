package com.spotdl.android.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

/**
 * API de Spotify Web API
 * Documentación: https://developer.spotify.com/documentation/web-api
 */
interface SpotifyApi {
    
    /**
     * Obtener token de acceso (Client Credentials Flow)
     */
    @FormUrlEncoded
    @POST("https://accounts.spotify.com/api/token")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<SpotifyTokenResponse>
    
    /**
     * Buscar tracks
     */
    @GET("search")
    suspend fun searchTracks(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10
    ): Response<SpotifySearchResponse>
    
    /**
     * Obtener información de un track
     */
    @GET("tracks/{id}")
    suspend fun getTrack(
        @Header("Authorization") authorization: String,
        @Path("id") trackId: String
    ): Response<SpotifyTrack>
    
    /**
     * Obtener información de un álbum
     */
    @GET("albums/{id}")
    suspend fun getAlbum(
        @Header("Authorization") authorization: String,
        @Path("id") albumId: String
    ): Response<SpotifyAlbum>
    
    /**
     * Obtener tracks de una playlist
     */
    @GET("playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Header("Authorization") authorization: String,
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 50
    ): Response<SpotifyPlaylistTracksResponse>
}

// Response models
data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class SpotifySearchResponse(
    val tracks: SpotifyTracksPaging
)

data class SpotifyTracksPaging(
    val items: List<SpotifyTrack>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    @SerializedName("duration_ms") val durationMs: Int,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("external_urls") val externalUrls: SpotifyExternalUrls,
    val popularity: Int,
    @SerializedName("explicit") val isExplicit: Boolean
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    @SerializedName("external_urls") val externalUrls: SpotifyExternalUrls
)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("total_tracks") val totalTracks: Int,
    val artists: List<SpotifyArtist>? = null
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class SpotifyExternalUrls(
    val spotify: String
)

data class SpotifyPlaylistTracksResponse(
    val items: List<SpotifyPlaylistTrackItem>,
    val total: Int
)

data class SpotifyPlaylistTrackItem(
    val track: SpotifyTrack,
    @SerializedName("added_at") val addedAt: String
)
