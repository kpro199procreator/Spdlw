package com.spotdl.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spotdl.android.data.model.*
import com.spotdl.android.ui.viewmodel.SetupViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val setupState by viewModel.setupState.collectAsState()
    val binaryStates by viewModel.binaryStates.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        when (setupState) {
            SetupState.CHECKING -> CheckingScreen()
            SetupState.NEEDS_SETUP -> SetupWelcomeScreen(viewModel)
            SetupState.INSTALLING -> InstallingScreen(viewModel, binaryStates, overallProgress)
            SetupState.COMPLETED -> CompletedScreen(onSetupComplete)
            SetupState.ERROR -> ErrorScreen(viewModel)
            SetupState.UPDATES_AVAILABLE -> UpdatesScreen(viewModel)
            SetupState.READY -> {
                LaunchedEffect(Unit) {
                    onSetupComplete()
                }
            }
        }
    }
}

@Composable
fun CheckingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Verificando componentes...",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun SetupWelcomeScreen(viewModel: SetupViewModel) {
    val installMode by viewModel.installMode.collectAsState()
    val binariesToInstall by viewModel.binariesToInstall.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val availableSources by viewModel.availableSources.collectAsState()
    
    var showSourceDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "🎵 Configuración Inicial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Configuremos los componentes necesarios para SpotDL",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StepIndicator(1, "Modo", currentStep >= 0, currentStep > 0)
            StepIndicator(2, "Fuente", currentStep >= 1, currentStep > 1)
            StepIndicator(3, "Revisar", currentStep >= 2, currentStep > 2)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                0 -> InstallModeStep(installMode, viewModel)
                1 -> SourceSelectionStep(
                    selectedSource,
                    availableSources,
                    onSourceSelect = { viewModel.selectSource(it) },
                    onShowDialog = { showSourceDialog = true }
                )
                2 -> ReviewStep(binariesToInstall, selectedSource)
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Atrás")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        viewModel.startSetup()
                    }
                }
            ) {
                Text(if (currentStep < 2) "Siguiente" else "Instalar")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (currentStep < 2) Icons.Default.ArrowForward else Icons.Default.Download,
                    contentDescription = null
                )
            }
        }
    }

    // Diálogo de fuente personalizada
    if (showSourceDialog) {
        CustomSourceDialog(
            onDismiss = { showSourceDialog = false },
            onAdd = { url, name ->
                viewModel.addCustomSource(url, name)
                showSourceDialog = false
            }
        )
    }
}

@Composable
fun StepIndicator(number: Int, label: String, isActive: Boolean, isCompleted: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InstallModeStep(mode: InstallMode, viewModel: SetupViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Selecciona el modo de instalación",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ModeCard(
            title = "Solo Requeridos",
            description = "FFmpeg y yt-dlp (Recomendado)\n~50 MB",
            icon = Icons.Default.Star,
            isSelected = mode == InstallMode.REQUIRED_ONLY,
            onClick = { viewModel.setInstallMode(InstallMode.REQUIRED_ONLY) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ModeCard(
            title = "Recomendado",
            description = "FFmpeg, yt-dlp y Python\n~70 MB",
            icon = Icons.Default.ThumbUp,
            isSelected = mode == InstallMode.RECOMMENDED,
            onClick = { viewModel.setInstallMode(InstallMode.RECOMMENDED) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ModeCard(
            title = "Todos",
            description = "Todas las herramientas disponibles\n~80 MB",
            icon = Icons.Default.Inventory,
            isSelected = mode == InstallMode.ALL,
            onClick = { viewModel.setInstallMode(InstallMode.ALL) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ModeCard(
            title = "Personalizado",
            description = "Selecciona manualmente",
            icon = Icons.Default.Settings,
            isSelected = mode == InstallMode.CUSTOM,
            onClick = { viewModel.setInstallMode(InstallMode.CUSTOM) }
        )
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SourceSelectionStep(
    selectedSource: DownloadSource,
    availableSources: List<DownloadSource>,
    onSourceSelect: (DownloadSource) -> Unit,
    onShowDialog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Selecciona la fuente de descarga",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableSources) { source ->
                SourceCard(
                    source = source,
                    isSelected = source.id == selectedSource.id,
                    onClick = { onSourceSelect(source) }
                )
            }
            
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowDialog)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Agregar fuente personalizada")
                    }
                }
            }
        }
    }
}

@Composable
fun SourceCard(
    source: DownloadSource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = source.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ReviewStep(
    binariesToInstall: List<BinaryInfo>,
    source: DownloadSource
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Revisión final",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fuente: ${source.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Total a descargar: ${formatSize(binariesToInstall.sumOf { it.size })}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(binariesToInstall) { binary ->
                BinaryListItem(binary)
            }
        }
    }
}

@Composable
fun BinaryListItem(binary: BinaryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (binary.name) {
                    "ffmpeg" -> Icons.Default.AudioFile
                    "yt-dlp", "yt-dlp-android" -> Icons.Default.Download
                    else -> Icons.Default.Code
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = binary.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = formatSize(binary.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (binary.required) {
                AssistChip(
                    onClick = { },
                    label = { Text("Requerido", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
fun InstallingScreen(
    viewModel: SetupViewModel,
    binaryStates: Map<String, BinaryState>,
    overallProgress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        
        Text(
            text = "Instalando componentes...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progreso general
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp
            )
            
            Text(
                text = "${(overallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Lista de binarios con progreso individual
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(binaryStates.values.toList()) { state ->
                BinaryProgressItem(state)
            }
        }
    }
}

@Composable
fun BinaryProgressItem(state: BinaryState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.info.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                when (state.status) {
                    BinaryStatus.DOWNLOADING -> {
                        Text(
                            text = "${(state.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    BinaryStatus.INSTALLED -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    BinaryStatus.FAILED -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            if (state.status != BinaryStatus.NOT_INSTALLED) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { state.downloadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = state.status.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CompletedScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "¡Todo listo!",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Los componentes se han instalado correctamente.\nAhora puedes comenzar a descargar música.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun ErrorScreen(viewModel: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Error en la instalación",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Hubo un problema al instalar los componentes.\nPor favor, intenta nuevamente.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.retrySetup() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun UpdatesScreen(viewModel: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Update,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Actualizaciones disponibles",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Hay nuevas versiones de los componentes disponibles.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.startSetup() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar ahora")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = { viewModel.skipSetup() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Más tarde")
        }
    }
}

@Composable
fun CustomSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar fuente personalizada") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL base") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(url, name) },
                enabled = url.isNotEmpty() && name.isNotEmpty()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
