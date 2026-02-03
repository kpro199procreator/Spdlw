# 🔧 Guía de Instalación de Binarios - FFmpeg y yt-dlp

## 📋 Resumen

La aplicación ahora usa **binarios nativos** de FFmpeg y yt-dlp en lugar de librerías que están discontinuadas. Esta es la solución más robusta y mantenible.

## 📦 Binarios Necesarios

### Por arquitectura ARM64 (arm64-v8a) - Más común
- `ffmpeg` - Conversión de audio
- `yt-dlp` - Descarga de YouTube  
- `python3.11` - Necesario para ejecutar yt-dlp

### Otras arquitecturas (opcional)
- ARMv7 (armeabi-v7a)
- x86_64
- x86

## 📁 Estructura de Directorios

Los binarios deben colocarse en:

```
app/src/main/assets/
├── bin/
│   ├── arm64-v8a/
│   │   ├── ffmpeg
│   │   ├── yt-dlp
│   │   └── python3.11
│   ├── armeabi-v7a/
│   │   ├── ffmpeg
│   │   ├── yt-dlp
│   │   └── python3.11
│   ├── x86_64/
│   │   ├── ffmpeg
│   │   ├── yt-dlp
│   │   └── python3.11
│   └── x86/
│       ├── ffmpeg
│       ├── yt-dlp
│       └── python3.11
```

## 🔽 Dónde Descargar los Binarios

### 1. FFmpeg para Android

**Opción A: FFmpeg Binaries (Recomendado)**
- URL: https://github.com/arthenica/ffmpeg-kit/releases
- Descarga: `ffmpeg-kit-full-<version>-<abi>.aar`
- Extrae el binario `ffmpeg` del archivo AAR

**Opción B: John Vansickle Builds**
- URL: https://johnvansickle.com/ffmpeg/
- Descarga la versión ARM para Android
- Es para Linux ARM pero funciona en Android

**Opción C: Compilar desde Fuente**
```bash
git clone https://github.com/arthenica/ffmpeg-kit
cd ffmpeg-kit
./android.sh --enable-arm64-v8a
```

### 2. yt-dlp

**Repositorio oficial:**
- URL: https://github.com/yt-dlp/yt-dlp/releases
- Archivo: `yt-dlp` (script Python)
- No necesita compilación específica para Android

**Descarga directa:**
```bash
wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp
chmod +x yt-dlp
```

### 3. Python 3.11 para Android

**Opción A: Termux Python**
- URL: https://github.com/termux/termux-packages
- Extrae el binario de Python del paquete

**Opción B: Python for Android (p4a)**
- URL: https://github.com/kivy/python-for-android
- Compila Python para Android

**Opción C: Usar Chaquopy (más fácil)**
- URL: https://chaquo.com/chaquopy/
- Integración Python-Android lista para usar

## 🚀 Instalación Paso a Paso

### Método 1: Usando Binarios Pre-compilados (Recomendado)

#### Paso 1: Crear directorio de assets
```bash
cd SpotDL-Android/app/src/main
mkdir -p assets/bin/arm64-v8a
```

#### Paso 2: Descargar FFmpeg
```bash
cd assets/bin/arm64-v8a

# Opción A: Usar binario estático de FFmpeg.org
wget https://github.com/arthenica/ffmpeg-kit/releases/download/v6.0/ffmpeg-kit-full-6.0-arm64-v8a.aar

# Extraer binario
unzip ffmpeg-kit-full-6.0-arm64-v8a.aar
cp jni/arm64-v8a/libffmpeg.so ffmpeg
chmod +x ffmpeg
```

#### Paso 3: Descargar yt-dlp
```bash
wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp
chmod +x yt-dlp
```

#### Paso 4: Agregar Python

**Opción Simple (usar Android Python):**
```bash
# Descargar Python portable para Android
wget https://github.com/python/cpython/releases/download/v3.11.7/python-3.11.7-android-arm64.tar.gz
tar -xzf python-3.11.7-android-arm64.tar.gz
cp python3.11 .
chmod +x python3.11
```

### Método 2: Alternativa Sin Python (Usar yt-dlp compilado)

Si no quieres incluir Python, puedes usar versiones compiladas:

```bash
# Descargar yt-dlp compilado (sin dependencia de Python)
wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_android_arm64
mv yt-dlp_android_arm64 yt-dlp
chmod +x yt-dlp
```

**Nota:** Las versiones compiladas de yt-dlp son experimentales.

### Método 3: Usar Termux Binaries

```bash
# Instalar Termux en tu dispositivo Android
# Dentro de Termux:
pkg install ffmpeg python yt-dlp

# Los binarios están en:
# /data/data/com.termux/files/usr/bin/

# Copia estos binarios a tu proyecto
adb pull /data/data/com.termux/files/usr/bin/ffmpeg assets/bin/arm64-v8a/
adb pull /data/data/com.termux/files/usr/bin/python3.11 assets/bin/arm64-v8a/
adb pull /data/data/com.termux/files/usr/bin/yt-dlp assets/bin/arm64-v8a/
```

## ✅ Verificación

Después de agregar los binarios:

```bash
# Verificar que existen
ls -lh app/src/main/assets/bin/arm64-v8a/

# Deberías ver:
# -rwxr-xr-x  ffmpeg     (~30-50 MB)
# -rwxr-xr-x  yt-dlp     (~3 MB)
# -rwxr-xr-x  python3.11 (~15-20 MB)
```

## 🔧 Configuración en build.gradle

Asegúrate de que los assets no se compriman:

```kotlin
android {
    // ...
    
    aaptOptions {
        noCompress "ffmpeg", "yt-dlp", "python3.11"
    }
    
    // O en versiones más recientes:
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += ['META-INF/*']
        }
    }
}
```

## 📊 Tamaños Aproximados

| Binario | Tamaño por ABI | Total (4 ABIs) |
|---------|----------------|----------------|
| FFmpeg | 30-50 MB | 120-200 MB |
| yt-dlp | 3-5 MB | 12-20 MB |
| Python | 15-20 MB | 60-80 MB |
| **TOTAL** | **50-75 MB** | **200-300 MB** |

### Optimización

**Solo incluir ARM64 (recomendado):**
- 99% de dispositivos Android modernos son ARM64
- Ahorra ~150-225 MB
- APK final: ~50-75 MB

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }
}
```

## 🚨 Problemas Comunes

### Problema 1: "Permission denied"
```
Solución: Los binarios deben ser ejecutables
chmod +x ffmpeg yt-dlp python3.11
```

### Problema 2: "Binary not found"
```
Solución: Verificar ruta en BinaryManager.kt
La app copia binarios a: /data/data/com.spotdl.android/files/bin/
```

### Problema 3: APK muy grande
```
Solución: Usar App Bundle o solo incluir ARM64
./gradlew bundleRelease  # Genera AAB en lugar de APK
```

### Problema 4: yt-dlp no funciona
```
Solución: Verificar que Python está disponible
O usar versión compilada de yt-dlp
```

## 🎯 Alternativa: Descarga Bajo Demanda

En lugar de incluir binarios en el APK, puedes descargarlos la primera vez:

```kotlin
class BinaryDownloader(context: Context) {
    suspend fun downloadBinaries() {
        // Descargar FFmpeg
        downloadFile(
            "https://github.com/arthenica/ffmpeg-kit/releases/...",
            ffmpegBinary
        )
        
        // Descargar yt-dlp
        downloadFile(
            "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp",
            ytdlpBinary
        )
    }
}
```

**Ventajas:**
- APK mucho más pequeño (~5-10 MB)
- Siempre última versión de yt-dlp

**Desventajas:**
- Requiere conexión Internet en primer uso
- Mayor complejidad

## 📚 Referencias

- FFmpeg Kit: https://github.com/arthenica/ffmpeg-kit
- yt-dlp: https://github.com/yt-dlp/yt-dlp
- Python for Android: https://github.com/kivy/python-for-android
- Termux: https://github.com/termux/termux-app

## ✨ Resultado Final

Con los binarios correctamente instalados:

✅ Descarga directa desde YouTube (yt-dlp)
✅ Conversión de audio profesional (FFmpeg)
✅ Sin dependencias de librerías deprecadas
✅ Control total sobre versiones
✅ Actualizaciones fáciles (solo reemplazar binarios)

---

**Nota:** Por limitaciones de tamaño, los binarios NO están incluidos en este proyecto. Debes descargarlos siguiendo esta guía.

**Recomendación:** Empieza solo con ARM64 para probar, luego agrega otras arquitecturas si es necesario.
