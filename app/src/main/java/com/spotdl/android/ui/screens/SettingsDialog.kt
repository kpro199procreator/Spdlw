package com.spotdl.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spotdl.android.data.model.AudioFormat
import com.spotdl.android.data.model.AudioQuality
import com.spotdl.android.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val config by viewModel.downloadConfig.collectAsState()
    
    var selectedFormat by remember { mutableStateOf(config.format) }
    var selectedQuality by remember { mutableStateOf(config.quality) }
    var embedArtwork by remember { mutableStateOf(config.embedArtwork) }
    var embedMetadata by remember { mutableStateOf(config.embedMetadata) }
    var filenameTemplate by remember { mutableStateOf(config.filenameTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Formato de audio
                Text(
                    text = "Formato de audio",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = format.extension.uppercase(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Calidad de audio
                Text(
                    text = "Calidad de audio",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                AudioQuality.values().forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedQuality == quality,
                            onClick = { selectedQuality = quality }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = getQualityLabel(quality),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = quality.bitrate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Opciones de metadatos
                Text(
                    text = "Metadatos",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Incrustar portada del álbum")
                    Switch(
                        checked = embedArtwork,
                        onCheckedChange = { embedArtwork = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Incrustar metadatos")
                    Switch(
                        checked = embedMetadata,
                        onCheckedChange = { embedMetadata = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Plantilla de nombre de archivo
                Text(
                    text = "Plantilla de nombre de archivo",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = filenameTemplate,
                    onValueChange = { filenameTemplate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Plantilla") },
                    supportingText = {
                        Text("Variables: {artist}, {title}, {album}, {year}")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateAudioFormat(selectedFormat)
                            viewModel.updateAudioQuality(selectedQuality)
                            viewModel.updateEmbedArtwork(embedArtwork)
                            viewModel.updateEmbedMetadata(embedMetadata)
                            viewModel.updateFilenameTemplate(filenameTemplate)
                            onDismiss()
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

fun getQualityLabel(quality: AudioQuality): String {
    return when (quality) {
        AudioQuality.LOW -> "Baja"
        AudioQuality.MEDIUM -> "Media"
        AudioQuality.HIGH -> "Alta"
        AudioQuality.VERY_HIGH -> "Muy Alta"
    }
}
