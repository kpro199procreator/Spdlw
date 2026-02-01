# SpotDL Android

Una aplicación Android completa que replica la funcionalidad de spotdl, permitiendo descargar música desde YouTube con metadatos de Spotify.

## 🎵 Características

- **Búsqueda inteligente**: Busca canciones por nombre o pega URLs de Spotify/YouTube
- **Descargas de alta calidad**: Descarga audio con calidad configurable (hasta 320kbps)
- **Múltiples formatos**: Soporte para MP3, M4A, FLAC, WAV y OGG
- **Metadatos automáticos**: Incrustación automática de título, artista, álbum y portada
- **UI moderna**: Interfaz Material Design 3 con Jetpack Compose
- **Gestión de descargas**: Visualiza el progreso de múltiples descargas simultáneas
- **Conversión con FFmpeg**: Conversión de audio de alta calidad usando Mobile FFmpeg

## 📱 Tecnologías utilizadas

### Core
- **Kotlin** - Lenguaje de programación
- **Jetpack Compose** - UI moderna y declarativa
- **Material Design 3** - Sistema de diseño

### Arquitectura
- **MVVM** (Model-View-ViewModel)
- **Coroutines & Flow** - Programación asíncrona y reactiva
- **StateFlow** - Manejo de estado

### Multimedia
- **Mobile FFmpeg** - Conversión y procesamiento de audio
- **FFmpeg comandos**:
  - Conversión de formato
  - Ajuste de bitrate y calidad
  - Incrustación de metadatos
  - Incrustación de artwork

### Networking
- **Retrofit** - Cliente HTTP
- **OkHttp** - Cliente HTTP de bajo nivel
- **Jsoup** - Web scraping para metadatos de Spotify
- **Coil** - Carga de imágenes

### Almacenamiento
- **MediaStore** - Integración con la biblioteca de música del sistema
- **File System** - Gestión de archivos

### Permisos
- **Accompanist Permissions** - Manejo moderno de permisos

## 🚀 Cómo usar

### Requisitos
- Android Studio Hedgehog o superior
- Android SDK 24+ (Android 7.0+)
- Permisos de almacenamiento

### Instalación

1. **Clonar el repositorio**
```bash
git clone https://github.com/tu-usuario/spotdl-android.git
cd spotdl-android
```

2. **Abrir en Android Studio**
- Abre Android Studio
- Selecciona "Open an Existing Project"
- Navega a la carpeta del proyecto

3. **Sincronizar dependencias**
```bash
./gradlew build
```

4. **Ejecutar la app**
- Conecta un dispositivo Android o inicia un emulador
- Presiona Run (▶️) en Android Studio

### Uso de la aplicación

#### Buscar y descargar

1. **Búsqueda por nombre**:
   - Escribe el nombre de la canción en la barra de búsqueda
   - Presiona "Buscar"
   - Selecciona la canción deseada
   - Toca el botón de descarga

2. **Usando URL de Spotify**:
   - Copia el enlace de una canción desde Spotify
   - Pégalo en la barra de búsqueda
   - La app extraerá automáticamente los metadatos
   - Toca descargar

3. **Compartir desde otras apps**:
   - Abre Spotify o YouTube
   - Comparte una canción
   - Selecciona SpotDL
   - La canción se cargará automáticamente

#### Configuración

1. Toca el icono de configuración (⚙️)
2. Ajusta las siguientes opciones:
   - **Formato**: MP3, M4A, FLAC, WAV, OGG
   - **Calidad**: Baja (128k), Media (192k), Alta (256k), Muy Alta (320k)
   - **Metadatos**: Activar/desactivar incrustación de metadatos
   - **Artwork**: Activar/desactivar portada del álbum
   - **Plantilla de nombre**: Personalizar formato del nombre de archivo

#### Gestión de descargas

- Ve a la pestaña "Descargas" para ver el progreso
- Cancela descargas en curso tocando la X
- Limpia descargas completadas con "Limpiar completadas"

## 📂 Estructura del proyecto

```
app/
├── src/main/
│   ├── java/com/spotdl/android/
│   │   ├── data/
│   │   │   ├── model/           # Modelos de datos
│   │   │   │   └── Models.kt
│   │   │   ├── repository/      # Repositorios
│   │   │   │   └── DownloadRepository.kt
│   │   │   └── service/         # Servicios
│   │   │       ├── YouTubeService.kt
│   │   │       ├── SpotifyService.kt
│   │   │       └── FFmpegService.kt
│   │   ├── ui/
│   │   │   ├── screens/         # Pantallas
│   │   │   │   ├── MainScreen.kt
│   │   │   │   └── SettingsDialog.kt
│   │   │   ├── theme/           # Tema
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   └── viewmodel/       # ViewModels
│   │   │       └── MainViewModel.kt
│   │   ├── MainActivity.kt
│   │   └── SpotDLApplication.kt
│   ├── res/                     # Recursos
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## ⚙️ Configuración avanzada

### Formatos de audio soportados

```kotlin
AudioFormat.MP3    // MPEG Audio Layer 3 (más compatible)
AudioFormat.M4A    // MPEG-4 Audio (Apple)
AudioFormat.FLAC   // Free Lossless Audio Codec (sin pérdida)
AudioFormat.WAV    // Waveform Audio (sin comprimir)
AudioFormat.OGG    // Ogg Vorbis (código abierto)
```

### Calidades disponibles

```kotlin
AudioQuality.LOW        // 128 kbps
AudioQuality.MEDIUM     // 192 kbps
AudioQuality.HIGH       // 256 kbps
AudioQuality.VERY_HIGH  // 320 kbps
```

### Variables de plantilla de nombre

- `{artist}` - Nombre del artista
- `{title}` - Título de la canción
- `{album}` - Nombre del álbum
- `{year}` - Año de lanzamiento

Ejemplo: `"{artist} - {title}"` → `"The Beatles - Hey Jude.mp3"`

## 🔧 Comandos FFmpeg utilizados

### Conversión a MP3
```bash
ffmpeg -i input.webm -codec:a libmp3lame -b:a 320k -ar 44100 -ac 2 -y output.mp3
```

### Conversión a M4A
```bash
ffmpeg -i input.webm -codec:a aac -b:a 256k -ar 44100 -ac 2 -y output.m4a
```

### Incrustación de metadatos
```bash
ffmpeg -i audio.mp3 -i artwork.jpg \
  -c copy \
  -metadata title="Song Title" \
  -metadata artist="Artist Name" \
  -metadata album="Album Name" \
  -map 0:0 -map 1:0 \
  -id3v2_version 3 \
  -y output.mp3
```

## 📝 Notas importantes

### Limitaciones actuales

1. **YouTube Download**: La implementación actual usa web scraping. Para producción, se recomienda usar:
   - `youtube-dl` / `yt-dlp` como binario
   - API oficial de YouTube (requiere API key)

2. **Spotify Metadata**: Sin API oficial de Spotify, los metadatos se extraen mediante web scraping. Para mejor funcionalidad:
   - Registrar una aplicación en Spotify Developer Dashboard
   - Implementar OAuth 2.0
   - Usar Spotify Web API

### Mejoras sugeridas

1. **Descarga en segundo plano**:
   - Implementar WorkManager para descargas persistentes
   - Notificaciones de progreso

2. **Base de datos local**:
   - Room Database para historial de descargas
   - Caché de búsquedas

3. **Playlist support**:
   - Descargar playlists completas
   - Importar desde Spotify/YouTube

4. **Más fuentes**:
   - SoundCloud
   - Bandcamp
   - Apple Music

## 🐛 Resolución de problemas

### La descarga falla

- Verifica tu conexión a Internet
- Asegúrate de tener permisos de almacenamiento
- Verifica que haya espacio disponible en el dispositivo

### No se encuentran resultados

- Verifica la ortografía de la búsqueda
- Intenta con una URL directa de Spotify/YouTube
- Revisa tu conexión a Internet

### Error de conversión FFmpeg

- El formato seleccionado puede no ser compatible
- Intenta con MP3 (más compatible)
- Verifica los logs para más detalles

## 📄 Licencia

Este proyecto es solo para fines educativos. Asegúrate de respetar los derechos de autor y las políticas de uso de YouTube y Spotify.

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📧 Contacto

¿Preguntas? Abre un issue en GitHub.

---

**Disclaimer**: Esta aplicación es solo para fines educativos. Respeta siempre los derechos de autor y las condiciones de servicio de las plataformas de streaming.
