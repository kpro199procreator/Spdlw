package com.spotdl.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.DOWNLOAD) }
    var showApiDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsTopBar(onDismiss = onDismiss)
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sidebar de categorías
            SettingsSidebar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // Contenido de configuración
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedCategory) {
                    SettingsCategory.DOWNLOAD -> DownloadSettings()
                    SettingsCategory.AUDIO -> AudioSettings()
                    SettingsCategory.APPEARANCE -> AppearanceSettings()
                    SettingsCategory.ADVANCED -> AdvancedSettings(
                        onApiConfig = { showApiDialog = true }
                    )
                    SettingsCategory.ABOUT -> AboutSettings()
                }
            }
        }
    }

    // Dialog de configuración de API
    if (showApiDialog) {
        ApiConfigDialog(onDismiss = { showApiDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onDismiss: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Configuración Avanzada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Cerrar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SettingsSidebar(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight(),
        tonalElevation = 1.dp
    ) {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(SettingsCategory.entries.size) { index ->
                val category = SettingsCategory.entries[index]
                CategoryItem(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer
        else 
            Color.Transparent,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = category.title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DownloadSettings() {
    SettingsContent(
        title = "Configuración de Descargas",
        description = "Personaliza cómo se descargan y almacenan las canciones"
    ) {
        var simultaneousDownloads by remember { mutableStateOf(3f) }
        var autoRetry by remember { mutableStateOf(true) }
        var wifiOnly by remember { mutableStateOf(false) }

        SettingSlider(
            label = "Descargas Simultáneas",
            value = simultaneousDownloads,
            onValueChange = { simultaneousDownloads = it },
            valueRange = 1f..10f,
            steps = 8,
            valueLabel = "${simultaneousDownloads.toInt()} canciones"
        )

        SettingSwitch(
            label = "Reintentar Automáticamente",
            description = "Reintentar descargas fallidas automáticamente",
            checked = autoRetry,
            onCheckedChange = { autoRetry = it },
            icon = Icons.Default.Refresh
        )

        SettingSwitch(
            label = "Solo WiFi",
            description = "Descargar solo cuando esté conectado a WiFi",
            checked = wifiOnly,
            onCheckedChange = { wifiOnly = it },
            icon = Icons.Default.Wifi
        )

        SettingAction(
            label = "Ubicación de Descargas",
            description = "/storage/emulated/0/Music/SpotDL",
            icon = Icons.Default.Folder,
            onClick = { }
        )
    }
}

@Composable
fun AudioSettings() {
    SettingsContent(
        title = "Configuración de Audio",
        description = "Ajusta la calidad y formato de audio"
    ) {
        var selectedFormat by remember { mutableStateOf("MP3") }
        var quality by remember { mutableStateOf(320f) }
        var embedMetadata by remember { mutableStateOf(true) }
        var embedArtwork by remember { mutableStateOf(true) }
        var normalizAudio by remember { mutableStateOf(false) }

        SettingSelector(
            label = "Formato de Audio",
            options = listOf("MP3", "M4A", "FLAC", "OGG", "WAV"),
            selectedOption = selectedFormat,
            onOptionSelected = { selectedFormat = it },
            icon = Icons.Default.MusicNote
        )

        SettingSlider(
            label = "Calidad (bitrate)",
            value = quality,
            onValueChange = { quality = it },
            valueRange = 128f..320f,
            steps = 3,
            valueLabel = "${quality.toInt()} kbps"
        )

        SettingSwitch(
            label = "Incrustar Metadatos",
            description = "Título, artista, álbum, etc.",
            checked = embedMetadata,
            onCheckedChange = { embedMetadata = it },
            icon = Icons.Default.Info
        )

        SettingSwitch(
            label = "Incrustar Portada",
            description = "Imagen del álbum en el archivo",
            checked = embedArtwork,
            onCheckedChange = { embedArtwork = it },
            icon = Icons.Default.Image
        )

        SettingSwitch(
            label = "Normalizar Volumen",
            description = "Ajustar volumen a -14 LUFS",
            checked = normalizeAudio,
            onCheckedChange = { normalizeAudio = it },
            icon = Icons.Default.VolumeUp
        )
    }
}

@Composable
fun AppearanceSettings() {
    SettingsContent(
        title = "Apariencia",
        description = "Personaliza la interfaz de la aplicación"
    ) {
        var selectedTheme by remember { mutableStateOf("System") }
        var useDynamicColors by remember { mutableStateOf(true) }
        var showAnimations by remember { mutableStateOf(true) }

        SettingSelector(
            label = "Tema",
            options = listOf("Light", "Dark", "System"),
            selectedOption = selectedTheme,
            onOptionSelected = { selectedTheme = it },
            icon = Icons.Default.Palette
        )

        SettingSwitch(
            label = "Colores Dinámicos",
            description = "Usar colores del sistema (Android 12+)",
            checked = useDynamicColors,
            onCheckedChange = { useDynamicColors = it },
            icon = Icons.Default.ColorLens
        )

        SettingSwitch(
            label = "Animaciones",
            description = "Habilitar animaciones y transiciones",
            checked = showAnimations,
            onCheckedChange = { showAnimations = it },
            icon = Icons.Default.Animation
        )
    }
}

@Composable
fun AdvancedSettings(onApiConfig: () -> Unit) {
    SettingsContent(
        title = "Configuración Avanzada",
        description = "Opciones para usuarios avanzados"
    ) {
        var debugMode by remember { mutableStateOf(false) }
        var developerMode by remember { mutableStateOf(false) }

        SettingAction(
            label = "Configurar APIs",
            description = "Spotify API, YouTube API",
            icon = Icons.Default.Key,
            onClick = onApiConfig
        )

        SettingSwitch(
            label = "Modo Debug",
            description = "Mostrar logs detallados",
            checked = debugMode,
            onCheckedChange = { debugMode = it },
            icon = Icons.Default.BugReport
        )

        SettingSwitch(
            label = "Modo Desarrollador",
            description = "Habilitar funciones experimentales",
            checked = developerMode,
            onCheckedChange = { developerMode = it },
            icon = Icons.Default.DeveloperMode
        )

        SettingAction(
            label = "Limpiar Cache",
            description = "Liberar espacio eliminando archivos temporales",
            icon = Icons.Default.Delete,
            onClick = { }
        )

        SettingAction(
            label = "Exportar Configuración",
            description = "Guardar configuración en archivo",
            icon = Icons.Default.Download,
            onClick = { }
        )
    }
}

@Composable
fun AboutSettings() {
    SettingsContent(
        title = "Acerca de",
        description = "Información de la aplicación"
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    "SpotDL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    "Versión 1.0.0",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                InfoRow("Desarrollador", "Tu Nombre")
                InfoRow("Licencia", "MIT License")
                InfoRow("Código Fuente", "GitHub")
            }
        }

        SettingAction(
            label = "Términos y Condiciones",
            icon = Icons.Default.Description,
            onClick = { }
        )

        SettingAction(
            label = "Política de Privacidad",
            icon = Icons.Default.Shield,
            onClick = { }
        )

        SettingAction(
            label = "Reportar un Bug",
            icon = Icons.Default.BugReport,
            onClick = { }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// Componentes reutilizables
@Composable
fun SettingsContent(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingSwitch(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (description != null) {
                        Text(
                            description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    valueLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
fun SettingSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    icon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        onClick = { expanded = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        selectedOption,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selectedOption) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun SettingAction(
    label: String,
    description: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ApiConfigDialog(onDismiss: () -> Unit) {
    var spotifyClientId by remember { mutableStateOf("") }
    var spotifyClientSecret by remember { mutableStateOf("") }
    var youtubeApiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Key, null)
                Text("Configurar APIs")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Configura tus claves de API para acceder a Spotify y YouTube",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = spotifyClientId,
                    onValueChange = { spotifyClientId = it },
                    label = { Text("Spotify Client ID") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                )

                OutlinedTextField(
                    value = spotifyClientSecret,
                    onValueChange = { spotifyClientSecret = it },
                    label = { Text("Spotify Client Secret") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) }
                )

                OutlinedTextField(
                    value = youtubeApiKey,
                    onValueChange = { youtubeApiKey = it },
                    label = { Text("YouTube API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.VideoLibrary, null) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    DOWNLOAD("Descargas", Icons.Default.Download),
    AUDIO("Audio", Icons.Default.AudioFile),
    APPEARANCE("Apariencia", Icons.Default.Palette),
    ADVANCED("Avanzado", Icons.Default.Settings),
    ABOUT("Acerca de", Icons.Default.Info)
}
