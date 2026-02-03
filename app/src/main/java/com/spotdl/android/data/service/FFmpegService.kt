package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.spotdl.android.data.model.AudioFormat
import com.spotdl.android.data.model.AudioQuality
import com.spotdl.android.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Servicio para conversión de audio usando FFmpeg
 */
class FFmpegService(private val context: Context) {

    companion object {
        private const val TAG = "FFmpegService"
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

            // Configurar callback de progreso
            var lastProgress = 0f
            FFmpegKitConfig.enableStatisticsCallback { statistics ->
                // Calcular progreso basado en el tiempo procesado
                val progress = (statistics.time / 100000f).coerceIn(0f, 1f)
                if (progress > lastProgress) {
                    lastProgress = progress
                    onProgress(progress)
                }
            }

            // Ejecutar FFmpeg
            val session = FFmpegKit.execute(command)

            // Resetear callback
            FFmpegKitConfig.enableStatisticsCallback(null)

            val returnCode = session.returnCode

            when {
                ReturnCode.isSuccess(returnCode) -> {
                    Log.d(TAG, "Conversión exitosa")
                    Result.success(outputFile)
                }
                ReturnCode.isCancel(returnCode) -> {
                    Log.w(TAG, "Conversión cancelada")
                    Result.failure(Exception("Conversión cancelada"))
                }
                else -> {
                    val error = session.output ?: "Error desconocido"
                    Log.e(TAG, "Error en conversión: $error")
                    Result.failure(Exception("Error en conversión: código $returnCode"))
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
        val commands = mutableListOf<String>()
        
        // Input file
        commands.add("-i")
        commands.add(inputFile.absolutePath)

        // Configuración según formato
        when (format) {
            AudioFormat.MP3 -> {
                commands.add("-codec:a")
                commands.add("libmp3lame")
                commands.add("-b:a")
                commands.add(quality.bitrate)
            }
            AudioFormat.M4A -> {
                commands.add("-codec:a")
                commands.add("aac")
                commands.add("-b:a")
                commands.add(quality.bitrate)
            }
            AudioFormat.FLAC -> {
                commands.add("-codec:a")
                commands.add("flac")
                commands.add("-compression_level")
                commands.add("8")
            }
            AudioFormat.WAV -> {
                commands.add("-codec:a")
                commands.add("pcm_s16le")
            }
            AudioFormat.OGG -> {
                commands.add("-codec:a")
                commands.add("libvorbis")
                commands.add("-q:a")
                commands.add(getOggQuality(quality))
            }
        }

        // Configuración de audio común
        commands.add("-ar")
        commands.add("44100") // Sample rate
        commands.add("-ac")
        commands.add("2") // Stereo

        // Sobrescribir sin preguntar
        commands.add("-y")

        // Output file
        commands.add(outputFile.absolutePath)

        return commands.joinToString(" ")
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
     * Extrae metadatos de un archivo de audio
     */
    suspend fun extractMetadata(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>()
        
        try {
            val command = "-i ${file.absolutePath} -f ffmetadata -"
            val session = FFmpegKit.execute(command)
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                val output = session.output ?: ""
                // Parsear metadatos del output
                parseMetadataOutput(output, metadata)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo metadatos: ${e.message}", e)
        }
        
        metadata
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
            val commands = mutableListOf<String>()
            
            // Input file
            commands.add("-i")
            commands.add(inputFile.absolutePath)

            // Artwork si está disponible
            if (artworkFile != null && artworkFile.exists()) {
                commands.add("-i")
                commands.add(artworkFile.absolutePath)
            }

            // Copiar streams
            commands.add("-c")
            commands.add("copy")

            // Metadatos
            commands.add("-metadata")
            commands.add("title=${song.title}")
            commands.add("-metadata")
            commands.add("artist=${song.artist}")
            
            song.album?.let {
                commands.add("-metadata")
                commands.add("album=$it")
            }
            
            song.year?.let {
                commands.add("-metadata")
                commands.add("date=$it")
            }
            
            song.genre?.let {
                commands.add("-metadata")
                commands.add("genre=$it")
            }

            // Mapear artwork si existe
            if (artworkFile != null && artworkFile.exists()) {
                commands.add("-map")
                commands.add("0:0")
                commands.add("-map")
                commands.add("1:0")
                commands.add("-id3v2_version")
                commands.add("3")
            }

            // Sobrescribir
            commands.add("-y")

            // Output
            commands.add(outputFile.absolutePath)

            val command = commands.joinToString(" ")
            Log.d(TAG, "Comando metadata: $command")

            val session = FFmpegKit.execute(command)

            when {
                ReturnCode.isSuccess(session.returnCode) -> Result.success(outputFile)
                else -> {
                    val error = session.output ?: "Error desconocido"
                    Result.failure(Exception("Error insertando metadatos: $error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error insertando metadatos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parsea la salida de metadatos de FFmpeg
     */
    private fun parseMetadataOutput(output: String, metadata: MutableMap<String, String>) {
        val lines = output.split("\n")
        for (line in lines) {
            if (line.trim().startsWith("title") ||
                line.trim().startsWith("artist") ||
                line.trim().startsWith("album") ||
                line.trim().startsWith("date")
            ) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    metadata[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }

    /**
     * Obtiene información del archivo de audio
     */
    suspend fun getAudioInfo(file: File): AudioInfo? = withContext(Dispatchers.IO) {
        try {
            val command = "-i ${file.absolutePath}"
            val session = FFmpegKit.execute(command)
            
            val output = session.output ?: ""
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
