package com.spotdl.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.spotdl.android.ui.screens.MainScreen
import com.spotdl.android.ui.screens.SetupScreen
import com.spotdl.android.ui.theme.SpotDLTheme
import com.spotdl.android.ui.viewmodel.MainViewModel
import com.spotdl.android.ui.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val setupViewModel: SetupViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // Mostrar diálogo explicando por qué se necesitan los permisos
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos necesarios
        requestNecessaryPermissions()

        // Manejar intent de compartir
        handleSharedIntent(intent)

        setContent {
            SpotDLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        setupViewModel = setupViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSharedIntent(it) }
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 y anteriores
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun handleSharedIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                // Verificar si es una URL de Spotify o YouTube
                if (sharedText.contains("spotify.com") || 
                    sharedText.contains("youtube.com") || 
                    sharedText.contains("youtu.be")) {
                    mainViewModel.processSharedUrl(sharedText)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    setupViewModel: SetupViewModel
) {
    // Verificar si el setup ya fue completado
    var showSetup by remember { mutableStateOf(!setupViewModel.isSetupCompleted()) }

    if (showSetup) {
        SetupScreen(
            viewModel = setupViewModel,
            onSetupComplete = {
                setupViewModel.markSetupCompleted()
                showSetup = false
            }
        )
    } else {
        MainScreen(viewModel = mainViewModel)
    }
}
