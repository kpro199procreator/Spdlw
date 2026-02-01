# 🚀 Guía Rápida de Inicio - SpotDL Android

## ✅ Proyecto Creado Exitosamente

Tu aplicación Android completa está lista. Este proyecto incluye:

- ✨ **UI moderna** con Jetpack Compose y Material Design 3
- 🎵 **Descarga de música** desde YouTube con metadatos de Spotify
- 🔄 **Conversión de audio** con Mobile FFmpeg
- 📱 **Múltiples formatos**: MP3, M4A, FLAC, WAV, OGG
- ⚙️ **Configuración completa** de calidad y metadatos

## 📂 Estructura del Proyecto

```
SpotDL-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/spotdl/android/
│   │   │   ├── data/                    # Capa de datos
│   │   │   │   ├── model/              # Modelos (Song, DownloadProgress, etc.)
│   │   │   │   ├── repository/         # DownloadRepository
│   │   │   │   └── service/            # YouTube, Spotify, FFmpeg services
│   │   │   ├── ui/                      # Capa de presentación
│   │   │   │   ├── screens/            # MainScreen, SettingsDialog
│   │   │   │   ├── theme/              # Theme, Typography
│   │   │   │   └── viewmodel/          # MainViewModel
│   │   │   ├── MainActivity.kt
│   │   │   └── SpotDLApplication.kt
│   │   ├── res/                         # Recursos (strings, colors, themes)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                 # Dependencias del módulo
│   └── proguard-rules.pro              # Reglas de ofuscación
├── build.gradle.kts                     # Configuración del proyecto
├── settings.gradle.kts                  # Configuración de módulos
├── gradle.properties                    # Propiedades de Gradle
├── README.md                            # Documentación principal
├── IMPLEMENTATION_GUIDE.md              # Guía técnica detallada
└── .gitignore                          # Archivos ignorados por Git
```

## 🛠️ Requisitos Previos

Antes de compilar, asegúrate de tener instalado:

1. **Android Studio** (Hedgehog 2023.1.1 o superior)
   - Descarga: https://developer.android.com/studio

2. **JDK 17** (incluido con Android Studio)

3. **Android SDK** con los siguientes componentes:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android Emulator (para testing)

## 📥 Cómo Abrir el Proyecto

### Opción 1: Desde Android Studio

1. Abre Android Studio
2. Selecciona **"Open an Existing Project"**
3. Navega a la carpeta `SpotDL-Android`
4. Haz clic en **"OK"**
5. Espera a que Gradle sincronice las dependencias (puede tomar 2-5 minutos)

### Opción 2: Desde la Línea de Comandos

```bash
cd SpotDL-Android

# En Linux/Mac
./gradlew build

# En Windows
gradlew.bat build
```

## 🔧 Configuración Inicial

### 1. Sincronizar Dependencias

Después de abrir el proyecto, Android Studio automáticamente:
- Descargará todas las dependencias de Gradle
- Sincronizará el proyecto
- Indexará los archivos

Si no ocurre automáticamente:
- **File → Sync Project with Gradle Files**

### 2. Verificar Configuración del SDK

1. Ve a **File → Project Structure**
2. En **SDK Location**, verifica que apunte a tu Android SDK
3. En **Modules → app**, verifica:
   - Compile SDK Version: 34
   - Build Tools Version: 34.0.0
   - Source Compatibility: Java 17
   - Target Compatibility: Java 17

## ▶️ Compilar y Ejecutar

### Ejecutar en Emulador

1. **Crear un Emulador** (si no tienes uno):
   - Tools → Device Manager → Create Device
   - Selecciona un dispositivo (ej: Pixel 6)
   - Selecciona una imagen del sistema (API 34 recomendado)
   - Haz clic en Finish

2. **Ejecutar la app**:
   - Haz clic en el botón **Run** (▶️) o presiona `Shift + F10`
   - Selecciona el emulador
   - Espera a que la app se instale y lance

### Ejecutar en Dispositivo Físico

1. **Habilitar Opciones de Desarrollador** en tu dispositivo:
   - Ve a Configuración → Acerca del teléfono
   - Toca "Número de compilación" 7 veces
   - Vuelve a Configuración → Opciones de desarrollador
   - Activa "Depuración USB"

2. **Conectar el dispositivo**:
   - Conecta tu dispositivo con un cable USB
   - Autoriza la depuración USB cuando se solicite

3. **Ejecutar la app**:
   - Haz clic en el botón **Run** (▶️)
   - Selecciona tu dispositivo
   - La app se instalará automáticamente

## 📦 Compilar APK

### Debug APK (para testing)

```bash
./gradlew assembleDebug
```

El APK se generará en:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (para distribución)

1. **Crear Keystore** (primera vez):
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. **Configurar en `app/build.gradle.kts`**:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "tu-password"
            keyAlias = "my-key-alias"
            keyPassword = "tu-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

3. **Compilar**:
```bash
./gradlew assembleRelease
```

El APK se generará en:
```
app/build/outputs/apk/release/app-release.apk
```

## 🐛 Solución de Problemas Comunes

### Error: "SDK location not found"

**Solución**: Crea un archivo `local.properties` en la raíz del proyecto:
```properties
sdk.dir=/ruta/a/tu/Android/Sdk
```

En Windows: `sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk`
En Mac: `sdk.dir=/Users/TuUsuario/Library/Android/sdk`
En Linux: `sdk.dir=/home/TuUsuario/Android/Sdk`

### Error: "Unsupported Java version"

**Solución**: Asegúrate de usar JDK 17
1. File → Project Structure → SDK Location → Gradle Settings
2. Gradle JDK: selecciona "jbr-17" o "JDK 17"

### Error: "Failed to resolve: com.arthenica:mobile-ffmpeg-full"

**Solución**: 
1. Verifica tu conexión a Internet
2. Limpia y reconstruye:
```bash
./gradlew clean build --refresh-dependencies
```

### La app se cierra inmediatamente

**Causas comunes**:
1. **Permisos no otorgados**: Verifica en Configuración → Apps → SpotDL → Permisos
2. **Falta FFmpeg**: El proyecto debería incluirlo, pero verifica las dependencias
3. **Error de red**: Asegúrate de tener conexión a Internet

**Verificar logs**:
```bash
adb logcat | grep "SpotDL"
```

## 📝 Próximos Pasos

### Mejoras Recomendadas

1. **Implementar youtube-dl/yt-dlp real**:
   - La implementación actual usa web scraping
   - Para producción, integra youtube-dl como binario

2. **API oficial de Spotify**:
   - Registra una app en https://developer.spotify.com
   - Implementa OAuth 2.0
   - Usa la Web API para metadatos precisos

3. **Base de datos local**:
   - Añade Room para historial de descargas
   - Implementa caché de búsquedas

4. **Descargas en segundo plano**:
   - Usa WorkManager para descargas persistentes
   - Implementa notificaciones de progreso

5. **Soporte de playlists**:
   - Descarga múltiples canciones de una playlist
   - Importa desde Spotify/YouTube

### Testing

```bash
# Unit tests
./gradlew test

# Instrumental tests (requiere emulador/dispositivo)
./gradlew connectedAndroidTest

# Todos los tests
./gradlew check
```

## 📚 Recursos Adicionales

- **Documentación**: Lee `README.md` para información general
- **Guía técnica**: Lee `IMPLEMENTATION_GUIDE.md` para detalles de implementación
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **FFmpeg**: https://trac.ffmpeg.org/wiki/CompilationGuide
- **Material Design 3**: https://m3.material.io/

## 🎯 Funcionalidades Principales

### Ya Implementadas ✅

- ✅ Búsqueda de canciones por nombre
- ✅ Procesamiento de URLs de Spotify
- ✅ Descarga de audio desde YouTube
- ✅ Conversión a múltiples formatos (MP3, M4A, FLAC, WAV, OGG)
- ✅ Selección de calidad (128k - 320k)
- ✅ Incrustación de metadatos (título, artista, álbum)
- ✅ Descarga de artwork
- ✅ UI con Jetpack Compose
- ✅ Gestión de descargas con progreso
- ✅ Configuración personalizable
- ✅ Integración con MediaStore

### Por Implementar 🚧

- ⏳ youtube-dl/yt-dlp binario (actualmente web scraping)
- ⏳ API oficial de Spotify (actualmente web scraping)
- ⏳ Base de datos Room para historial
- ⏳ WorkManager para descargas en background
- ⏳ Soporte de playlists
- ⏳ Tests unitarios e instrumentales

## 💡 Tips de Desarrollo

1. **Hot Reload**: Jetpack Compose soporta hot reload para cambios en UI
2. **Preview**: Usa `@Preview` en funciones Composable para vista previa
3. **Logs**: Usa `Log.d(TAG, "mensaje")` para debugging
4. **Breakpoints**: Usa el debugger de Android Studio
5. **Layout Inspector**: Tools → Layout Inspector para inspeccionar UI

## 🤝 Contribuir

Si encuentras bugs o quieres añadir features:

1. Fork el proyecto
2. Crea una rama: `git checkout -b feature/nueva-feature`
3. Commit: `git commit -m 'Añade nueva feature'`
4. Push: `git push origin feature/nueva-feature`
5. Abre un Pull Request

## 📄 Licencia

Este proyecto es solo para fines educativos. Respeta siempre los derechos de autor y las condiciones de servicio de las plataformas.

---

**¿Problemas?** Abre un issue en GitHub o revisa la documentación adicional.

**¡Disfruta creando tu aplicación de música!** 🎵
