package com.spotdl.android.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo de datos para una canción
 */
@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Int? = null, // Duración en segundos
    val artworkUrl: String? = null,
    val youtubeUrl: String? = null,
    val spotifyUrl: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val isrc: String? = null
) : Parcelable

/**
 * Estado de descarga
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Modelo de datos para el progreso de descarga
 */
data class DownloadProgress(
    val song: Song,
    val status: DownloadStatus,
    val progress: Float = 0f, // 0.0 a 1.0
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val currentStep: String = "",
    val errorMessage: String? = null,
    val filePath: String? = null
)

/**
 * Formato de audio
 */
enum class AudioFormat(val extension: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    M4A("m4a", "audio/mp4"),
    FLAC("flac", "audio/flac"),
    WAV("wav", "audio/wav"),
    OGG("ogg", "audio/ogg")
}

/**
 * Calidad de audio
 */
enum class AudioQuality(val bitrate: String) {
    LOW("128k"),
    MEDIUM("192k"),
    HIGH("256k"),
    VERY_HIGH("320k")
}

/**
 * Configuración de descarga
 */
data class DownloadConfig(
    val format: AudioFormat = AudioFormat.MP3,
    val quality: AudioQuality = AudioQuality.HIGH,
    val embedArtwork: Boolean = true,
    val embedMetadata: Boolean = true,
    val outputDirectory: String = "",
    val filenameTemplate: String = "{artist} - {title}"
)

/**
 * Resultado de búsqueda
 */
data class SearchResult(
    val query: String,
    val songs: List<Song>,
    val source: SearchSource
)

enum class SearchSource {
    SPOTIFY,
    YOUTUBE,
    LOCAL
}
