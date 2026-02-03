package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import com.spotdl.android.data.model.AudioFormat
import com.spotdl.android.data.model.AudioQuality
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Servicio para conversión de audio usando FFmpeg binario
 */
class FFmpegService(private val context: Context) {

    companion object {
        private const val TAG = "FFmpegService"
    }

    private val binaryManager = BinaryManager(context)

    /**
     * Inicializa FFmpeg
     */
    suspend fun initialize(): Result<Unit> {
        return binaryManager.initializeBinaries()
    }

    /**
     * Convierte un archivo de audio a otro formato
     */
    suspend fun convertAudio(
        inputFile: File,
        outputFile: File,
        format: AudioFormat,
        quality: AudioQuality,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!inputFile.exists()) {
                return@withContext Result.failure(Exception("El archivo de entrada no existe"))
            }

            Log.d(TAG, "Convirtiendo ${inputFile.name} a ${format.extension}")

            // Construir comando FFmpeg
            val command = buildConversionCommand(inputFile, outputFile, format, quality)
            
            Log.d(TAG, "Comando FFmpeg: $command")

            // Ejecutar FFmpeg
            val result = binaryManager.executeFFmpeg(command, onProgress)

            when {
                result.isSuccess -> {
                    Log.d(TAG, "Conversión exitosa")
                    Result.success(outputFile)
                }
                else -> {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    Log.e(TAG, "Error en conversión: $error")
                    Result.failure(Exception("Error en conversión: $error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Construye el comando de FFmpeg para la conversión
     */
    private fun buildConversionCommand(
        inputFile: File,
        outputFile: File,
        format: AudioFormat,
        quality: AudioQuality
    ): String {
        return buildString {
            append("-i \"${inputFile.absolutePath}\"")
            
            // Configuración según formato
            when (format) {
                AudioFormat.MP3 -> {
                    append(" -codec:a libmp3lame")
                    append(" -b:a ${quality.bitrate}")
                }
                AudioFormat.M4A -> {
                    append(" -codec:a aac")
                    append(" -b:a ${quality.bitrate}")
                }
                AudioFormat.FLAC -> {
                    append(" -codec:a flac")
                    append(" -compression_level 8")
                }
                AudioFormat.WAV -> {
                    append(" -codec:a pcm_s16le")
                }
                AudioFormat.OGG -> {
                    append(" -codec:a libvorbis")
                    append(" -q:a ${getOggQuality(quality)}")
                }
            }

            // Configuración de audio común
            append(" -ar 44100") // Sample rate
            append(" -ac 2") // Stereo

            // Sobrescribir sin preguntar
            append(" -y")

            // Output file
            append(" \"${outputFile.absolutePath}\"")
        }
    }

    /**
     * Convierte AudioQuality a valor de calidad para OGG
     */
    private fun getOggQuality(quality: AudioQuality): String {
        return when (quality) {
            AudioQuality.LOW -> "3"
            AudioQuality.MEDIUM -> "5"
            AudioQuality.HIGH -> "7"
            AudioQuality.VERY_HIGH -> "9"
        }
    }

    /**
     * Inserta metadatos en un archivo de audio
     */
    suspend fun embedMetadata(
        inputFile: File,
        outputFile: File,
        song: Song,
        artworkFile: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val command = buildString {
                append("-i \"${inputFile.absolutePath}\"")

                // Artwork si está disponible
                if (artworkFile != null && artworkFile.exists()) {
                    append(" -i \"${artworkFile.absolutePath}\"")
                }

                // Copiar streams
                append(" -c copy")

                // Metadatos
                append(" -metadata title=\"${song.title}\"")
                append(" -metadata artist=\"${song.artist}\"")
                
                song.album?.let {
                    append(" -metadata album=\"$it\"")
                }
                
                song.year?.let {
                    append(" -metadata date=\"$it\"")
                }
                
                song.genre?.let {
                    append(" -metadata genre=\"$it\"")
                }

                // Mapear artwork si existe
                if (artworkFile != null && artworkFile.exists()) {
                    append(" -map 0:0")
                    append(" -map 1:0")
                    append(" -id3v2_version 3")
                }

                // Sobrescribir
                append(" -y")

                // Output
                append(" \"${outputFile.absolutePath}\"")
            }

            Log.d(TAG, "Comando metadata: $command")

            val result = binaryManager.executeFFmpeg(command)

            when {
                result.isSuccess -> Result.success(outputFile)
                else -> {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    Result.failure(Exception("Error insertando metadatos: $error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error insertando metadatos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene información del archivo de audio
     */
    suspend fun getAudioInfo(file: File): AudioInfo? = withContext(Dispatchers.IO) {
        try {
            val command = "-i \"${file.absolutePath}\""
            val result = binaryManager.executeFFmpeg(command)
            
            val output = result.getOrNull() ?: ""
            parseAudioInfo(output)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo info de audio: ${e.message}", e)
            null
        }
    }

    /**
     * Parsea la información de audio del output de FFmpeg
     */
    private fun parseAudioInfo(output: String): AudioInfo? {
        try {
            var duration = 0
            var bitrate = ""
            var codec = ""
            var sampleRate = ""

            val lines = output.split("\n")
            for (line in lines) {
                when {
                    line.contains("Duration:") -> {
                        val durationPattern = Regex("Duration: (\\d{2}):(\\d{2}):(\\d{2})")
                        val match = durationPattern.find(line)
                        if (match != null) {
                            val hours = match.groupValues[1].toInt()
                            val minutes = match.groupValues[2].toInt()
                            val seconds = match.groupValues[3].toInt()
                            duration = hours * 3600 + minutes * 60 + seconds
                        }
                    }
                    line.contains("Audio:") -> {
                        val parts = line.split(",")
                        if (parts.isNotEmpty()) {
                            codec = parts[0].substringAfter("Audio:").trim()
                            
                            for (part in parts) {
                                when {
                                    part.contains("Hz") -> sampleRate = part.trim()
                                    part.contains("kb/s") -> bitrate = part.trim()
                                }
                            }
                        }
                    }
                }
            }

            return AudioInfo(duration, bitrate, codec, sampleRate)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Obtiene la versión de FFmpeg
     */
    suspend fun getVersion(): String? {
        return binaryManager.getFFmpegVersion()
    }
}

/**
 * Información de audio
 */
data class AudioInfo(
    val duration: Int,
    val bitrate: String,
    val codec: String,
    val sampleRate: String
)
