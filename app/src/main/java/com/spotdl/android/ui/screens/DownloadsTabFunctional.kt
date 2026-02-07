package com.spotdl.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotdl.android.data.model.*
import com.spotdl.android.ui.viewmodel.MainViewModel

/**
 * Tab de descargas con animaciones en tiempo real
 */
@Composable
fun DownloadsTabFunctional(viewModel: MainViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val stats = viewModel.getStats()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E14),
                        Color(0xFF1A1F26)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con estadísticas
            DownloadsHeader(
                stats = stats,
                activeDownloads = activeDownloads,
                onClearCompleted = { viewModel.clearCompletedDownloads() }
            )
            
            // Lista de descargas
            if (downloads.isEmpty()) {
                EmptyDownloadsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = downloads,
                        key = { it.downloadId }
                    ) { download ->
                        DownloadCard(
                            download = download,
                            onCancel = { viewModel.cancelDownload(download.downloadId) },
                            onRetry = { viewModel.retryDownload(download.downloadId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsHeader(
    stats: com.spotdl.android.ui.viewmodel.DownloadStats,
    activeDownloads: Int,
    onClearCompleted: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título y acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Descargas",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    AnimatedContent(
                        targetState = activeDownloads,
                        label = "active_downloads"
                    ) { count ->
                        Text(
                            text = if (count > 0) "$count descargando" else "Todo listo",
                            color = if (count > 0) Color(0xFF1DB954) else Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = if (count > 0) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
                
                if (stats.completed > 0) {
                    TextButton(onClick = onClearCompleted) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Limpiar",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total",
                    value = stats.total.toString(),
                    color = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    label = "Completadas",
                    value = stats.completed.toString(),
                    color = Color(0xFF00FFAA),
                    modifier = Modifier.weight(1f)
                )
                
                if (stats.failed > 0) {
                    StatCard(
                        label = "Fallidas",
                        value = stats.failed.toString(),
                        color = Color(0xFFFF0055),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DownloadCard(
    download: DownloadProgress,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Artwork
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    if (download.song.artworkUrl != null) {
                        AsyncImage(
                            model = download.song.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    
                    // Status overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when (download.status) {
                                    DownloadStatus.DOWNLOADING, 
                                    DownloadStatus.PROCESSING -> Color.Black.copy(alpha = 0.3f)
                                    DownloadStatus.COMPLETED -> Color(0xFF00FFAA).copy(alpha = 0.3f)
                                    DownloadStatus.FAILED -> Color(0xFFFF0055).copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (download.status) {
                            DownloadStatus.COMPLETED -> Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            DownloadStatus.FAILED -> Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            else -> {}
                        }
                    }
                }
                
                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = download.song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = download.song.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Current step/error
                    AnimatedContent(
                        targetState = download.currentStep,
                        label = "current_step"
                    ) { step ->
                        Text(
                            text = when (download.status) {
                                DownloadStatus.FAILED -> download.errorMessage ?: "Error"
                                else -> step
                            },
                            color = when (download.status) {
                                DownloadStatus.FAILED -> Color(0xFFFF0055)
                                DownloadStatus.COMPLETED -> Color(0xFF00FFAA)
                                else -> Color(0xFF1DB954)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Action button
                when (download.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PROCESSING -> {
                        IconButton(onClick = onCancel) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reintentar",
                                tint = Color(0xFFFF0055)
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            // Progress bar
            if (download.status == DownloadStatus.DOWNLOADING || 
                download.status == DownloadStatus.PROCESSING) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = download.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF1DB954),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(download.progress * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        
                        if (download.estimatedTimeRemaining != null) {
                            Text(
                                text = formatTime(download.estimatedTimeRemaining),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF1DB954).copy(alpha = 0.3f)
            )
            
            Text(
                "No hay descargas",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Las canciones que descargues aparecerán aquí",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    return if (seconds < 60) {
        "${seconds}s"
    } else {
        val minutes = seconds / 60
        val secs = seconds % 60
        "${minutes}m ${secs}s"
    }
}
