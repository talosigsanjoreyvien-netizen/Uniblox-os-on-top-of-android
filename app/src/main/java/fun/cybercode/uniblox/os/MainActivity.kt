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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import `fun`.cybercode.uniblox.os.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                UnibloxOSApp()
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
fun UnibloxOSApp() {
    var currentStep by remember { mutableStateOf(SetupStep.SET_DEFAULT) }
    var userName by remember { mutableStateOf("") }
    var userCountry by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (currentStep) {
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
                onNext = { currentStep = SetupStep.HOME }
            )
            SetupStep.HOME -> DesktopScreen(userName = userName)
        }
    }
}

@Composable
fun OSStatusBar() {
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
fun DesktopScreen(userName: String) {
    val context = LocalContext.current
    val pm = context.packageManager
    val installedApps = remember {
        val intents = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = pm.queryIntentActivities(intents, 0).map {
            AppInfo(name = it.loadLabel(pm).toString(), packageName = it.activityInfo.packageName, icon = it.loadIcon(pm))
        }.toMutableList()
        
        apps.add(AppInfo("uniblox-fun", "com.web.uniblox", context.packageManager.defaultActivityIcon, true, "https://uniblox-fun.vercel.app"))
        apps.add(AppInfo("youtube", "com.google.android.youtube", context.packageManager.defaultActivityIcon, true, "https://www.youtube.com"))
        apps.add(AppInfo("recycle bin", "com.os.recyclebin", context.packageManager.defaultActivityIcon))
        
        apps.sortedBy { it.name }
    }

    var openWebWindows by remember { mutableStateOf(listOf<AppInfo>()) }
    var focusedApp by remember { mutableStateOf<AppInfo?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var isStartMenuOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val desktopWidth = maxWidth
        AnimatedWallpaper()
        
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // Desktop Icons Layer
                if (openWebWindows.isEmpty() || !isFullscreen) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(installedApps.take(5)) { app ->
                            DesktopIcon(app) {
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
                            .padding(bottom = if (isFullscreen) 0.dp else 40.dp), // Leave space for taskbar
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        openWebWindows.forEachIndexed { index, app ->
                            Box(
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
                            
                            // Add Splitter between first and second window if exactly 2 are open
                            if (openWebWindows.size == 2 && index == 0 && !isFullscreen) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaRatio = dragAmount.x / size.width
                                                // Adjust ratio based on screen width
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
                if (isStartMenuOpen) {
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
                        onClose = { isStartMenuOpen = false }
                    )
                }
            }
            
            // Taskbar is always at the bottom
            DesktopTaskbar(
                onStartClick = { isStartMenuOpen = !isStartMenuOpen },
                activeApps = openWebWindows,
                onAppClick = { app -> focusedApp = app }
            )
        }
        
        if (!isFullscreen) {
            RightSidePanel(activeApps = openWebWindows)
        }
    }
}

@Composable
fun StartMenu(
    apps: List<AppInfo>,
    userName: String,
    onAppClick: (AppInfo) -> Unit,
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
                .clickable(enabled = false) {}, // Prevent clicks from going through to the desktop
            shape = RoundedCornerShape(24.dp),
            color = Color(0xEE1A1C2E), // Dark blurred-like background
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                // Search Bar
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
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Apps Grid
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
                                .clickable { onAppClick(app) }
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
                
                // User Footer
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
        color = Color.White,
        border = if (isFullscreen) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        shadowElevation = 8.dp
    ) {
        Column {
            // Window Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = Color(0xFFF0F0F0),
                shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onFullscreenToggle, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            
            // Browser Content
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            loadUrl(app.url ?: "")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay to catch clicks and manage focus if needed
            }
        }
    }
}

@Composable
fun DesktopTaskbar(
    onStartClick: () -> Unit,
    activeApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(36.dp)),
        color = Color(0xCC1A1C1E)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Blue.copy(alpha = 0.2f))
                    .clickable { onStartClick() },
                contentAlignment = Alignment.Center
            ) {
                OSIcon(size = 32.dp)
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                activeApps.forEach { app ->
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppClick(app) },
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWallpaper() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 30f, 
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing), 
            repeatMode = RepeatMode.Reverse
        ), 
        label = ""
    )
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF001220))) {
        Canvas(modifier = Modifier.fillMaxSize().offset(x = offset.dp)) {
            drawCircle(color = Color(0xFF0D47A1), radius = size.width, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.8f), alpha = 0.5f)
            drawCircle(color = Color(0xFF1976D2), radius = size.width * 0.8f, center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f), alpha = 0.3f)
        }
    }
}

@Composable
fun OSIcon(size: androidx.compose.ui.unit.Dp = 96.dp) {
    Surface(
        modifier = Modifier.size(size).shadow(if (size > 50.dp) 12.dp else 0.dp, RoundedCornerShape(size / 2.5f)), 
        color = Color.White, 
        shape = RoundedCornerShape(size / 2.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(size * 0.6f).background(
                    brush = Brush.linearGradient(colors = listOf(Color(0xFF6750A4), Color(0xFF927BCC))), 
                    shape = RoundedCornerShape(size * 0.16f)
                ), 
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(size * 0.25f).border(4.dp, Color.White, RoundedCornerShape(size * 0.06f)))
            }
        }
    }
}

@Composable
fun DesktopIcon(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier.width(IntrinsicSize.Min).clickable { onClick() }.padding(8.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(), 
            contentDescription = app.name, 
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(if (app.name == "recycle bin") Color(0xFF4CAF50) else Color.White).padding(if (app.name == "recycle bin") 8.dp else 0.dp), 
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = app.name.lowercase(), color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun RightSidePanel(activeApps: List<AppInfo>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
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
            activeApps.takeLast(3).forEach { app -> 
                Image(bitmap = app.icon.toBitmap().asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).padding(4.dp)) 
            }
        }
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
