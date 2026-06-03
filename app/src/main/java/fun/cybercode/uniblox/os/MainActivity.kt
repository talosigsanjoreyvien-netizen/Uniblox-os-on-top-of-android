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

class MainActivity : ComponentActivity() {
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
fun RecoveryScreen(
    log: List<String>,
    isRecovering: Boolean,
    onStartRecovery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "UNIBLOX RECOVERY CONSOLE",
                color = Color.Red,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                log.forEach { line ->
                    Text(
                        "> $line",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isRecovering) {
                Button(
                    onClick = onStartRecovery,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("INITIATE SYSTEM REPAIR")
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color.White,
                    trackColor = Color.DarkGray
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

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isWeb: Boolean = false,
    val url: String? = null
)

@Composable
fun UnibloxOSApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var isSystemReady by rememberSaveable { mutableStateOf(false) }
    var recoveryLog by remember { mutableStateOf(listOf<String>()) }
    var isRecovering by remember { mutableStateOf(false) }

    val systemFiles = listOf(
        "system32/config.sys.uea.txt",
        "system32/data.assets.uea.txt",
        "system32/data.data.txt",
        "system32/drivers.sys.uea.txt",
        "system32/main.uea.txt",
        "system32/main.uniblox_os_activity.uea.txt",
        "system32/system.executable.uea.txt",
        "packages/uniblox-os-datazip.code-workspace.txt",
        "packages/uniblox.audio.sound.effects.upk.txt",
        "packages/uniblox.graphics.renderer.dx11.upk.txt",
        "packages/unibloxrunpack.upk.txt",
        "packages/unibloxwebpack.upk.txt",
        "README.md.txt",
        "data.base.txt"
    )

    val context = LocalContext.current
    val systemDir = remember { context.filesDir.resolve("uniblox-os-datazip") }

    LaunchedEffect(Unit) {
        if (!systemDir.exists()) {
            systemDir.mkdirs()
        }
        
        // Check if system files are already present
        val missing = systemFiles.any { !systemDir.resolve(it).exists() }
        isSystemReady = !missing
    }

    if (!isSystemReady) {
        RecoveryScreen(
            log = recoveryLog,
            isRecovering = isRecovering,
            onStartRecovery = {
                isRecovering = true
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val logLines = mutableListOf<String>()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        logLines.add("UNIBLOX-OS BOOT FAILURE: Critical files missing.")
                        logLines.add("Initializing remote recovery from GitHub...")
                        recoveryLog = logLines.toList()
                    }
                    delay(1000)

                    try {
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder()
                            .url("https://github.com/talosigsanjoreyvien-netizen/Uniblox-os-on-top-of-android/archive/refs/heads/main.zip")
                            .build()

                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            logLines.add("Connecting to server...")
                            recoveryLog = logLines.toList()
                        }

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw java.io.IOException("Failed to download: $response")

                            val body = response.body ?: throw java.io.IOException("Empty response body")
                            val tempZip = context.cacheDir.resolve("system_recovery.zip")
                            
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                logLines.add("Downloading system_recovery.zip (${body.contentLength() / 1024} KB)...")
                                recoveryLog = logLines.toList()
                            }

                            body.byteStream().use { input ->
                                tempZip.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                logLines.add("Download complete. Extracting packages...")
                                recoveryLog = logLines.toList()
                            }

                            // Unzip and find the specific folder
                            val extractDir = context.cacheDir.resolve("temp_extract")
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

                            // Copy from the deep folder to systemDir
                            // The zip root folder is usually Uniblox-os-on-top-of-android-main
                            val sourcePath = "Uniblox-os-on-top-of-android-main/app/src/main/assets/uniblox-os-datazip"
                            val sourceDir = File(extractDir, sourcePath)
                            
                            if (sourceDir.exists()) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    logLines.add("Installing system32 assets...")
                                    recoveryLog = logLines.toList()
                                }
                                
                                sourceDir.copyRecursively(systemDir, overwrite = true)
                                
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    logLines.add("Cleaning up temporary files...")
                                    recoveryLog = logLines.toList()
                                }
                                tempZip.delete()
                                extractDir.deleteRecursively()
                                
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    logLines.add("Verifying system integrity...")
                                    delay(500)
                                    logLines.add("System core: OK")
                                    logLines.add("Rebooting kernel...")
                                    recoveryLog = logLines.toList()
                                }
                                delay(1000)
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isSystemReady = true
                                    isRecovering = false
                                }
                            } else {
                                throw Exception("Could not find system files in the downloaded archive.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            logLines.add("FATAL ERROR: ${e.message}")
                            logLines.add("Recovery aborted. Please check internet connection.")
                            recoveryLog = logLines.toList()
                            isRecovering = false
                        }
                    }
                }
            }
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
                        DesktopScreen(viewModel = viewModel, desktopState = desktopState)
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
                                        DesktopScreen(viewModel = viewModel, desktopState = desktopState)
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
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(modifier = Modifier.size(80.dp), color = OSSecondaryContainer, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(40.dp), tint = OSPrimary) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("enable all permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("(including administration)", color = OSTextSecondary)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(50)) {
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
fun DesktopScreen(viewModel: MainViewModel, desktopState: DesktopUiState) {
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
        
        appsList.sortedBy { it.name }
    }

    var openWebWindows by remember { mutableStateOf(listOf<AppInfo>()) }
    var focusedApp by remember { mutableStateOf<AppInfo?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var isStartMenuOpen by remember { mutableStateOf(false) }
    
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showCustomizer by remember { mutableStateOf(false) }
    var showWebAppInstaller by remember { mutableStateOf(false) }

    BackHandler {
        if (isStartMenuOpen) {
            isStartMenuOpen = false
        } else if (openWebWindows.isNotEmpty()) {
            openWebWindows = openWebWindows.dropLast(1)
            focusedApp = openWebWindows.lastOrNull()
        }
    }

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
                
                // Start Menu Layer
                if (activeDesktop.hasStartMenu) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isStartMenuOpen,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        StartMenu(
                            apps = installedApps,
                            userName = userName,
                            onAppClick = { app ->
                                isStartMenuOpen = false
                                if (app.isWeb && app.url != null) {
                                    if (!openWebWindows.contains(app)) {
                                        openWebWindows = openWebWindows + app
                                    }
                                    focusedApp = app
                                } else {
                                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) context.startActivity(launchIntent)
                                }
                            },
                            onPinApp = { app ->
                                viewModel.addWidget("app", activeDesktop.id, app.packageName)
                            },
                            onClose = { isStartMenuOpen = false }
                        )
                    }
                }
            }
            
            DesktopTaskbar(
                onStartClick = { if (activeDesktop.hasStartMenu) isStartMenuOpen = !isStartMenuOpen },
                activeApps = openWebWindows,
                onAppClick = { app -> focusedApp = app },
                showStart = activeDesktop.hasStartMenu
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
                        var showHidden by remember { mutableStateOf(false) }
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
                                    Text("Explorer: ${currentDir.absolutePath.substringAfter("uniblox-os-datazip")}", style = MaterialTheme.typography.titleSmall)
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
                                    loadUrl(app.url ?: "")
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
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f),
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
