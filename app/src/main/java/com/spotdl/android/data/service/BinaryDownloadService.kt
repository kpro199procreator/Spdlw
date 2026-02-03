package com.spotdl.android.data.service

import android.content.Context
import android.util.Log
import com.spotdl.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Servicio para descargar e instalar binarios
 */
class BinaryDownloadService(private val context: Context) {

    companion object {
        private const val TAG = "BinaryDownloadService"
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
    }

    private val binaryDir = File(context.filesDir, "bin").apply { mkdirs() }
    private val downloadDir = File(context.cacheDir, "downloads").apply { mkdirs() }
    
    private val _downloadStates = MutableStateFlow<Map<String, BinaryState>>(emptyMap())
    val downloadStates: Flow<Map<String, BinaryState>> = _downloadStates.asStateFlow()

    /**
     * Descarga e instala un binario
     */
    suspend fun downloadAndInstall(
        binary: BinaryInfo,
        source: DownloadSource = BinaryRepositories.OFFICIAL_GITHUB
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            updateState(binary, BinaryStatus.DOWNLOADING, 0f)
            
            // Determinar URL de descarga
            val downloadUrl = if (source == BinaryRepositories.OFFICIAL_GITHUB) {
                binary.downloadUrl
            } else {
                adaptUrlForSource(binary.downloadUrl, source)
            }
            
            Log.d(TAG, "Descargando ${binary.name} desde $downloadUrl")
            
            // Descargar archivo
            val downloadedFile = File(downloadDir, "${binary.name}.tmp")
            val downloadResult = downloadFile(downloadUrl, downloadedFile) { progress ->
                updateState(binary, BinaryStatus.DOWNLOADING, progress)
            }
            
            if (downloadResult.isFailure) {
                // Intentar con URLs alternativas
                return@withContext tryAlternativeUrls(binary, source)
            }
            
            // Verificar checksum si está disponible
            if (binary.checksum != null) {
                updateState(binary, BinaryStatus.VERIFYING, 0.9f)
                if (!verifyChecksum(downloadedFile, binary.checksum, binary.checksumAlgorithm)) {
                    updateState(binary, BinaryStatus.FAILED, 0f, "Checksum inválido")
                    return@withContext Result.failure(Exception("Checksum verification failed"))
                }
            }
            
            // Extraer o copiar binario
            updateState(binary, BinaryStatus.EXTRACTING, 0.95f)
            val installedFile = extractAndInstall(binary, downloadedFile)
            
            // Hacer ejecutable
            installedFile.setExecutable(true)
            installedFile.setReadable(true)
            
            // Limpiar archivo temporal
            downloadedFile.delete()
            
            // Marcar como instalado
            updateState(binary, BinaryStatus.INSTALLED, 1f)
            saveInstalledVersion(binary.name, binary.version)
            
            Log.d(TAG, "${binary.name} instalado correctamente")
            Result.success(installedFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando ${binary.name}: ${e.message}", e)
            updateState(binary, BinaryStatus.FAILED, 0f, e.message)
            Result.failure(e)
        }
    }

    /**
     * Descarga un archivo con progreso
     */
    private suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "SpotDL-Android/1.0")
            
            // Seguir redirects
            connection.instanceFollowRedirects = true
            
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("HTTP error: $responseCode")
                )
            }
            
            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L
            
            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
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
            
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando archivo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Intenta descargar usando URLs alternativas
     */
    private suspend fun tryAlternativeUrls(
        binary: BinaryInfo,
        source: DownloadSource
    ): Result<File> {
        for ((index, altUrl) in binary.alternativeUrls.withIndex()) {
            Log.d(TAG, "Intentando URL alternativa ${index + 1}/${binary.alternativeUrls.size}")
            
            val downloadedFile = File(downloadDir, "${binary.name}_alt$index.tmp")
            val result = downloadFile(altUrl, downloadedFile) { progress ->
                updateState(binary, BinaryStatus.DOWNLOADING, progress)
            }
            
            if (result.isSuccess) {
                updateState(binary, BinaryStatus.EXTRACTING, 0.95f)
                val installedFile = extractAndInstall(binary, downloadedFile)
                installedFile.setExecutable(true)
                downloadedFile.delete()
                
                updateState(binary, BinaryStatus.INSTALLED, 1f)
                saveInstalledVersion(binary.name, binary.version)
                
                return Result.success(installedFile)
            }
        }
        
        updateState(binary, BinaryStatus.FAILED, 0f, "Todas las URLs fallaron")
        return Result.failure(Exception("Could not download from any source"))
    }

    /**
     * Extrae e instala el binario
     */
    private suspend fun extractAndInstall(
        binary: BinaryInfo,
        downloadedFile: File
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(binaryDir, binary.name)
        
        when {
            // Si es un archivo AAR (FFmpeg Kit)
            downloadedFile.name.endsWith(".aar") -> {
                extractFromAar(downloadedFile, binary.name, outputFile)
            }
            // Si es un archivo ZIP
            downloadedFile.name.endsWith(".zip") -> {
                extractFromZip(downloadedFile, binary.name, outputFile)
            }
            // Si es un tar.xz
            downloadedFile.name.endsWith(".tar.xz") || 
            downloadedFile.name.endsWith(".tar.gz") -> {
                extractFromTar(downloadedFile, binary.name, outputFile)
            }
            // Si es un DEB (Termux packages)
            downloadedFile.name.endsWith(".deb") -> {
                extractFromDeb(downloadedFile, binary.name, outputFile)
            }
            // Archivo directo
            else -> {
                downloadedFile.copyTo(outputFile, overwrite = true)
            }
        }
        
        outputFile
    }

    /**
     * Extrae binario de un archivo AAR
     */
    private fun extractFromAar(aarFile: File, binaryName: String, outputFile: File) {
        ZipInputStream(aarFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            
            while (entry != null) {
                // Buscar el binario en jni/<arch>/
                if (entry.name.contains(binaryName) && 
                    entry.name.contains("jni/") &&
                    !entry.isDirectory) {
                    
                    FileOutputStream(outputFile).use { output ->
                        zip.copyTo(output)
                    }
                    return
                }
                entry = zip.nextEntry
            }
        }
        
        throw Exception("Binary not found in AAR")
    }

    /**
     * Extrae binario de un archivo ZIP
     */
    private fun extractFromZip(zipFile: File, binaryName: String, outputFile: File) {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            
            while (entry != null) {
                if (entry.name.endsWith(binaryName) && !entry.isDirectory) {
                    FileOutputStream(outputFile).use { output ->
                        zip.copyTo(output)
                    }
                    return
                }
                entry = zip.nextEntry
            }
        }
        
        throw Exception("Binary not found in ZIP")
    }

    /**
     * Extrae binario de un archivo TAR
     */
    private fun extractFromTar(tarFile: File, binaryName: String, outputFile: File) {
        // Por ahora, copiamos directamente
        // En producción, usarías una librería como Apache Commons Compress
        tarFile.copyTo(outputFile, overwrite = true)
    }

    /**
     * Extrae binario de un paquete DEB
     */
    private fun extractFromDeb(debFile: File, binaryName: String, outputFile: File) {
        // Similar a TAR - requiere extracción del paquete DEB
        debFile.copyTo(outputFile, overwrite = true)
    }

    /**
     * Verifica el checksum de un archivo
     */
    private suspend fun verifyChecksum(
        file: File,
        expectedChecksum: String,
        algorithm: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(BUFFER_SIZE)
            
            file.inputStream().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val checksum = digest.digest().joinToString("") { 
                "%02x".format(it) 
            }
            
            checksum.equals(expectedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando checksum: ${e.message}", e)
            false
        }
    }

    /**
     * Adapta URL para una fuente específica
     */
    private fun adaptUrlForSource(originalUrl: String, source: DownloadSource): String {
        return when (source.id) {
            "jsdelivr" -> {
                // Convertir GitHub URL a jsDelivr
                // https://github.com/user/repo/releases/download/v1.0/file
                // -> https://cdn.jsdelivr.net/gh/user/repo@v1.0/file
                originalUrl.replace(
                    Regex("https://github.com/([^/]+)/([^/]+)/releases/download/([^/]+)/(.+)"),
                    "https://cdn.jsdelivr.net/gh/$1/$2@$3/$4"
                )
            }
            "ghproxy" -> {
                // Agregar proxy
                "https://ghproxy.com/$originalUrl"
            }
            else -> originalUrl
        }
    }

    /**
     * Actualiza el estado de un binario
     */
    private fun updateState(
        binary: BinaryInfo,
        status: BinaryStatus,
        progress: Float = 0f,
        error: String? = null
    ) {
        val currentStates = _downloadStates.value.toMutableMap()
        currentStates[binary.name] = BinaryState(
            info = binary,
            status = status,
            downloadProgress = progress,
            errorMessage = error
        )
        _downloadStates.value = currentStates
    }

    /**
     * Verifica qué binarios están instalados
     */
    suspend fun checkInstalledBinaries(
        binaries: List<BinaryInfo>
    ): Map<String, BinaryState> = withContext(Dispatchers.IO) {
        val states = mutableMapOf<String, BinaryState>()
        
        for (binary in binaries) {
            val file = File(binaryDir, binary.name)
            val installedVersion = getInstalledVersion(binary.name)
            
            val status = when {
                !file.exists() -> BinaryStatus.NOT_INSTALLED
                installedVersion == null -> BinaryStatus.INSTALLED
                installedVersion != binary.version -> BinaryStatus.UPDATE_AVAILABLE
                else -> BinaryStatus.INSTALLED
            }
            
            states[binary.name] = BinaryState(
                info = binary,
                status = status,
                installedVersion = installedVersion,
                downloadProgress = if (status == BinaryStatus.INSTALLED) 1f else 0f
            )
        }
        
        _downloadStates.value = states
        states
    }

    /**
     * Guarda la versión instalada
     */
    private fun saveInstalledVersion(binaryName: String, version: String) {
        val prefs = context.getSharedPreferences("binaries", Context.MODE_PRIVATE)
        prefs.edit().putString("${binaryName}_version", version).apply()
    }

    /**
     * Obtiene la versión instalada
     */
    private fun getInstalledVersion(binaryName: String): String? {
        val prefs = context.getSharedPreferences("binaries", Context.MODE_PRIVATE)
        return prefs.getString("${binaryName}_version", null)
    }

    /**
     * Elimina un binario
     */
    suspend fun removeBinary(binaryName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(binaryDir, binaryName)
            if (file.exists()) {
                file.delete()
            }
            
            val prefs = context.getSharedPreferences("binaries", Context.MODE_PRIVATE)
            prefs.edit().remove("${binaryName}_version").apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
