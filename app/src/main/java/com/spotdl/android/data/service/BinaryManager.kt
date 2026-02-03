package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Servicio para gestionar binarios de FFmpeg y yt-dlp
 */
class BinaryManager(private val context: Context) {

    companion object {
        private const val TAG = "BinaryManager"
        private const val FFMPEG_ASSET = "ffmpeg"
        private const val YTDLP_ASSET = "yt-dlp"
        private const val PYTHON_ASSET = "python3.11"
    }

    private val binaryDir: File
        get() = File(context.filesDir, "bin").also { it.mkdirs() }

    val ffmpegBinary: File
        get() = File(binaryDir, "ffmpeg")

    val ytdlpBinary: File
        get() = File(binaryDir, "yt-dlp")

    val pythonBinary: File
        get() = File(binaryDir, "python")

    /**
     * Inicializa los binarios copiándolos desde assets si es necesario
     */
    suspend fun initializeBinaries(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Inicializando binarios...")

            // Copiar FFmpeg
            if (!ffmpegBinary.exists() || shouldUpdate(ffmpegBinary)) {
                copyBinaryFromAssets(FFMPEG_ASSET, ffmpegBinary)
                ffmpegBinary.setExecutable(true)
                Log.d(TAG, "FFmpeg copiado y configurado")
            }

            // Copiar yt-dlp
            if (!ytdlpBinary.exists() || shouldUpdate(ytdlpBinary)) {
                copyBinaryFromAssets(YTDLP_ASSET, ytdlpBinary)
                ytdlpBinary.setExecutable(true)
                Log.d(TAG, "yt-dlp copiado y configurado")
            }

            // Copiar Python (necesario para yt-dlp)
            if (!pythonBinary.exists() || shouldUpdate(pythonBinary)) {
                copyBinaryFromAssets(PYTHON_ASSET, pythonBinary)
                pythonBinary.setExecutable(true)
                Log.d(TAG, "Python copiado y configurado")
            }

            Log.d(TAG, "Binarios inicializados correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando binarios: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Copia un binario desde assets
     */
    private fun copyBinaryFromAssets(assetName: String, destination: File) {
        val abi = getDeviceAbi()
        val assetPath = "bin/$abi/$assetName"

        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Binario $assetName copiado desde $assetPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando binario $assetName: ${e.message}", e)
            throw e
        }
    }

    /**
     * Obtiene la arquitectura del dispositivo
     */
    private fun getDeviceAbi(): String {
        val supportedAbis = android.os.Build.SUPPORTED_ABIS
        return when {
            supportedAbis.contains("arm64-v8a") -> "arm64-v8a"
            supportedAbis.contains("armeabi-v7a") -> "armeabi-v7a"
            supportedAbis.contains("x86_64") -> "x86_64"
            supportedAbis.contains("x86") -> "x86"
            else -> {
                Log.w(TAG, "ABI no soportado, usando arm64-v8a por defecto")
                "arm64-v8a"
            }
        }
    }

    /**
     * Verifica si el binario debe actualizarse
     */
    private fun shouldUpdate(binary: File): Boolean {
        // Aquí puedes implementar lógica de versiones
        // Por ahora, siempre usar el binario existente si existe
        return false
    }

    /**
     * Ejecuta un comando FFmpeg
     */
    suspend fun executeFFmpeg(
        command: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullCommand = "${ffmpegBinary.absolutePath} $command"
            executeCommand(fullCommand, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando FFmpeg: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Ejecuta yt-dlp
     */
    suspend fun executeYtDlp(
        url: String,
        outputPath: String,
        format: String = "bestaudio",
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = buildYtDlpCommand(url, outputPath, format)
            executeCommand(command, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando yt-dlp: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Construye comando de yt-dlp
     */
    private fun buildYtDlpCommand(url: String, outputPath: String, format: String): String {
        return buildString {
            append(pythonBinary.absolutePath)
            append(" ")
            append(ytdlpBinary.absolutePath)
            append(" -f $format")
            append(" --extract-audio")
            append(" --audio-format mp3")
            append(" --audio-quality 0")
            append(" -o \"$outputPath\"")
            append(" --no-playlist")
            append(" --progress")
            append(" \"$url\"")
        }
    }

    /**
     * Ejecuta un comando del sistema
     */
    private suspend fun executeCommand(
        command: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Ejecutando comando: $command")

            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = StringBuilder()
            val error = StringBuilder()

            // Leer output
            val outputReader = process.inputStream.bufferedReader()
            val errorReader = process.errorStream.bufferedReader()

            // Thread para leer stdout
            val outputThread = Thread {
                outputReader.useLines { lines ->
                    lines.forEach { line ->
                        output.appendLine(line)
                        Log.d(TAG, "Output: $line")
                        
                        // Parsear progreso si está disponible
                        onProgress?.let { callback ->
                            parseProgress(line)?.let { progress ->
                                callback(progress)
                            }
                        }
                    }
                }
            }

            // Thread para leer stderr
            val errorThread = Thread {
                errorReader.useLines { lines ->
                    lines.forEach { line ->
                        error.appendLine(line)
                        Log.d(TAG, "Error: $line")
                    }
                }
            }

            outputThread.start()
            errorThread.start()

            val exitCode = process.waitFor()
            outputThread.join()
            errorThread.join()

            if (exitCode == 0) {
                Log.d(TAG, "Comando ejecutado exitosamente")
                Result.success(output.toString())
            } else {
                Log.e(TAG, "Comando falló con código $exitCode: ${error.toString()}")
                Result.failure(Exception("Comando falló: ${error.toString()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando comando: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parsea el progreso desde la salida del comando
     */
    private fun parseProgress(line: String): Float? {
        return try {
            // FFmpeg: time=00:01:23.45
            if (line.contains("time=")) {
                val timeStr = line.substringAfter("time=").substringBefore(" ").trim()
                val parts = timeStr.split(":")
                if (parts.size >= 3) {
                    val hours = parts[0].toFloatOrNull() ?: 0f
                    val minutes = parts[1].toFloatOrNull() ?: 0f
                    val seconds = parts[2].toFloatOrNull() ?: 0f
                    val totalSeconds = hours * 3600 + minutes * 60 + seconds
                    // Necesitarías la duración total para calcular el porcentaje real
                    // Por ahora retornamos un valor aproximado
                    (totalSeconds / 180f).coerceIn(0f, 1f) // Asumiendo ~3 min
                } else null
            }
            // yt-dlp: [download] 45.2% of 3.5MiB
            else if (line.contains("[download]") && line.contains("%")) {
                val percentStr = line.substringAfter("[download]")
                    .substringBefore("%")
                    .trim()
                    .toFloatOrNull()
                percentStr?.div(100f)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene la versión de FFmpeg
     */
    suspend fun getFFmpegVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val result = executeFFmpeg("-version")
            if (result.isSuccess) {
                result.getOrNull()?.lines()?.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene la versión de yt-dlp
     */
    suspend fun getYtDlpVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val command = "${pythonBinary.absolutePath} ${ytdlpBinary.absolutePath} --version"
            val result = executeCommand(command)
            if (result.isSuccess) {
                result.getOrNull()?.trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
