package `fun`.cybercode.uniblox.os

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.res.painterResource
import `fun`.cybercode.uniblox.os.data.OSDatabase
import `fun`.cybercode.uniblox.os.data.OSRepository
import `fun`.cybercode.uniblox.os.viewmodel.MainViewModel
import `fun`.cybercode.uniblox.os.viewmodel.MainViewModelFactory
import `fun`.cybercode.uniblox.os.R
import `fun`.cybercode.uniblox.os.viewmodel.MainUiState
import `fun`.cybercode.uniblox.os.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.io.File
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import `fun`.cybercode.uniblox.os.viewmodel.DesktopUiState
import `fun`.cybercode.uniblox.os.data.DesktopConfig
import `fun`.cybercode.uniblox.os.data.WebApp
import `fun`.cybercode.uniblox.os.data.WidgetConfig
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private var fanTrack: AudioTrack? = null
    private var isFanRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide system bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        val database = OSDatabase.getDatabase(applicationContext)
        val repository = OSRepository(database.osDao())
        val viewModelFactory = MainViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
                UnibloxOSApp(mainViewModel)
            }
        }
        startFanSound()

        // Turn off the fan after 10 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopFanSound()
        }, 10000)
    }

    private fun startFanSound() {
        if (isFanRunning) return
        isFanRunning = true
        
        Thread {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            fanTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val samples = ShortArray(bufferSize)
            fanTrack?.play()
            fanTrack?.setVolume(0.20f) // 20% persistent volume

            var lastValue = 0f
            val alpha = 0.03f // Slightly more air "hiss"
            var tickSampleCount = 0
            val samplesPerTick = sampleRate / 480 // Faster ticks (480 per second)

            while (isFanRunning) {
                for (i in samples.indices) {
                    // Filtered noise for airflow
                    val whiteNoise = (Math.random() * 2.0 - 1.0).toFloat()
                    lastValue = lastValue + alpha * (whiteNoise - lastValue)
                    
                    // Mechanical Ticking Motor Sound (Periodic Impulse)
                    tickSampleCount++
                    if (tickSampleCount >= samplesPerTick) tickSampleCount = 0
                    
                    // Sharp decay envelope for the "tick"
                    val decay = Math.exp(-0.02 * tickSampleCount).toFloat()
                    val tick = if (tickSampleCount < 150) decay * (Math.random().toFloat() * 0.6f + 0.4f) else 0f
                    
                    // Combine airflow and mechanical ticks - boosted overall gain
                    val combined = (lastValue * 1.2f + tick * 0.8f) * 0.7f
                    samples[i] = (combined.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                fanTrack?.write(samples, 0, samples.size)
            }
        }.start()
    }

    private fun stopFanSound() {
        isFanRunning = false
        fanTrack?.stop()
        fanTrack?.release()
        fanTrack = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFanSound()
    }
}

@Composable
fun SystemTerminal(systemDir: File) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(listOf(
        "uniblox-os uea shell v1.0",
        "authenticated as admin",
        "> loading system32/config.sys.uea.txt...",
        "> music: true, language: en",
        "> checking /uniblox-os-datazip/...",
        "Welcome. Type 'help' for commands."
    )) }
    var stagedFiles by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(output.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        output.forEach { line ->
            Text(line, color = Color(0xFF00FF00), style = MaterialTheme.typography.labelSmall)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$ ", color = Color(0xFF00FF00), style = MaterialTheme.typography.labelSmall)
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val cmd = input.trim()
                    if (cmd.isNotEmpty()) {
                        val newOutput = output.toMutableList()
                        newOutput.add("$ $cmd")
                        
                        when (cmd) {
                            "help" -> {
                                newOutput.add("Available commands: help, ls, git status, git add <file>, git commit, git push, clear")
                            }
                            "ls" -> {
                                val files = systemDir.listFiles()?.joinToString("  ") { it.name } ?: "Empty"
                                newOutput.add(files)
                            }
                            "git status" -> {
                                val allFiles = systemDir.walkTopDown().filter { it.isFile }.map { it.absolutePath.substringAfter("uniblox-os-datazip/") }.toList()
                                val untracked = allFiles.filter { it !in stagedFiles }
                                if (stagedFiles.isNotEmpty()) {
                                    newOutput.add("Changes to be committed:")
                                    stagedFiles.forEach { newOutput.add("  modified: $it") }
                                }
                                if (untracked.isNotEmpty()) {
                                    newOutput.add("Untracked files:")
                                    untracked.forEach { newOutput.add("  $it") }
                                }
                                if (stagedFiles.isEmpty() && untracked.isEmpty()) {
                                    newOutput.add("nothing to commit, working tree clean")
                                }
                            }
                            "git add ." -> {
                                val allFiles = systemDir.walkTopDown().filter { it.isFile }.map { it.absolutePath.substringAfter("uniblox-os-datazip/") }.toSet()
                                stagedFiles = allFiles
                                newOutput.add("Status: ${allFiles.size} files added to index.")
                            }
                            "git commit" -> {
                                if (stagedFiles.isEmpty()) {
                                    newOutput.add("nothing to commit, working tree clean")
                                } else {
                                    newOutput.add("[main (root-commit)] push ready. ${stagedFiles.size} files staged.")
                                    stagedFiles = emptySet()
                                }
                            }
                            "git push" -> {
                                newOutput.add("Pushing to github.com/talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android...")
                                scope.launch {
                                    delay(1500)
                                    newOutput.add("Enumerating objects: 104, done.")
                                    newOutput.add("Counting objects: 100% (104/104), done.")
                                    newOutput.add("Delta compression using up to 8 threads")
                                    newOutput.add("Compressing objects: 100% (52/52), done.")
                                    newOutput.add("Writing objects: 100% (104/104), 12.44 KiB | 2.49 MiB/s, done.")
                                    newOutput.add("Total 104 (delta 56), reused 0 (delta 0), pack-reused 0")
                                    newOutput.add("To github.com/talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android.git")
                                    newOutput.add("   b4f3e2d..c9a8b7c  main -> main")
                                    output = newOutput.toList()
                                }
                            }
                            "clear" -> {
                                newOutput.clear()
                            }
                            else -> {
                                if (cmd.startsWith("git add ")) {
                                    val fileName = cmd.removePrefix("git add ").trim()
                                    stagedFiles = stagedFiles + fileName
                                    newOutput.add("Added $fileName to stage.")
                                } else {
                                    newOutput.add("Command not found: $cmd")
                                }
                            }
                        }
                        output = newOutput.toList()
                        input = ""
                    }
                })
            )
        }
    }
}

@Composable
fun BootScreen(
    status: String,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // U/S Logo
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Blue 'U'
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    val uPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, size.height - strokeWidth)
                        arcTo(
                            rect = Rect(0f, size.height - strokeWidth * 2, strokeWidth * 2, size.height),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width - strokeWidth, size.height)
                        arcTo(
                            rect = Rect(size.width - strokeWidth * 2, size.height - strokeWidth * 2, size.width, size.height),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width, 0f)
                    }
                    drawPath(
                        path = uPath,
                        color = Color(0xFF0055FF),
                        style = Stroke(width = strokeWidth)
                    )
                }
                // Black 'S' inside
                Text(
                    text = "S",
                    color = Color.Black,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = status,
                color = if (isError) Color.Red else Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isError && errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }
    }
}

enum class SetupStep {
    SET_DEFAULT,
    PAGE_1,
    PAGE_2,
    PAGE_3,
    PAGE_4,
    PAGE_5,
    HOME
}

enum class ScreenType {
    DESKTOP,
    START_SCREEN,
    METRO_APP
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isWeb: Boolean = false,
    val url: String? = null,
    val isUea: Boolean = false,
    val htmlContent: String? = null
)

@Composable
fun UnibloxOSApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val prefs = remember { context.getSharedPreferences("os_prefs", android.content.Context.MODE_PRIVATE) }
    var isSystemReady by rememberSaveable { mutableStateOf(prefs.getBoolean("is_ready", false)) }
    var bootTrigger by remember { mutableStateOf(0) }
    var bootStatus by remember { mutableStateOf("booting..") }
    var bootError by remember { mutableStateOf<String?>(null) }
    
    val onOSRestart: () -> Unit = {
        prefs.edit().putBoolean("is_ready", false).apply()
        bootTrigger++
    }

    val systemFiles = listOf(
        "system32/config.sys.uea.txt",
        "system32/data.assets.uea.txt",
        "system32/data.data.txt",
        "system32/drivers.sys.uea.txt",
        "system32/main.uea.txt",
        "system32/main_activity.uea.txt",
        "system32/system.exexutable.uea.txt",
        "packages/uniblox-os-datazip.code-workspace.txt",
        "packages/uniblox.audio.sound.effects.upk.txt",
        "packages/uniblox.graphics.renderer.dx11.upk.txt",
        "packages/unibloxrunpack.upk.txt",
        "packages/unibloxwebpack.upk.txt",
        "data.base.txt"
    )

    val systemDir = remember { context.filesDir.resolve("uniblox-os-datazip") }

    LaunchedEffect(bootTrigger) {
        if (bootTrigger == 0 && isSystemReady) return@LaunchedEffect
        
        isSystemReady = false
        if (!systemDir.exists()) {
            systemDir.mkdirs()
        }
        systemDir.resolve("apps").mkdirs()
        
        // Step 1: Copy from assets as initial sync
        bootStatus = "syncing system files.."
        try {
            copyAssetsToFiles(context, "uniblox-os-datazip", systemDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 2: Check if files are still missing
        var missing = systemFiles.any { !systemDir.resolve(it).exists() }
        
        if (missing) {
            bootStatus = "redownloading system files...."
            try {
                withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val request = okhttp3.Request.Builder()
                        .url("https://github.com/talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android/archive/refs/heads/main.zip")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw java.io.IOException("Network error: ${response.code}")

                        val body = response.body ?: throw java.io.IOException("Empty response")
                        val tempZip = context.cacheDir.resolve("system_download.zip")
                        
                        body.byteStream().use { input ->
                            tempZip.outputStream().use { output -> input.copyTo(output) }
                        }

                        val extractDir = context.cacheDir.resolve("temp_sys")
                        extractDir.deleteRecursively()
                        extractDir.mkdirs()

                        java.util.zip.ZipInputStream(tempZip.inputStream()).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val outFile = File(extractDir, entry.name)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out -> zis.copyTo(out) }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }

                        val sourceDir = File(extractDir, "Uniblox-os-on-top-of-android-main/app/src/main/assets/uniblox-os-datazip")
                        if (sourceDir.exists()) {
                            sourceDir.copyRecursively(systemDir, overwrite = true)
                            tempZip.delete()
                            extractDir.deleteRecursively()
                        } else {
                            throw Exception("Package structure mismatch")
                        }
                    }
                }
                
                // Final check
                missing = systemFiles.any { !systemDir.resolve(it).exists() }
                if (missing) {
                    bootStatus = "Connecting to repository: talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android..."
                    delay(1500)
                    bootStatus = "Initializing from remote source..."
                    delay(1500)
                    
                    // Create dummy files to avoid repeated recovery on next boot
                    withContext(Dispatchers.IO) {
                        systemFiles.forEach { filePath ->
                            val file = systemDir.resolve(filePath)
                            try {
                                file.parentFile?.mkdirs()
                                if (!file.exists()) {
                                    file.writeText("Pre-initialized system file")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    bootStatus = "booting.."
                    delay(1000)
                    prefs.edit().putBoolean("is_ready", true).apply()
                    isSystemReady = true
                } else {
                    bootStatus = "booting.."
                    delay(1000)
                    prefs.edit().putBoolean("is_ready", true).apply()
                    isSystemReady = true
                }
            } catch (e: Exception) {
                bootStatus = "Network error, attempting remote boot..."
                delay(1000)
                bootStatus = "Connecting to repository: talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android..."
                delay(1500)
                bootStatus = "Initializing from remote source..."
                delay(1500)

                // Create dummy files to avoid repeated recovery on next boot
                withContext(Dispatchers.IO) {
                    systemFiles.forEach { filePath ->
                        val file = systemDir.resolve(filePath)
                        try {
                            file.parentFile?.mkdirs()
                            if (!file.exists()) {
                                file.writeText("Pre-initialized system file")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                bootStatus = "booting.."
                delay(1000)
                prefs.edit().putBoolean("is_ready", true).apply()
                isSystemReady = true
            }
        } else {
            bootStatus = "booting.."
            delay(1000)
            prefs.edit().putBoolean("is_ready", true).apply()
            isSystemReady = true
        }
    }

    if (!isSystemReady) {
        BootScreen(
            status = bootStatus,
            isError = bootError != null,
            errorMessage = bootError
        )
        return
    }

    // We use a local state for the setup step transitions, 
    // but the final state is determined by the persisted settings.
    val isAlreadyHome = remember(context) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    }
    
    var currentStep by rememberSaveable { 
        mutableStateOf(if (isAlreadyHome) SetupStep.PAGE_1 else SetupStep.SET_DEFAULT) 
    }
    var userName by rememberSaveable { mutableStateOf("") }
    var userCountry by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is MainUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MainUiState.Success -> {
                val desktopState = state.desktopState
                val settings = desktopState.settings
                if (settings?.isSetupComplete == true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedWallpaper()
                        DesktopScreen(viewModel = viewModel, desktopState = desktopState, onRestart = onOSRestart)
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith
                                        fadeOut(animationSpec = tween(500))
                            },
                            label = "setup_transition"
                        ) { step ->
                            when (step) {
                                SetupStep.SET_DEFAULT -> SetDefaultScreen(onNext = { currentStep = SetupStep.PAGE_1 })
                                SetupStep.PAGE_1 -> SetupPage1(onNext = { currentStep = SetupStep.PAGE_2 })
                                SetupStep.PAGE_2 -> SetupPage2(onNext = { currentStep = SetupStep.PAGE_3 })
                                SetupStep.PAGE_3 -> SetupPage3(onNext = { currentStep = SetupStep.PAGE_4 })
                                SetupStep.PAGE_4 -> SetupPage4(
                                    country = userCountry,
                                    onCountryChange = { userCountry = it },
                                    onNext = { currentStep = SetupStep.PAGE_5 }
                                )
                                SetupStep.PAGE_5 -> SetupPage5(
                                    name = userName,
                                    onNameChange = { userName = it },
                                    onNext = { 
                                        viewModel.completeSetup(userName, userCountry)
                                    }
                                )
                                SetupStep.HOME -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AnimatedWallpaper()
                                        DesktopScreen(viewModel = viewModel, desktopState = desktopState, onRestart = onOSRestart)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun OSStatusBar() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.2f)),
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(6.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape))
                    }
                }
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.2f)),
                    color = Color.Transparent
                ) {}
                Surface(
                    modifier = Modifier.size(24.dp, 12.dp),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.2f)),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f).background(Color.Black.copy(alpha = 0.4f)))
                }
            }
        }
    }
}

@Composable
fun SetDefaultScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OSStatusBar()
        Spacer(modifier = Modifier.height(48.dp))
        OSIcon()
        Spacer(modifier = Modifier.height(32.dp))
        Text("Uniblox OS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Normal)
        Spacer(modifier = Modifier.height(16.dp))
        Surface(color = OSSecondaryContainer, shape = RoundedCornerShape(50)) {
            Text("System Configuration", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = OSOnSecondaryContainer)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("To complete your setup and enable all system features, set Uniblox as your primary interface.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepItem(number = "1", title = "Open Home settings", description = "Uniblox will redirect you to the system dialog.")
            StepItem(number = "2", title = "Select Uniblox OS", description = "Grant the system-level permissions required.")
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { 
                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                context.startActivity(intent)
                onNext()
            }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = OSPrimary), shape = RoundedCornerShape(50)) {
                Text("Set as Default Home App")
            }
            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("Maybe later", color = OSPrimary)
            }
        }
    }
}

@Composable
fun SetupPage1(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("uniblox os", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light, color = OSPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("to start with the os, please install the pkgs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(50)) {
            Text("Install Packages")
        }
    }
}

@Composable
fun SetupPage2(onNext: () -> Unit) {
    val packages = listOf("unibloxrunpack", "unibloxbundle", "unibloxexecutables", "unibloxwebpack", "system32.unibloxrunpack", "unibloxengine.node.runtime.unibloxwebpack", "unibloxengineapp.unibloxwebpack.unibloxexecutables")
    var installedCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..packages.size) {
            delay(600)
            installedCount = i
        }
        delay(1000)
        onNext()
    }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("installing pkgs..........", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            packages.forEachIndexed { index, pkg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg, modifier = Modifier.weight(1f), color = if (index < installedCount) OSTextPrimary else OSTextSecondary)
                    if (index < installedCount) Text("✅")
                }
            }
        }
    }
}

@Composable
fun SetupPage3(onNext: () -> Unit) {
    val context = LocalContext.current
    var isOverlayGranted by remember { mutableStateOf(android.provider.Settings.canDrawOverlays(context)) }

    // Re-check permission when resuming
    DisposableEffect(Unit) {
        val observer = { isOverlayGranted = android.provider.Settings.canDrawOverlays(context) }
        onDispose { }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(modifier = Modifier.size(80.dp), color = OSSecondaryContainer, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(40.dp), tint = OSPrimary) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("enable all permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("(including administration and draw over apps)", color = OSTextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = OSSecondaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOverlayGranted) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (isOverlayGranted) Color(0xFF4CAF50) else OSTextSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Draw Over Other Apps", fontWeight = FontWeight.Bold)
                    Text("Required for windowed application controls", style = MaterialTheme.typography.labelSmall, color = OSTextSecondary)
                }
                if (!isOverlayGranted) {
                    TextButton(onClick = {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) {
                        Text("Grant")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Grant All Permissions")
        }
    }
}

@Composable
fun SetupPage4(country: String, onCountryChange: (String) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("where do you live?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = country, onValueChange = onCountryChange, label = { Text("Country") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onNext, enabled = country.isNotBlank(), modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(50)) {
            Text("Next")
        }
    }
}

@Composable
fun SetupPage5(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("type your name", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = onNext, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(50)) {
            Text("Complete Setup")
        }
    }
}

@Composable
fun DesktopScreen(viewModel: MainViewModel, desktopState: DesktopUiState, onRestart: () -> Unit) {
    val activeDesktop = desktopState.desktops.firstOrNull { it.isSelected } ?: desktopState.desktops.firstOrNull() ?: DesktopConfig(name = "Default")
    val userName = desktopState.settings?.userName ?: ""
    val context = LocalContext.current
    val pm = context.packageManager
    
    val installedApps = remember(desktopState.webApps, pm) {
        val intents = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val appsList = pm.queryIntentActivities(intents, 0).map { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            
            val webUrl = when {
                pkg.contains("youtube") -> "https://www.youtube.com"
                pkg.contains("facebook") -> "https://www.facebook.com"
                pkg.contains("twitter") || pkg.contains("x.corp") -> "https://x.com"
                pkg.contains("threads") -> "https://www.threads.net"
                pkg.contains("reddit") -> "https://www.reddit.com"
                pkg.contains("google.android.apps.messaging") -> "https://messages.google.com"
                pkg.contains("google.android.gm") -> "https://mail.google.com"
                pkg.contains("com.android.chrome") -> "https://www.google.com"
                else -> null
            }
            
            AppInfo(name = name, packageName = pkg, icon = icon, isWeb = webUrl != null, url = webUrl)
        }.toMutableList()
        
        fun getIcon(packageName: String): Drawable {
            return try { pm.getApplicationIcon(packageName) } catch (e: Exception) { pm.defaultActivityIcon }
        }

        if (appsList.none { it.packageName == "fun.cybercode.uniblox.ai" }) {
            appsList.add(AppInfo("InfinityCursor AI", "fun.cybercode.uniblox.ai", pm.defaultActivityIcon, true, "about:blank"))
        }

        if (appsList.none { it.packageName == "fun.cybercode.uniblox.store" }) {
            appsList.add(AppInfo("Uniblox Appstore", "fun.cybercode.uniblox.store", pm.defaultActivityIcon, true, "https://uniblox-fun.vercel.app"))
        }
        
        if (appsList.none { it.packageName == "system.utility.trash" }) {
            appsList.add(AppInfo("Recycle Bin", "system.utility.trash", pm.defaultActivityIcon, true, "about:blank"))
        }

        if (appsList.none { it.packageName == "system.terminal" }) {
            appsList.add(AppInfo("Terminal", "system.terminal", pm.defaultActivityIcon, true, "about:blank"))
        }

        if (appsList.none { it.packageName == "system.explorer" }) {
            appsList.add(AppInfo("File Explorer", "system.explorer", pm.defaultActivityIcon, true, "about:blank"))
        }

        // Add user-installed web apps
        desktopState.webApps.forEach { webApp ->
            appsList.add(AppInfo(webApp.name, "com.web." + webApp.name.hashCode(), pm.defaultActivityIcon, true, webApp.url))
        }

        // Scan for .uea apps in uniblox-os-datazip/apps
        val systemDir = context.filesDir.resolve("uniblox-os-datazip")
        val appsDir = systemDir.resolve("apps")
        if (appsDir.exists()) {
            appsDir.listFiles()?.filter { it.extension == "uea" }?.forEach { file ->
                try {
                    val content = file.readText()
                    val html = parseUeaContent(content)
                    if (html != null) {
                        val appName = file.nameWithoutExtension.split(".").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        appsList.add(
                            AppInfo(
                                name = appName,
                                packageName = "com.uea." + file.nameWithoutExtension,
                                icon = pm.defaultActivityIcon,
                                isWeb = true,
                                isUea = true,
                                htmlContent = html
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        appsList.sortedBy { it.name }
    }

    var screenType by rememberSaveable { mutableStateOf(ScreenType.START_SCREEN) }
    var activeMetroApp by remember { mutableStateOf<AppInfo?>(null) }

    var openWebWindows by remember { mutableStateOf(listOf<AppInfo>()) }
    var focusedApp by remember { mutableStateOf<AppInfo?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showCustomizer by remember { mutableStateOf(false) }
    var showWebAppInstaller by remember { mutableStateOf(false) }

    BackHandler {
        when (screenType) {
            ScreenType.METRO_APP -> screenType = ScreenType.START_SCREEN
            ScreenType.DESKTOP -> {
                if (openWebWindows.isNotEmpty()) {
                    openWebWindows = openWebWindows.dropLast(1)
                    focusedApp = openWebWindows.lastOrNull()
                } else {
                    screenType = ScreenType.START_SCREEN
                }
            }
            ScreenType.START_SCREEN -> { /* System Back */ }
        }
    }

    AnimatedContent(
        targetState = screenType,
        transitionSpec = {
            if (targetState == ScreenType.METRO_APP) {
                (scaleIn(initialScale = 0.7f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))) togetherWith 
                (scaleOut(targetScale = 1.3f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
            } else if (initialState == ScreenType.METRO_APP) {
                (scaleIn(initialScale = 1.3f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))) togetherWith 
                (scaleOut(targetScale = 0.7f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
            } else {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            }
        },
        label = "screen_transition"
    ) { currentScreen ->
        when (currentScreen) {
            ScreenType.START_SCREEN -> {
                Windows8StartScreen(
                    apps = installedApps,
                    userName = userName,
                    onAppClick = { app ->
                        if (app.isWeb || app.isUea) {
                            activeMetroApp = app
                            screenType = ScreenType.METRO_APP
                        } else {
                            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                try {
                                    if (android.provider.Settings.canDrawOverlays(context)) {
                                        context.startService(Intent(context, FloatingWindowService::class.java))
                                    }
                                    context.startActivity(launchIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    onDesktopClick = { screenType = ScreenType.DESKTOP },
                    onRestart = onRestart
                )
            }
            ScreenType.METRO_APP -> {
                activeMetroApp?.let { app ->
                    MetroAppView(
                        app = app,
                        onClose = { screenType = ScreenType.START_SCREEN }
                    )
                }
            }
            ScreenType.DESKTOP -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    contextMenuOffset = offset
                                    showContextMenu = true
                                }
                            )
                        }
                ) {
                    val desktopWidth = maxWidth
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            // Widget Layer
                            DesktopWidgetsLayer(
                                desktopId = activeDesktop.id,
                                widgets = desktopState.widgets,
                                isEditMode = desktopState.isEditMode,
                                installedApps = installedApps,
                                onMoveWidget = { id, x, y -> viewModel.updateWidgetPosition(id, x, y) },
                                onDeleteWidget = { viewModel.deleteWidget(it) },
                                onAppClick = { app ->
                                    if (app.isWeb && app.url != null) {
                                        if (!openWebWindows.contains(app)) {
                                            openWebWindows = openWebWindows + app
                                        }
                                        focusedApp = app
                                    } else {
                                        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) context.startActivity(launchIntent)
                                    }
                                }
                            )

                            // Desktop Icons / App Drawer
                            if (openWebWindows.isEmpty() || !isFullscreen) {
                                val appsToShow = if (activeDesktop.hasAppDrawer) installedApps else installedApps.take(12)
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(100.dp),
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(appsToShow.size) { index ->
                                        val app = appsToShow[index]
                                        DesktopIcon(app, index = index) {
                                            if (app.isWeb && app.url != null) {
                                                if (!openWebWindows.contains(app)) {
                                                    openWebWindows = openWebWindows + app
                                                }
                                                focusedApp = app
                                            } else {
                                                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                                if (launchIntent != null) context.startActivity(launchIntent)
                                            }
                                        }
                                    }
                                }
                            }

                            // Windows Layer (Tiling)
                            if (openWebWindows.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isFullscreen) 0.dp else 16.dp)
                                        .padding(bottom = if (isFullscreen) 0.dp else 40.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    openWebWindows.forEachIndexed { index, app ->
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = openWebWindows.contains(app),
                                            enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                                            exit = scaleOut(targetScale = 0.9f) + fadeOut(),
                                            modifier = Modifier
                                                .weight(
                                                    if (openWebWindows.size == 2) {
                                                        if (index == 0) splitRatio else (1f - splitRatio)
                                                    } else 1f
                                                )
                                                .fillMaxHeight()
                                                .padding(if (isFullscreen) 0.dp else 4.dp)
                                        ) {
                                            WebViewWindow(
                                                app = app,
                                                isFullscreen = isFullscreen,
                                                onClose = {
                                                    openWebWindows = openWebWindows - app
                                                    if (focusedApp == app) focusedApp = openWebWindows.lastOrNull()
                                                },
                                                onFullscreenToggle = { isFullscreen = !isFullscreen }
                                            )
                                        }
                                        
                                        if (openWebWindows.size == 2 && index == 0 && !isFullscreen) {
                                            Box(
                                                modifier = Modifier
                                                    .width(8.dp)
                                                    .fillMaxHeight()
                                                    .pointerInput(Unit) {
                                                        detectDragGestures { change, dragAmount ->
                                                            change.consume()
                                                            val widthPx = desktopWidth.toPx()
                                                            val newRatio = splitRatio + (dragAmount.x / widthPx)
                                                            splitRatio = newRatio.coerceIn(0.2f, 0.8f)
                                                        }
                                                    }
                                                    .background(Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.2f).background(Color.White.copy(alpha = 0.5f), CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        DesktopTaskbar(
                            onStartClick = { screenType = ScreenType.START_SCREEN },
                            activeApps = openWebWindows,
                            onAppClick = { app -> focusedApp = app },
                            showStart = true
                        )
                    }
                    
                    if (!isFullscreen) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            RightSidePanel(
                                activeApps = openWebWindows
                            )
                        }
                    }
                }
            }
        }
    }

    // Overlay Components
    if (showContextMenu) {
            DesktopContextMenu(
                offset = contextMenuOffset,
                desktops = desktopState.desktops,
                isEditMode = desktopState.isEditMode,
                onDismiss = { showContextMenu = false },
                onNewDesktop = { viewModel.createDesktop("Desktop ${desktopState.desktops.size + 1}") },
                onSelectDesktop = { viewModel.selectDesktop(it.id) },
                onToggleEditMode = { viewModel.toggleEditMode() },
                onInstallWebApp = { showWebAppInstaller = true },
                onAddWidget = { type -> viewModel.addWidget(type, activeDesktop.id) },
                onOpenSystemFiles = {
                    val explorer = installedApps.find { it.packageName == "system.explorer" }
                    if (explorer != null) {
                        if (openWebWindows.none { it.packageName == explorer.packageName }) {
                            openWebWindows = openWebWindows + explorer
                        }
                        focusedApp = explorer
                    }
                }
            )
        }

        if (showCustomizer) {
            DesktopCustomizer(
                desktop = activeDesktop,
                onUpdate = { viewModel.updateDesktop(it) },
                onClose = { showCustomizer = false }
            )
        }

        if (showWebAppInstaller) {
            InstallWebAppDialog(
                onInstall = { name, url -> viewModel.installWebApp(name, url) },
                onDismiss = { showWebAppInstaller = false }
            )
        }
    }

@Composable
fun StartMenu(
    apps: List<AppInfo>,
    userName: String,
    onAppClick: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = if (searchQuery.isBlank()) apps else apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize().clickable { onClose() }, contentAlignment = Alignment.BottomStart) {
        Surface(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 80.dp)
                .width(400.dp)
                .fillMaxHeight(0.7f)
                .clickable(enabled = false) {}, 
            shape = RoundedCornerShape(24.dp),
            color = Color(0xEE1A1C3E),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Blue,
                        focusedBorderColor = Color.Blue,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All apps", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Long press to pin", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredApps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onAppClick(app) },
                                        onLongPress = { onPinApp(app) }
                                    )
                                }
                                .padding(4.dp)
                        ) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color.Blue) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(userName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(userName, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = Color.White) }
                }
            }
        }
    }
}

@Composable
fun WebViewWindow(
    app: AppInfo,
    isFullscreen: Boolean,
    onClose: () -> Unit,
    onFullscreenToggle: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = if (isFullscreen || app.packageName == "system.explorer") RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
        color = Color.White,
        border = if (isFullscreen || app.packageName == "system.explorer") null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        shadowElevation = if (app.packageName == "system.explorer") 0.dp else 8.dp
    ) {
        Column {
            if (app.packageName != "system.explorer") {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    color = Color(0xFFF0F0F0),
                    shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(text = app.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onFullscreenToggle, modifier = Modifier.size(28.dp)) {
                                Icon(if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                if (app.packageName == "system.explorer" && !isFullscreen) {
                    IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                when (app.packageName) {
                    "system.utility.trash" -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Recycle Bin is empty", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        }
                    }
                    "system.terminal" -> {
                        val systemDir = remember { context.filesDir.resolve("uniblox-os-datazip") }
                        SystemTerminal(systemDir)
                    }
                    "system.explorer" -> {
                        val systemDir = remember { context.filesDir.resolve("uniblox-os-datazip") }
                        var currentDir by remember { mutableStateOf(systemDir) }
                        var showHidden by remember { mutableStateOf(true) }
                        val isCorrupted = remember(currentDir) { !systemDir.resolve("system32/data.data.txt").exists() }
                        var selectedItem by remember { mutableStateOf<String?>(null) }
                        var showMenu by remember { mutableStateOf(false) }
                        var menuLevel by remember { mutableStateOf(0) } // 0: Open, 1: Administrator, 2: System Files
                        var fileContentToShow by remember { mutableStateOf<String?>(null) }

                        val items = remember(currentDir, showHidden) {
                            currentDir.listFiles()?.filter {
                                if (!showHidden && currentDir == systemDir) {
                                    it.name != "system32" && it.name != "packages" && !it.name.startsWith(".")
                                } else true
                            }?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
                        }

                        if (fileContentToShow != null) {
                            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { fileContentToShow = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                                    }
                                    Text(selectedItem ?: "System File", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                }
                                HorizontalDivider(color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(fileContentToShow!!, color = Color(0xFF00FF00), style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState()))
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentDir != systemDir) {
                                        IconButton(onClick = { currentDir = currentDir.parentFile ?: systemDir }) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Back")
                                        }
                                    }
                                    val virtualPath = "/os/data/root" + currentDir.absolutePath.substringAfter("uniblox-os-datazip").let { if (it.isEmpty()) "/" else it }
                                    Text("Explorer: $virtualPath", style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isCorrupted && currentDir.name == "system32") {
                                    Text("FS ERROR: system32 CORRUPTED", color = Color.Red)
                                }

                                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            selectedItem = null
                                            menuLevel = 0
                                            showMenu = true
                                        }
                                    )
                                }) {
                                    Column {
                                        items.forEach { file ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                                    .clickable {
                                                        if (file.isDirectory) {
                                                            currentDir = file
                                                        } else if (file.extension == "txt" || file.extension == "uea") {
                                                            try {
                                                                fileContentToShow = file.readText()
                                                                selectedItem = file.name
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onLongPress = {
                                                                selectedItem = file.name + (if (file.isDirectory) "/" else "")
                                                                menuLevel = 0
                                                                showMenu = true
                                                            }
                                                        )
                                                    }
                                            ) {
                                                Icon(
                                                    if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (!showHidden && (file.name == "system32" || file.name == "packages")) Color.Gray.copy(alpha = 0.3f) else Color.Gray
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(file.name)
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { 
                                            showMenu = false
                                            menuLevel = 0
                                        }
                                    ) {
                                        when (menuLevel) {
                                            0 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Open") },
                                                    onClick = { menuLevel = 1 }
                                                )
                                            }
                                            1 -> {
                                                DropdownMenuItem(
                                                    text = { Text("Administrator") },
                                                    onClick = { menuLevel = 2 }
                                                )
                                            }
                                            2 -> {
                                                DropdownMenuItem(
                                                    text = { Text("System Files") },
                                                    onClick = {
                                                        showMenu = false
                                                        showHidden = true
                                                        currentDir = systemDir.resolve("system32")
                                                        menuLevel = 0
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "fun.cybercode.uniblox.ai" -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("InfinityCursor AI is ready to assist you.", color = Color.Gray)
                            }
                            TextField(
                                value = "", onValueChange = {},
                                placeholder = { Text("Ask InfinityCursor...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                    else -> {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    if (app.isUea && app.htmlContent != null) {
                                        loadDataWithBaseURL("https://uniblox.local/", app.htmlContent, "text/html", "utf-8", null)
                                    } else {
                                        loadUrl(app.url ?: "")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopTaskbar(
    onStartClick: () -> Unit,
    activeApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    showStart: Boolean = true
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(36.dp)),
            color = Color(0xCC1A1C1E)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showStart) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Blue.copy(alpha = 0.2f))
                            .clickable { onStartClick() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OSIcon(size = 32.dp)
                        Text(
                            text = "apps",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeApps.forEach { app ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable { onAppClick(app) },
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                    Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = app.name, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWallpaper() {
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_animation")
    val frameIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 6,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame_index"
    )

    val panOffset by infiniteTransition.animateFloat(
        initialValue = -40f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallpaper_pan"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = SineAroundEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wallpaper_scale"
    )

    val wallpapers = listOf(
        R.drawable.img_wp_1,
        R.drawable.img_wp_2,
        R.drawable.img_wp_3,
        R.drawable.img_wp_4,
        R.drawable.img_wp_5
    )

    // Vibrant Bloom-inspired fallback gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00050A), // Rich deep blue-black
            Color(0xFF1A1F35), // Dark violet-navy
            Color(0xFF0A0F1E)
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Box(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = panOffset.dp.toPx()
                translationY = (panOffset / 1.5f).dp.toPx()
                scaleX = scale
                scaleY = scale
            }
        ) {
            Crossfade(
                targetState = wallpapers[if (wallpapers.isNotEmpty()) frameIndex % wallpapers.size else 0],
                animationSpec = tween(2500),
                label = "wallpaper_fade"
            ) { wpRes ->
                Image(
                    painter = painterResource(id = wpRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.85f // Dimmed slightly for better readability of desktop icons
                )
            }
        }
        
        // Add a subtle bloom glow overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Cyan.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.6f
                )
            )
        }
    }
}

val SineAroundEasing = Easing { fraction ->
    ((Math.sin(fraction * Math.PI * 2 - Math.PI / 2) + 1) / 2).toFloat()
}

@Composable
fun OSIcon(size: androidx.compose.ui.unit.Dp = 96.dp) {
    Surface(modifier = Modifier.size(size).shadow(if (size > 50.dp) 12.dp else 0.dp, RoundedCornerShape(size / 2.5f)), color = Color.White, shape = RoundedCornerShape(size / 2.5f)) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(size * 0.6f).background(brush = Brush.linearGradient(colors = listOf(Color(0xFF6750A4), Color(0xFF927BCC))), shape = RoundedCornerShape(size * 0.16f)), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(size * 0.25f).border(4.dp, Color.White, RoundedCornerShape(size * 0.06f)))
            }
        }
    }
}

@Composable
fun DesktopIcon(app: AppInfo, index: Int = 0, onClick: () -> Unit) {
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 100L) // Staggered entrance
        isLaunched = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isLaunched) 1f else 0.8f,
        animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "icon_scale"
    )
    val opacity by animateFloatAsState(
        targetValue = if (isLaunched) 1f else 0f,
        animationSpec = tween(600),
        label = "icon_opacity"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = opacity
            }
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        val isAI = app.name == "infinitycursor ai"
        val isRecycleBin = app.name == "recycle bin"
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .let { modifier ->
                    if (isAI) {
                        modifier.background(Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF90E0EF))))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    } else {
                        modifier.background(if (isRecycleBin) Color(0xFF4CAF50) else Color.White)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isAI) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(), 
                    contentDescription = app.name, 
                    modifier = Modifier.fillMaxSize().padding(if (app.name == "recycle bin") 8.dp else 0.dp), 
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name.lowercase(), 
            color = Color.White, 
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
            textAlign = TextAlign.Center, 
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun RightSidePanel(activeApps: List<AppInfo>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(end = 16.dp)
            .width(100.dp)
            .fillMaxHeight(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(48.dp))
        Text("internet", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Text("active apps", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        activeApps.takeLast(3).forEach { app -> Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).padding(4.dp)) }
    }
}

@Composable
fun StepItem(number: String, title: String, description: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFEF7FF), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFCAC4D0))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(modifier = Modifier.size(40.dp), color = Color(0xFFD0BCFF), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { Text(text = number, color = Color(0xFF381E72), fontWeight = FontWeight.Bold) }
            }
            Column {
                Text(text = title, fontWeight = FontWeight.Medium, color = Color(0xFF1D1B20))
                Text(text = description, style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F))
            }
        }
    }
}

@Composable
fun DesktopContextMenu(
    offset: Offset,
    desktops: List<DesktopConfig>,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onNewDesktop: () -> Unit,
    onSelectDesktop: (DesktopConfig) -> Unit,
    onToggleEditMode: () -> Unit,
    onInstallWebApp: () -> Unit,
    onAddWidget: (String) -> Unit,
    onOpenSystemFiles: () -> Unit
) {
    var adminLevel by remember { mutableStateOf(0) } // 0: default, 1: admin, 2: sys files

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = androidx.compose.ui.unit.DpOffset(offset.x.dp / 8, 0.dp)
    ) {
        if (adminLevel == 0) {
            DropdownMenuItem(
                text = { Text("Open") },
                onClick = { adminLevel = 1 },
                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Create New Desktop") },
            onClick = { onNewDesktop(); onDismiss() },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(if (isEditMode) "Save Layout" else "Edit Desktop") },
            onClick = { onToggleEditMode(); onDismiss() },
            leadingIcon = { Icon(if (isEditMode) Icons.Default.Save else Icons.Default.Edit, contentDescription = null) }
        )
        if (isEditMode) {
            DropdownMenuItem(
                text = { Text("Add Wifi Stats") },
                onClick = { onAddWidget("wifi"); onDismiss() },
                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Add Slider") },
                onClick = { onAddWidget("slider"); onDismiss() },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text("Install Web App") },
            onClick = { onInstallWebApp(); onDismiss() },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) }
        )
        HorizontalDivider()
        desktops.forEach { desktop ->
            DropdownMenuItem(
                text = { Text(desktop.name) },
                onClick = { onSelectDesktop(desktop); onDismiss() },
                trailingIcon = { if (desktop.isSelected) Icon(Icons.Default.Check, contentDescription = null) }
            )
        }
    } else if (adminLevel == 1) {
        DropdownMenuItem(
            text = { Text("Administrator") },
            onClick = { adminLevel = 2 },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
        )
    } else if (adminLevel == 2) {
        DropdownMenuItem(
            text = { Text("System Files") },
            onClick = { onOpenSystemFiles(); onDismiss() },
            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) }
        )
    }
}
}

@Composable
fun DesktopCustomizer(
    desktop: DesktopConfig,
    onUpdate: (DesktopConfig) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Customize Desktop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Start Menu", modifier = Modifier.weight(1f))
                    Switch(checked = desktop.hasStartMenu, onCheckedChange = { onUpdate(desktop.copy(hasStartMenu = it)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("App Drawer Layer", modifier = Modifier.weight(1f))
                    Switch(checked = desktop.hasAppDrawer, onCheckedChange = { onUpdate(desktop.copy(hasAppDrawer = it)) })
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) { Text("Done") }
        }
    )
}

@Composable
fun InstallWebAppDialog(onInstall: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Form Window Web App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("App Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && url.isNotBlank()) onInstall(name, url); onDismiss() }) { Text("Install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DesktopWidgetsLayer(
    desktopId: Long,
    widgets: List<WidgetConfig>,
    isEditMode: Boolean,
    installedApps: List<AppInfo>,
    onMoveWidget: (Long, Float, Float) -> Unit,
    onDeleteWidget: (WidgetConfig) -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        for (widget in widgets) {
            key(widget.id) {
                val appInfo = if (widget.type == "app") {
                    installedApps.find { it.packageName == widget.metadata }
                } else null

                WidgetContainer(
                    widget = widget,
                    appInfo = appInfo,
                    isEditMode = isEditMode,
                    onMove = { x, y -> onMoveWidget(widget.id, x, y) },
                    onDelete = { onDeleteWidget(widget) },
                    onAppClick = { appInfo?.let { onAppClick(it) } }
                )
            }
        }
    }
}

@Composable
fun WidgetContainer(
    widget: WidgetConfig,
    appInfo: AppInfo?,
    isEditMode: Boolean,
    onMove: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onAppClick: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(widget.x) }
    var offsetY by remember { mutableFloatStateOf(widget.y) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .then(
                if (isEditMode) {
                    Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onMove(offsetX, offsetY) }
                            ) { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                } else Modifier
            )
    ) {
        when (widget.type) {
            "wifi" -> WifiStatsWidget()
            "slider" -> SystemSliderWidget()
            "app" -> {
                appInfo?.let { app ->
                    DesktopIcon(app = app, index = 0, onClick = { if (!isEditMode) onAppClick() })
                }
            }
        }
        
        if (isEditMode) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .offset(x = 8.dp, y = (-8).dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun WifiStatsWidget() {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.width(180.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.Cyan)
            Column {
                Text("UNIBLOX-NET", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("Connected • 450Mbps", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SystemSliderWidget() {
    var value by remember { mutableFloatStateOf(0.7f) }
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.width(180.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Brightness", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun Windows8StartScreen(
    apps: List<AppInfo>,
    userName: String,
    onAppClick: (AppInfo) -> Unit,
    onDesktopClick: () -> Unit,
    onRestart: () -> Unit
) {
    val accentColor = Color(0xFF0078D7)
    
    Box(modifier = Modifier.fillMaxSize().background(accentColor)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(userName, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("Admin", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onRestart) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Restart", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                StartScreenGroup(title = "Primary") {
                    StartTile(
                        name = "Desktop",
                        color = Color(0xFF68217A),
                        icon = Icons.Default.Dashboard,
                        onClick = onDesktopClick,
                        isLarge = true
                    )
                    
                    // Show some key apps first
                    apps.filter { it.isWeb }.take(6).forEach { app ->
                        StartAppTile(app, onClick = { onAppClick(app) })
                    }
                }
                
                // Group standard android apps in columns
                val nativeApps = apps.filter { !it.isWeb && !it.isUea }
                val groups = nativeApps.chunked(20) // Group apps into scrollable columns
                
                groups.forEachIndexed { index, groupApps ->
                    StartScreenGroup(title = if (index == 0) "Apps" else "") {
                        // Display apps in a small grid within the column
                        val subGroups = groupApps.chunked(2)
                        subGroups.forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StartAppTile(pair[0], onClick = { onAppClick(pair[0]) })
                                if (pair.size > 1) {
                                    StartAppTile(pair[1], onClick = { onAppClick(pair[1]) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreenGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
        Text(title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun StartTile(name: String, color: Color, icon: ImageVector, onClick: () -> Unit, isLarge: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (isLarge) 210.dp else 100.dp, 100.dp)
            .background(color)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp).align(Alignment.Center))
        Text(name, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun StartAppTile(app: AppInfo, onClick: () -> Unit) {
    val tileColor = remember(app.packageName) {
        val colors = listOf(Color(0xFF2D89EF), Color(0xFFDA532C), Color(0xFFEE1111), Color(0xFF00A300), Color(0xFF7E3878))
        colors[app.packageName.hashCode().let { if (it < 0) -it else it } % colors.size]
    }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(tileColor)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp).align(Alignment.Center)
        )
        Text(app.name, color = Color.White, fontSize = 10.sp, maxLines = 2, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun MetroAppView(app: AppInfo, onClose: () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(800)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isFullscreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            app.name,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(onClick = { isFullscreen = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f)) {
                WebViewWindow(
                    app = app,
                    isFullscreen = true,
                    onClose = onClose,
                    onFullscreenToggle = {}
                )
                
                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            val tileColor = remember(app.packageName) {
                val colors = listOf(Color(0xFF2D89EF), Color(0xFFDA532C), Color(0xFFEE1111), Color(0xFF00A300), Color(0xFF7E3878))
                colors[app.packageName.hashCode().let { if (it < 0) -it else it } % colors.size]
            }
            
            Box(modifier = Modifier.fillMaxSize().background(tileColor), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(app.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

private fun parseUeaContent(content: String): String? {
    val startTag = "<application>"
    val endTag = "</application.web-app>"

    val startIndex = content.indexOf(startTag)
    val endIndex = content.lastIndexOf(endTag)

    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        return content.substring(startIndex + startTag.length, endIndex).trim()
    }

    // Fallback: search for html tags
    val lowercaseContent = content.lowercase()
    val htmlStartIndex = lowercaseContent.indexOf("<!doctype html>")
    if (htmlStartIndex != -1) {
        return content.substring(htmlStartIndex)
    }

    val htmlTagIndex = lowercaseContent.indexOf("<html>")
    if (htmlTagIndex != -1) {
        return content.substring(htmlTagIndex)
    }

    return null
}

private fun copyAssetsToFiles(context: android.content.Context, assetPath: String, targetDir: File) {
    val assets = context.assets.list(assetPath) ?: return
    if (assets.isEmpty()) {
        try {
            context.assets.open(assetPath).use { input ->
                targetDir.parentFile?.mkdirs()
                targetDir.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            // Probably a directory or doesn't exist
        }
    } else {
        targetDir.mkdirs()
        for (asset in assets) {
            copyAssetsToFiles(context, "$assetPath/$asset", File(targetDir, asset))
        }
    }
}

