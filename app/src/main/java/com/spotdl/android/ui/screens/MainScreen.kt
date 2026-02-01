package com.spotdl.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spotdl.android.data.model.DownloadProgress
import com.spotdl.android.data.model.DownloadStatus
import com.spotdl.android.data.model.Song
import com.spotdl.android.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpotDL") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pestañas
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Búsqueda") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Descargas") },
                    icon = { 
                        Badge(
                            containerColor = if (downloads.isNotEmpty()) 
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(downloads.size.toString())
                        }
                    }
                )
            }

            // Contenido según pestaña seleccionada
            when (selectedTab) {
                0 -> SearchTab(
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSearch = viewModel::search,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onDownloadSong = viewModel::downloadSong
                )
                1 -> DownloadsTab(
                    downloads = downloads,
                    onCancelDownload = viewModel::cancelDownload,
                    onClearCompleted = viewModel::clearCompletedDownloads
                )
            }
        }

        // Diálogo de configuración
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Snackbar para errores
        error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Mostrar snackbar
                viewModel.clearError()
            }
        }

        // Snackbar para mensajes
        message?.let { msg ->
            LaunchedEffect(msg) {
                // Mostrar snackbar
                viewModel.clearMessage()
            }
        }
    }
}

@Composable
fun SearchTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResults: List<Song>,
    isSearching: Boolean,
    onDownloadSong: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar canción o pegar URL de Spotify") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch() }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotEmpty() && !isSearching
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSearching) "Buscando..." else "Buscar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resultados de búsqueda
        if (searchResults.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { song ->
                    SongCard(
                        song = song,
                        onDownload = { onDownloadSong(song) }
                    )
                }
            }
        } else if (!isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Busca una canción o pega una URL de Spotify",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SongCard(
    song: Song,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = "Portada del álbum",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información de la canción
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                song.album?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                song.duration?.let {
                    Text(
                        text = formatDuration(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botón de descarga
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DownloadsTab(
    downloads: List<DownloadProgress>,
    onCancelDownload: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Botón para limpiar completadas
        if (downloads.any { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }) {
            TextButton(
                onClick = onClearCompleted,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpiar completadas")
            }
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay descargas en curso",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads) { download ->
                    DownloadCard(
                        download = download,
                        onCancel = { onCancelDownload(download.song.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    download: DownloadProgress,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Icono de estado
                when (download.status) {
                    DownloadStatus.PENDING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    DownloadStatus.DOWNLOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    DownloadStatus.PROCESSING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    DownloadStatus.COMPLETED -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completado",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    DownloadStatus.FAILED -> Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    DownloadStatus.CANCELLED -> Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancelado",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botón cancelar si está en progreso
                if (download.status == DownloadStatus.DOWNLOADING || 
                    download.status == DownloadStatus.PROCESSING) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progreso
            if (download.status == DownloadStatus.DOWNLOADING || 
                download.status == DownloadStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = download.currentStep,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mensaje de error
            download.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
