# Guía de Implementación - SpotDL Android

## 🏗️ Arquitectura de la Aplicación

### MVVM Pattern

```
┌─────────────┐
│     View    │ ← MainScreen.kt (Compose UI)
└──────┬──────┘
       │
       ↓
┌─────────────┐
│  ViewModel  │ ← MainViewModel.kt (Estado y lógica)
└──────┬──────┘
       │
       ↓
┌─────────────┐
│ Repository  │ ← DownloadRepository.kt (Coordinación)
└──────┬──────┘
       │
       ↓
┌─────────────┐
│  Services   │ ← YouTubeService, SpotifyService, FFmpegService
└─────────────┘
```

## 📦 Flujo de Descarga

### Paso 1: Búsqueda
```kotlin
// Usuario busca una canción
viewModel.search("The Beatles Hey Jude")

// O procesa una URL de Spotify
viewModel.processSharedUrl("https://open.spotify.com/track/...")
```

### Paso 2: Obtención de Metadatos
```kotlin
// SpotifyService extrae metadatos
val song = spotifyService.getSongFromUrl(spotifyUrl)
// Retorna: Song(title, artist, album, artworkUrl, etc.)
```

### Paso 3: Búsqueda en YouTube
```kotlin
// YouTubeService busca el audio
val videoId = youtubeService.searchSong("${song.artist} ${song.title}")
// Encuentra el mejor match en YouTube
```

### Paso 4: Descarga de Audio
```kotlin
// Descarga el stream de audio
youtubeService.downloadAudio(
    youtubeUrl = "https://youtube.com/watch?v=...",
    outputFile = tempFile,
    onProgress = { progress ->
        // Actualiza UI con progreso 0.0 - 1.0
    }
)
```

### Paso 5: Conversión con FFmpeg
```kotlin
// Convierte a formato deseado
ffmpegService.convertAudio(
    inputFile = tempFile,
    outputFile = finalFile,
    format = AudioFormat.MP3,
    quality = AudioQuality.HIGH
)
```

### Paso 6: Metadatos y Artwork
```kotlin
// Descarga portada
spotifyService.downloadArtwork(song.artworkUrl, artworkFile)

// Incrusta metadatos
ffmpegService.embedMetadata(
    inputFile = convertedFile,
    outputFile = finalFile,
    song = song,
    artworkFile = artworkFile
)
```

### Paso 7: Registro en MediaStore
```kotlin
// Hace visible el archivo en la galería de música
MediaScannerConnection.scanFile(
    context,
    arrayOf(finalFile.absolutePath),
    arrayOf("audio/*"),
    null
)
```

## 🎨 Componentes de UI en Compose

### Barra de Búsqueda
```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Buscar canción") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}
```

### Tarjeta de Canción
```kotlin
@Composable
fun SongCard(song: Song, onDownload: () -> Unit) {
    Card {
        Row {
            AsyncImage(model = song.artworkUrl, ...)
            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, "Descargar")
            }
        }
    }
}
```

### Indicador de Progreso
```kotlin
@Composable
fun DownloadProgressCard(download: DownloadProgress) {
    Card {
        Column {
            Text(download.song.title)
            LinearProgressIndicator(progress = download.progress)
            Text(download.currentStep)
        }
    }
}
```

## 🔧 Configuración de FFmpeg

### Parámetros de Conversión

#### MP3 (Más compatible)
```kotlin
val command = listOf(
    "-i", inputFile,
    "-codec:a", "libmp3lame",    // Codec MP3
    "-b:a", "320k",               // Bitrate
    "-ar", "44100",               // Sample rate
    "-ac", "2",                   // Canales (estéreo)
    "-y", outputFile
)
```

#### FLAC (Sin pérdida)
```kotlin
val command = listOf(
    "-i", inputFile,
    "-codec:a", "flac",
    "-compression_level", "8",   // Máxima compresión
    "-ar", "44100",
    "-y", outputFile
)
```

#### M4A (Apple/iTunes)
```kotlin
val command = listOf(
    "-i", inputFile,
    "-codec:a", "aac",
    "-b:a", "256k",
    "-ar", "44100",
    "-ac", "2",
    "-y", outputFile
)
```

### Incrustación de Metadatos ID3v2

```kotlin
val command = listOf(
    "-i", audioFile,              // Archivo de audio
    "-i", artworkFile,            // Imagen de portada
    "-c", "copy",                 // Copiar streams sin re-codificar
    "-metadata", "title=$title",
    "-metadata", "artist=$artist",
    "-metadata", "album=$album",
    "-metadata", "date=$year",
    "-metadata", "genre=$genre",
    "-map", "0:0",                // Mapear audio
    "-map", "1:0",                // Mapear imagen
    "-id3v2_version", "3",        // Versión ID3v2.3
    "-y", outputFile
)
```

## 🔄 Manejo de Estado con Flow

### ViewModel con StateFlow
```kotlin
class MainViewModel : ViewModel() {
    private val _downloads = MutableStateFlow<List<DownloadProgress>>(emptyList())
    val downloads: StateFlow<List<DownloadProgress>> = _downloads.asStateFlow()
    
    fun downloadSong(song: Song) {
        viewModelScope.launch {
            repository.downloadSong(song, config)
                .collect { progress ->
                    updateProgress(progress)
                }
        }
    }
}
```

### Observación en Compose
```kotlin
@Composable
fun DownloadsList(viewModel: MainViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    
    LazyColumn {
        items(downloads) { download ->
            DownloadCard(download)
        }
    }
}
```

## 🎯 Casos de Uso Comunes

### 1. Descargar canción por búsqueda
```kotlin
// 1. Buscar
viewModel.search("Pink Floyd - Time")

// 2. Esperar resultados
viewModel.searchResults.collect { results ->
    if (results.isNotEmpty()) {
        // 3. Descargar primera coincidencia
        viewModel.downloadSong(results[0])
    }
}
```

### 2. Descargar desde URL de Spotify
```kotlin
val spotifyUrl = "https://open.spotify.com/track/..."
viewModel.processSharedUrl(spotifyUrl)

// Automáticamente:
// 1. Extrae metadatos de Spotify
// 2. Busca audio en YouTube
// 3. Descarga y convierte
```

### 3. Configurar calidad personalizada
```kotlin
viewModel.updateAudioFormat(AudioFormat.FLAC)
viewModel.updateAudioQuality(AudioQuality.VERY_HIGH)
viewModel.updateEmbedArtwork(true)
viewModel.updateEmbedMetadata(true)

// Luego descargar
viewModel.downloadSong(song)
```

### 4. Batch download (múltiples canciones)
```kotlin
val playlist = listOf(song1, song2, song3)
viewModel.downloadMultipleSongs(playlist)

// Monitorear progreso de todas
viewModel.downloads.collect { downloadsList ->
    val completed = downloadsList.count { it.status == DownloadStatus.COMPLETED }
    val total = downloadsList.size
    println("$completed de $total completadas")
}
```

## 🛡️ Manejo de Errores

### Errores de Red
```kotlin
try {
    val result = youtubeService.downloadAudio(url, file)
    if (result.isFailure) {
        showError("Error de red: ${result.exceptionOrNull()?.message}")
    }
} catch (e: IOException) {
    showError("Sin conexión a Internet")
}
```

### Errores de FFmpeg
```kotlin
val returnCode = FFmpeg.execute(command)
when (returnCode) {
    Config.RETURN_CODE_SUCCESS -> {
        // Éxito
    }
    Config.RETURN_CODE_CANCEL -> {
        showError("Conversión cancelada")
    }
    else -> {
        val error = Config.getLastCommandOutput()
        showError("Error FFmpeg: $error")
    }
}
```

### Errores de Permisos
```kotlin
if (ContextCompat.checkSelfPermission(context, permission) 
    != PackageManager.PERMISSION_GRANTED) {
    requestPermissions(arrayOf(permission))
} else {
    proceedWithDownload()
}
```

## 🚀 Optimizaciones

### 1. Caché de Búsquedas
```kotlin
private val searchCache = mutableMapOf<String, List<Song>>()

suspend fun search(query: String): List<Song> {
    return searchCache.getOrPut(query) {
        youtubeService.searchSong(query)
    }
}
```

### 2. Descargas Paralelas
```kotlin
suspend fun downloadMultipleSongs(songs: List<Song>) {
    songs.map { song ->
        async { downloadSong(song) }
    }.awaitAll()
}
```

### 3. Compresión de Artwork
```kotlin
suspend fun compressArtwork(inputFile: File): File {
    return withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(inputFile.path)
        val compressed = Bitmap.createScaledBitmap(bitmap, 800, 800, true)
        val outputFile = File(cacheDir, "compressed_artwork.jpg")
        FileOutputStream(outputFile).use { out ->
            compressed.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        outputFile
    }
}
```

## 📊 Testing

### Unit Tests
```kotlin
@Test
fun `download song with valid URL should succeed`() = runTest {
    val song = Song(id = "1", title = "Test", artist = "Test Artist")
    val result = repository.downloadSong(song, config)
    
    assertTrue(result.isSuccess)
}
```

### UI Tests
```kotlin
@Test
fun searchSongShowsResults() {
    composeTestRule.setContent {
        MainScreen(viewModel)
    }
    
    composeTestRule.onNodeWithText("Buscar").performClick()
    composeTestRule.onNodeWithTag("searchResults").assertIsDisplayed()
}
```

## 🔐 Seguridad

### Validación de URLs
```kotlin
fun isValidSpotifyUrl(url: String): Boolean {
    return url.matches(Regex("https://open\\.spotify\\.com/track/[a-zA-Z0-9]+"))
}

fun isValidYouTubeUrl(url: String): Boolean {
    return url.contains("youtube.com/watch?v=") || url.contains("youtu.be/")
}
```

### Sanitización de Nombres de Archivo
```kotlin
fun sanitizeFilename(name: String): String {
    return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .take(255) // Límite de sistema de archivos
}
```

## 📱 Integración con el Sistema

### Compartir desde otras apps
```kotlin
// En AndroidManifest.xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>

// En MainActivity
if (intent.action == Intent.ACTION_SEND) {
    val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
    viewModel.processSharedUrl(sharedUrl)
}
```

### Notificaciones de Progreso
```kotlin
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Descargando: ${song.title}")
    .setContentText(download.currentStep)
    .setProgress(100, (download.progress * 100).toInt(), false)
    .setSmallIcon(R.drawable.ic_download)
    .build()
```

---

Esta guía cubre los aspectos más importantes de la implementación. Para más detalles, consulta el código fuente y los comentarios inline.
