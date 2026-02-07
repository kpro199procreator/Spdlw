package com.spotdl.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Terminal/CLI Screen - Estilo retro hacker
 */
@Composable
fun TerminalScreen() {
    val terminalState = remember { TerminalState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll al final cuando se agregan nuevos mensajes
    LaunchedEffect(terminalState.messages.size) {
        if (terminalState.messages.isNotEmpty()) {
            listState.animateScrollToItem(terminalState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header de terminal
            TerminalHeader(terminalState)

            // Área de output
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState
            ) {
                // Mensaje de bienvenida
                if (terminalState.messages.isEmpty()) {
                    item {
                        WelcomeMessage()
                    }
                }

                // Mensajes del terminal
                items(terminalState.messages) { message ->
                    TerminalMessage(message)
                }
            }

            // Input de comandos
            TerminalInput(
                onCommand = { command ->
                    scope.launch {
                        terminalState.executeCommand(command)
                    }
                }
            )
        }

        // Indicador de procesando
        if (terminalState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                ProcessingIndicator()
            }
        }
    }
}

@Composable
fun TerminalHeader(state: TerminalState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TerminalColors.headerBackground,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = TerminalColors.green,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "SpotDL Terminal",
                        color = TerminalColors.green,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "v1.0.0 | Session: ${state.sessionId.take(8)}",
                        color = TerminalColors.green.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Status indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusIndicator("CPU", state.cpuUsage)
                StatusIndicator("MEM", state.memUsage)
            }
        }
    }
}

@Composable
fun StatusIndicator(label: String, value: Int) {
    val color = when {
        value < 50 -> TerminalColors.green
        value < 80 -> TerminalColors.yellow
        else -> TerminalColors.red
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TerminalColors.green.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = "$value%",
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WelcomeMessage() {
    Column(
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        val asciiArt = """
            ╔═══════════════════════════════════════╗
            ║     SpotDL Terminal Interface v1.0    ║
            ║     Advanced Music Download System    ║
            ╚═══════════════════════════════════════╝
        """.trimIndent()

        Text(
            text = asciiArt,
            color = TerminalColors.cyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val helpText = """
            Welcome to SpotDL Terminal Interface
            Type 'help' for available commands
            Type 'status' for system information
            Type 'download <url>' to download a song
            
            Ready to process commands...
        """.trimIndent()

        Text(
            text = helpText,
            color = TerminalColors.green,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun TerminalMessage(message: TerminalMessage) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Timestamp
            Text(
                text = message.timestamp,
                color = TerminalColors.gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Prompt prefix
            Text(
                text = if (message.isCommand) "$ " else "> ",
                color = if (message.isCommand) TerminalColors.cyan else TerminalColors.green,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Message content
            Text(
                text = message.content,
                color = when (message.type) {
                    MessageType.ERROR -> TerminalColors.red
                    MessageType.WARNING -> TerminalColors.yellow
                    MessageType.SUCCESS -> TerminalColors.green
                    MessageType.INFO -> TerminalColors.cyan
                    else -> TerminalColors.white
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TerminalInput(onCommand: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val cursorBlink = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by cursorBlink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TerminalColors.inputBackground,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "spotdl@terminal:~$",
                color = TerminalColors.green,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        color = TerminalColors.white,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row {
                            innerTextField()
                            Text(
                                text = "█",
                                color = TerminalColors.green.copy(alpha = cursorAlpha),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                )
            }

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onCommand(text)
                        text = ""
                    }
                }
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Execute",
                    tint = TerminalColors.green
                )
            }
        }
    }
}

@Composable
fun ProcessingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            color = TerminalColors.green,
            strokeWidth = 3.dp
        )

        Text(
            text = "Processing...",
            color = TerminalColors.green,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Terminal State Management
class TerminalState {
    var messages by mutableStateOf<List<TerminalMessage>>(emptyList())
        private set

    var isProcessing by mutableStateOf(false)
        private set

    val sessionId = UUID.randomUUID().toString()
    var cpuUsage by mutableStateOf((10..30).random())
        private set
    var memUsage by mutableStateOf((20..40).random())
        private set

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun executeCommand(command: String) {
        // Agregar comando a mensajes
        addMessage(
            TerminalMessage(
                content = command,
                type = MessageType.COMMAND,
                isCommand = true,
                timestamp = currentTime()
            )
        )

        isProcessing = true
        delay(300) // Simular procesamiento

        // Procesar comando
        when {
            command.equals("help", ignoreCase = true) -> showHelp()
            command.equals("status", ignoreCase = true) -> showStatus()
            command.equals("clear", ignoreCase = true) -> clearScreen()
            command.startsWith("download", ignoreCase = true) -> processDownload(command)
            command.startsWith("search", ignoreCase = true) -> processSearch(command)
            command.equals("version", ignoreCase = true) -> showVersion()
            command.equals("config", ignoreCase = true) -> showConfig()
            else -> addMessage(
                TerminalMessage(
                    content = "Unknown command: '$command'. Type 'help' for available commands.",
                    type = MessageType.ERROR
                )
            )
        }

        // Actualizar stats aleatoriamente
        cpuUsage = (cpuUsage + (-5..5).random()).coerceIn(10, 90)
        memUsage = (memUsage + (-3..3).random()).coerceIn(20, 80)

        isProcessing = false
    }

    private suspend fun showHelp() {
        val helpCommands = listOf(
            "Available Commands:",
            "",
            "  help              - Show this help message",
            "  status            - Show system status",
            "  version           - Show version information",
            "  config            - Show configuration",
            "  clear             - Clear terminal screen",
            "  download <url>    - Download song from URL",
            "  search <query>    - Search for songs",
            "  exit              - Close terminal",
            ""
        )

        helpCommands.forEach { line ->
            addMessage(TerminalMessage(content = line, type = MessageType.INFO))
            delay(30)
        }
    }

    private suspend fun showStatus() {
        addMessage(TerminalMessage(content = "System Status:", type = MessageType.INFO))
        delay(100)
        addMessage(TerminalMessage(content = "  Session ID: $sessionId", type = MessageType.INFO))
        delay(50)
        addMessage(TerminalMessage(content = "  CPU Usage: $cpuUsage%", type = MessageType.SUCCESS))
        delay(50)
        addMessage(TerminalMessage(content = "  Memory Usage: $memUsage%", type = MessageType.SUCCESS))
        delay(50)
        addMessage(TerminalMessage(content = "  Status: ONLINE", type = MessageType.SUCCESS))
    }

    private suspend fun processDownload(command: String) {
        val url = command.substringAfter("download").trim()
        if (url.isEmpty()) {
            addMessage(TerminalMessage(content = "Usage: download <url>", type = MessageType.ERROR))
            return
        }

        addMessage(TerminalMessage(content = "Initializing download...", type = MessageType.INFO))
        delay(300)
        
        // Aquí se conectaría con el ViewModel/Repository para descarga real
        // Por ahora simulamos el proceso
        addMessage(TerminalMessage(content = "Fetching metadata from $url", type = MessageType.INFO))
        delay(500)
        addMessage(TerminalMessage(content = "Searching on YouTube...", type = MessageType.INFO))
        delay(500)
        addMessage(TerminalMessage(content = "Download started - Track added to queue", type = MessageType.SUCCESS))
        addMessage(TerminalMessage(content = "Check Downloads tab for progress", type = MessageType.INFO))
    }

    private suspend fun processSearch(command: String) {
        val query = command.substringAfter("search").trim()
        if (query.isEmpty()) {
            addMessage(TerminalMessage(content = "Usage: search <query>", type = MessageType.ERROR))
            return
        }

        addMessage(TerminalMessage(content = "Searching for: $query", type = MessageType.INFO))
        delay(400)
        addMessage(TerminalMessage(content = "Found 10 results - check main screen", type = MessageType.SUCCESS))
    }

    private fun showVersion() {
        addMessage(TerminalMessage(content = "SpotDL Terminal v1.0.0", type = MessageType.INFO))
        addMessage(TerminalMessage(content = "Build: 2024.02.05", type = MessageType.INFO))
    }

    private fun showConfig() {
        addMessage(TerminalMessage(content = "Configuration:", type = MessageType.INFO))
        addMessage(TerminalMessage(content = "  Format: MP3 (320kbps)", type = MessageType.INFO))
        addMessage(TerminalMessage(content = "  Metadata: Enabled", type = MessageType.SUCCESS))
        addMessage(TerminalMessage(content = "  Artwork: Enabled", type = MessageType.SUCCESS))
    }

    private fun clearScreen() {
        messages = emptyList()
    }

    private fun addMessage(message: TerminalMessage) {
        messages = messages + message
    }

    private fun currentTime(): String = timeFormat.format(Date())
}

data class TerminalMessage(
    val content: String,
    val type: MessageType = MessageType.NORMAL,
    val isCommand: Boolean = false,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

enum class MessageType {
    NORMAL, COMMAND, ERROR, WARNING, SUCCESS, INFO
}

object TerminalColors {
    val background = Color(0xFF0A0E14)
    val headerBackground = Color(0xFF14191F)
    val inputBackground = Color(0xFF1A1F26)
    val white = Color(0xFFE6E6E6)
    val green = Color(0xFF00FF00)
    val cyan = Color(0xFF00FFFF)
    val yellow = Color(0xFFFFFF00)
    val red = Color(0xFFFF0055)
    val gray = Color(0xFF808080)
}
