# 🔧 Troubleshooting GitHub Actions - SpotDL Android

## ❌ Problema Reportado

```
Kotlin DSL script compilation is not up-to-date
Se queda colgado durante la compilación
```

## ✅ Soluciones Aplicadas

### 1. Repositorio Maven Inválido Eliminado

**Problema:**
```kotlin
maven { url = uri("https://github.com/arthenica/ffmpeg-kit/releases/download/v6.0/") }
```
Esta URL NO es un repositorio Maven válido.

**Solución:**
```kotlin
// settings.gradle.kts - CORRECTO
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

### 2. DefaultConfig Duplicado Corregido

**Problema:**
```kotlin
// app/build.gradle.kts - INCORRECTO
android {
    defaultConfig { ... }
    
    // Más abajo...
    defaultConfig { ... }  // ❌ Duplicado
}
```

**Solución:**
```kotlin
// app/build.gradle.kts - CORRECTO
android {
    defaultConfig {
        applicationId = "com.spotdl.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        // NDK config en el mismo bloque
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }
}
```

### 3. Gradle Properties Optimizado para CI

**Cambios aplicados:**
```properties
# Deshabilitar daemon en CI
org.gradle.daemon=false

# Deshabilitar configuration cache (problemas en CI)
org.gradle.configuration-cache=false

# Habilitar builds paralelos
org.gradle.parallel=true

# Aumentar timeouts de red
systemProp.http.socketTimeout=60000
systemProp.http.connectionTimeout=60000
```

### 4. GitHub Actions Workflow Creado

**Archivo:** `.github/workflows/android-ci.yml`

**Características:**
- ✅ Cache de Gradle
- ✅ Timeout de 30 minutos
- ✅ Build sin daemon
- ✅ Flags de stacktrace
- ✅ Upload de APKs

## 🚀 Cómo Usar en GitHub

### Configuración Inicial

1. **Subir el proyecto a GitHub**
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/kpro199procreator/Spdlw.git
git push -u origin main
```

2. **El workflow se ejecuta automáticamente** en:
   - Push a `main`, `master`, o `develop`
   - Pull requests
   - Manualmente desde Actions tab

### Ver Resultados

1. Ve a tu repositorio en GitHub
2. Click en tab "Actions"
3. Verás el workflow corriendo
4. Click en el run para ver logs detallados

### Descargar APKs

1. En el run completado, baja a "Artifacts"
2. Descarga `app-debug.apk` o `app-release-unsigned.apk`

## 🔍 Comandos de Diagnóstico

### Verificar Configuración Local

```bash
# Limpiar
./gradlew clean

# Build con logs detallados
./gradlew build --stacktrace --info

# Build sin daemon (como CI)
./gradlew build --no-daemon --stacktrace

# Verificar dependencias
./gradlew dependencies
```

### Simular Comportamiento de CI

```bash
# Deshabilitar daemon temporalmente
./gradlew build --no-daemon \
  --stacktrace \
  -Dorg.gradle.jvmargs=-Xmx2048m \
  -Dorg.gradle.caching=true
```

## ⚠️ Problemas Comunes en CI

### Problema 1: "Connection timeout"

**Síntoma:**
```
> Could not download ...
> Connection timed out
```

**Solución:**
```properties
# gradle.properties
systemProp.http.socketTimeout=60000
systemProp.http.connectionTimeout=60000
```

### Problema 2: "Out of memory"

**Síntoma:**
```
> Expiring Daemon because JVM heap space is exhausted
```

**Solución:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

**O en GitHub Actions:**
```yaml
env:
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4096m"
```

### Problema 3: "Daemon disappeared unexpectedly"

**Síntoma:**
```
> Gradle build daemon disappeared unexpectedly
```

**Solución:**
```bash
# En CI, siempre usar --no-daemon
./gradlew build --no-daemon
```

### Problema 4: "Configuration cache problems"

**Síntoma:**
```
> Configuration cache problems found
```

**Solución:**
```properties
# gradle.properties
org.gradle.configuration-cache=false
```

### Problema 5: "Stuck at 'Configure project'"

**Síntoma:**
```
> Configure project :
[Se queda aquí indefinidamente]
```

**Causas comunes:**
1. Repositorio Maven inválido ✅ CORREGIDO
2. defaultConfig duplicado ✅ CORREGIDO
3. Dependencia inexistente
4. Red lenta

**Solución:**
```bash
# Verificar con modo verbose
./gradlew build --debug > gradle-debug.log 2>&1

# Buscar donde se atora
grep -n "Configure project" gradle-debug.log
```

## 📊 Configuración Recomendada de GitHub Actions

### Workflow Básico (Actual)
```yaml
- Build Debug APK
- Run tests (opcional)
- Upload artifacts
```

### Workflow Avanzado (Opcional)
```yaml
- Build Debug + Release
- Run Unit Tests
- Run UI Tests (con emulador)
- Lint checks
- Upload a Google Play (si firmado)
```

## 🎯 Optimizaciones para CI

### 1. Cache Agresivo
```yaml
- name: Cache Gradle
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
      ~/.android/build-cache
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
```

### 2. Build Paralelo
```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### 3. Timeouts Apropiados
```yaml
jobs:
  build:
    timeout-minutes: 30  # 30 min máximo
```

### 4. Dependencias Específicas
```kotlin
// No usar 'latest' o '+'
implementation("androidx.core:core-ktx:1.12.0")  // ✅ Específica
// implementation("androidx.core:core-ktx:+")    // ❌ Evitar
```

## 🧪 Testing Local de CI

Puedes usar **act** para ejecutar GitHub Actions localmente:

```bash
# Instalar act
brew install act  # macOS
# o
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Ejecutar workflow local
act push

# Ejecutar job específico
act -j build
```

## 📝 Checklist Pre-Push

Antes de hacer push, verifica:

- [ ] `./gradlew clean build` funciona localmente
- [ ] `./gradlew build --no-daemon` funciona
- [ ] No hay dependencias con versión '+'
- [ ] Todos los repositorios Maven son válidos
- [ ] No hay configuraciones duplicadas
- [ ] `gradle.properties` está configurado para CI

## 🆘 Si Aún Tienes Problemas

### 1. Generar Reporte Detallado
```bash
./gradlew build --stacktrace --info > build-log.txt 2>&1
```

### 2. Revisar en GitHub
- Ve a Actions → Run fallido → Click en step
- Expande los logs completos
- Busca "FAILURE" o "ERROR"

### 3. Issues Conocidos

**Kotlin DSL compilation timeout:**
- Gradle puede tardar en compilar scripts Kotlin
- Primer build siempre es más lento
- Cache ayuda en builds subsecuentes

**Android SDK missing:**
```yaml
# GitHub Actions ya tiene SDK, pero verifica versión
- name: Set up Android SDK
  uses: android-actions/setup-android@v2
  with:
    api-level: 34
    build-tools: 34.0.0
```

## 📚 Referencias

- [GitHub Actions for Android](https://github.com/actions/setup-java)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle Daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html)
- [Android CI Best Practices](https://developer.android.com/studio/build/building-cmdline)

## ✅ Estado Actual

Con las correcciones aplicadas:

✅ Repositorios Maven válidos
✅ Configuración sin duplicados
✅ Gradle optimizado para CI
✅ Workflow de GitHub Actions incluido
✅ Timeouts configurados
✅ Cache habilitado

**El proyecto debería compilar correctamente en GitHub Actions.**

---

Si el problema persiste, comparte los logs completos del step donde falla.
