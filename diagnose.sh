#!/bin/bash

# Script de diagnóstico para problemas de compilación en GitHub Actions
# Este script ayuda a identificar problemas comunes

echo "🔍 SpotDL Android - Diagnóstico de Compilación"
echo "================================================"
echo ""

# 1. Verificar Gradle Wrapper
echo "📦 1. Verificando Gradle Wrapper..."
if [ -f "gradlew" ]; then
    echo "✅ gradlew encontrado"
    chmod +x gradlew
    ./gradlew --version
else
    echo "❌ gradlew NO encontrado"
    echo "   Ejecuta: gradle wrapper --gradle-version 8.2"
fi
echo ""

# 2. Verificar archivos de configuración
echo "📝 2. Verificando archivos de configuración..."
files=("build.gradle.kts" "settings.gradle.kts" "gradle.properties" "app/build.gradle.kts")
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file existe"
    else
        echo "❌ $file NO existe"
    fi
done
echo ""

# 3. Verificar sintaxis de Gradle files
echo "🔧 3. Verificando sintaxis de archivos Gradle..."
./gradlew help --dry-run 2>&1 | head -20
echo ""

# 4. Limpiar cache
echo "🧹 4. Limpiando cache de Gradle..."
./gradlew clean --no-daemon
echo ""

# 5. Intentar build con diagnóstico
echo "🏗️  5. Intentando build con información detallada..."
./gradlew build --stacktrace --info --no-daemon 2>&1 | tee build-diagnostic.log
echo ""

# 6. Resumen
echo "📊 6. Resumen del Diagnóstico"
echo "================================================"
if grep -q "BUILD SUCCESSFUL" build-diagnostic.log; then
    echo "✅ BUILD EXITOSO"
else
    echo "❌ BUILD FALLÓ"
    echo ""
    echo "Errores encontrados:"
    grep -i "error\|exception\|failed" build-diagnostic.log | head -10
fi
echo ""

echo "📄 Log completo guardado en: build-diagnostic.log"
echo ""
echo "Para GitHub Actions, revisa:"
echo "1. .github/workflows/android-ci.yml existe"
echo "2. Java 17 está configurado"
echo "3. Cache de Gradle está habilitado"
