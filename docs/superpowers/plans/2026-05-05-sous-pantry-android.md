# Sous Pantry Android — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the production iOS Sous Pantry app to native Android — Kotlin + Jetpack Compose — hitting the existing Node.js backend, shipped in 3 staged sprints over 10 days.

**Architecture:** MVVM with Hilt DI, Compose UI, Retrofit for API, Room for local pantry persistence, DataStore for user prefs/session. Each screen has a ViewModel (StateFlow state) + Screen composable. Navigation is Compose Nav with a bottom bar (4 tabs) plus pushed routes for camera/auth/onboarding.

**Tech Stack:** Kotlin 2.1, Jetpack Compose + Material3, Hilt, Retrofit + Gson, Room 2.7, CameraX 1.4, ML Kit, Firebase FCM, Supabase Kotlin SDK (stage 2), Google Play Billing v7 (stage 2)

---

## Existing Code (do not rewrite)

All 4 core screens are already working — `HomeScreen`, `PantryScreen`, `ShoppingScreen`, `PlanCookScreen` — with real ViewModels, API wiring, and Room. Tasks below build the missing 40%.

---

## Task 1: DataStore — User Preferences Foundation

**Files:**
- Create: `app/src/main/java/com/souspantry/app/data/local/UserPreferencesRepository.kt`
- Modify: `app/build.gradle.kts` (add test deps)

- [ ] **Step 1: Add test dependencies to `app/build.gradle.kts` inside `dependencies {}`**

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
testImplementation("io.mockk:mockk:1.13.12")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
```

- [ ] **Step 2: Create `UserPreferencesRepository.kt`**

```kotlin
package com.souspantry.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_DEVICE_ID        = stringPreferencesKey("device_id")
        val KEY_USER_NAME        = stringPreferencesKey("user_name")
        val KEY_ONBOARDING_DONE  = booleanPreferencesKey("onboarding_done")
        val KEY_NOTIF_ENABLED    = booleanPreferencesKey("notif_enabled")
        val KEY_SUPABASE_TOKEN   = stringPreferencesKey("supabase_token")
        val KEY_SUPABASE_USER_ID = stringPreferencesKey("supabase_user_id")
    }

    val deviceId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: UUID.randomUUID().toString().also { id ->
            context.dataStore.edit { it[KEY_DEVICE_ID] = id }
        }
    }

    val userName: Flow<String>        = context.dataStore.data.map { it[KEY_USER_NAME]       ?: "" }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val notifEnabled: Flow<Boolean>   = context.dataStore.data.map { it[KEY_NOTIF_ENABLED]   ?: true }
    val supabaseToken: Flow<String?>  = context.dataStore.data.map { it[KEY_SUPABASE_TOKEN] }
    val supabaseUserId: Flow<String?> = context.dataStore.data.map { it[KEY_SUPABASE_USER_ID] }

    suspend fun setUserName(name: String)         = context.dataStore.edit { it[KEY_USER_NAME]       = name }
    suspend fun setOnboardingDone()               = context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    suspend fun setNotifEnabled(on: Boolean)      = context.dataStore.edit { it[KEY_NOTIF_ENABLED]   = on }
    suspend fun setSupabaseSession(token: String, userId: String) = context.dataStore.edit {
        it[KEY_SUPABASE_TOKEN]   = token
        it[KEY_SUPABASE_USER_ID] = userId
    }
    suspend fun clearSupabaseSession() = context.dataStore.edit {
        it.remove(KEY_SUPABASE_TOKEN)
        it.remove(KEY_SUPABASE_USER_ID)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideUserPrefs(@ApplicationContext ctx: Context): UserPreferencesRepository =
        UserPreferencesRepository(ctx)
}
```

- [ ] **Step 3: Sync project — verify `BUILD SUCCESSFUL`**

```bash
./gradlew assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/souspantry/app/data/local/ app/build.gradle.kts
git commit -m "feat: DataStore UserPreferencesRepository (deviceId, name, onboarding, session)"
```

---

## Task 2: Settings Screen

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`
- Modify: `app/src/main/java/com/souspantry/app/ui/home/HomeScreen.kt`

- [ ] **Step 1: Create `SettingsViewModel.kt`**

```kotlin
package com.souspantry.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val userName     : String  = "",
    val notifEnabled : Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        prefs.userName,
        prefs.notifEnabled,
    ) { name, notif -> SettingsState(userName = name, notifEnabled = notif) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setUserName(name: String)       = viewModelScope.launch { prefs.setUserName(name) }
    fun setNotifEnabled(on: Boolean)    = viewModelScope.launch { prefs.setNotifEnabled(on) }
}
```

- [ ] **Step 2: Create `SettingsScreen.kt`**

```kotlin
package com.souspantry.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state    by vm.state.collectAsState()
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Name card
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it },
                        placeholder = { Text("Enter your name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                    Button(
                        onClick  = { vm.setUserName(nameInput.trim()) },
                        colors   = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save Name") }
                }
            }

            // Notifications
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                        Text("Recipe ideas and pantry reminders", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = state.notifEnabled, onCheckedChange = { vm.setNotifEnabled(it) },
                        colors  = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = SoftMint),
                    )
                }
            }

            // App info
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Sous Pantry v1.0", style = MaterialTheme.typography.bodyMedium)
                    Text("Your AI-powered kitchen companion", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update `NavHost.kt` — add Settings route + settings/barcode/receipt to sealed class**

Replace the full `NavHost.kt` with:

```kotlin
package com.souspantry.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.souspantry.app.ui.home.HomeScreen
import com.souspantry.app.ui.pantry.PantryScreen
import com.souspantry.app.ui.plancook.PlanCookScreen
import com.souspantry.app.ui.settings.SettingsScreen
import com.souspantry.app.ui.shopping.ShoppingScreen
import com.souspantry.app.ui.theme.Green
import com.souspantry.app.ui.theme.White

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Home      : Screen("home",      "Home",        Icons.Filled.Home)
    data object Pantry    : Screen("pantry",    "Pantry",      Icons.Filled.Kitchen)
    data object PlanCook  : Screen("plancook",  "Plan & Cook", Icons.Filled.MenuBook)
    data object Shopping  : Screen("shopping",  "Shopping",    Icons.Filled.ShoppingCart)
    data object Settings  : Screen("settings",  "Settings")
    data object Barcode   : Screen("barcode",   "Scan Barcode")
    data object Receipt   : Screen("receipt",   "Scan Receipt")
    data object Onboarding: Screen("onboarding","Onboarding")
    data object Auth      : Screen("auth",      "Sign In")
    data object Paywall   : Screen("paywall",   "Premium")
}

private val bottomNavItems = listOf(Screen.Home, Screen.Pantry, Screen.PlanCook, Screen.Shopping)

@Composable
fun SousPantryNavHost() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination
    val showBottomBar = currentDest?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = White) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon   = { Icon(screen.icon!!, screen.label) },
                            label  = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Green, selectedTextColor   = Green,
                                unselectedIconColor = Green.copy(alpha = 0.45f),
                                unselectedTextColor = Green.copy(alpha = 0.45f),
                                indicatorColor      = White,
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route)     { HomeScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) }) }
            composable(Screen.Pantry.route)   { PantryScreen(onBarcodeScan = { navController.navigate(Screen.Barcode.route) }, onReceiptScan = { navController.navigate(Screen.Receipt.route) }) }
            composable(Screen.PlanCook.route) { PlanCookScreen() }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Barcode.route)  { /* Task 4 — BarcodeScanScreen */ }
            composable(Screen.Receipt.route)  { /* Task 5 — ReceiptScanScreen */ }
            composable(Screen.Onboarding.route) { /* Task 9 — OnboardingScreen */ }
            composable(Screen.Auth.route)     { /* Task 7 — AuthScreen */ }
            composable(Screen.Paywall.route)  { /* Task 8 — PaywallScreen */ }
        }
    }
}
```

- [ ] **Step 4: Update `HomeScreen.kt` — add settings icon + `onNavigateToSettings` param**

Change `HomeScreen` signature:
```kotlin
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
)
```

Replace `HomeHeader()` composable:
```kotlin
@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Navy, Green)))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text("Sous Pantry", style = MaterialTheme.typography.headlineLarge.copy(color = White))
            Text("What are we cooking today?", style = MaterialTheme.typography.bodyMedium.copy(color = White.copy(alpha = 0.75f)))
        }
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Filled.Settings, "Settings", tint = White)
        }
    }
}
```

Update the `item { HomeHeader() }` call to `item { HomeHeader(onNavigateToSettings) }`.

Add import: `import androidx.compose.material.icons.filled.Settings`

- [ ] **Step 5: Build and run. Tap gear icon → Settings screen appears. Save name → reopen → name persists.**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/souspantry/app/ui/settings/ \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt \
        app/src/main/java/com/souspantry/app/ui/home/HomeScreen.kt
git commit -m "feat: Settings screen with name save + notifications toggle"
```

---

## Task 3: Camera Module

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/camera/CameraPreview.kt`

- [ ] **Step 1: Create `CameraPreview.kt`**

```kotlin
package com.souspantry.app.ui.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraPreview(
    modifier        : Modifier = Modifier,
    onCameraReady   : (ImageCapture) -> Unit,
    analysisUseCase : ImageAnalysis? = null,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView    = remember { PreviewView(context) }
    val imageCapture   = remember { ImageCapture.Builder().build() }

    LaunchedEffect(analysisUseCase) {
        val cameraProvider = context.getCameraProvider()
        val preview = androidx.camera.core.Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val useCases = buildList {
            add(preview); add(imageCapture)
            analysisUseCase?.let { add(it) }
        }
        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases.toTypedArray())
            onCameraReady(imageCapture)
        }.onFailure { Log.e("CameraPreview", "bind failed", it) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get(), null) }, ContextCompat.getMainExecutor(this))
    }
```

- [ ] **Step 2: Build — verify `BUILD SUCCESSFUL`**

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/souspantry/app/ui/camera/
git commit -m "feat: shared CameraPreview composable (CameraX + lifecycle binding)"
```

---

## Task 4: Barcode Scan Flow

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/pantry/BarcodeScanViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/pantry/BarcodeScanScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/ui/pantry/PantryScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`

- [ ] **Step 1: Create `BarcodeScanViewModel.kt`**

```kotlin
package com.souspantry.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BarcodeScanState {
    data object Scanning                             : BarcodeScanState
    data object Loading                              : BarcodeScanState
    data class  Result(val item: PantryItem)         : BarcodeScanState
    data class  Error(val message: String)           : BarcodeScanState
    data object Saved                                : BarcodeScanState
}

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BarcodeScanState>(BarcodeScanState.Scanning)
    val state = _state.asStateFlow()
    private var lastScanned = ""

    fun onBarcodeDetected(code: String) {
        if (code == lastScanned || _state.value is BarcodeScanState.Loading) return
        lastScanned = code
        viewModelScope.launch {
            _state.value = BarcodeScanState.Loading
            runCatching { api.lookupBarcode(code) }
                .onSuccess { r ->
                    _state.value = BarcodeScanState.Result(
                        PantryItem(name = r.name ?: "Unknown", brand = r.brand, category = r.category)
                    )
                }
                .onFailure { _state.value = BarcodeScanState.Error("Barcode not recognised. Add manually.") }
        }
    }

    fun saveItem(item: PantryItem) = viewModelScope.launch {
        repo.add(item)
        _state.value = BarcodeScanState.Saved
    }

    fun rescan() { lastScanned = ""; _state.value = BarcodeScanState.Scanning }
}
```

- [ ] **Step 2: Create `BarcodeScanScreen.kt`**

```kotlin
package com.souspantry.app.ui.pantry

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.souspantry.app.ui.camera.CameraPreview
import com.souspantry.app.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun BarcodeScanScreen(
    onDismiss : () -> Unit,
    onSaved   : () -> Unit,
    vm        : BarcodeScanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state) { if (state is BarcodeScanState.Saved) onSaved() }

    val analysisUseCase = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also { analysis ->
                val scanner  = BarcodeScanning.getClient()
                val executor = Executors.newSingleThreadExecutor()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val img = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(img)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                    ?.let { vm.onBarcodeDetected(it) }
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else proxy.close()
                }
            }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = {}, analysisUseCase = analysisUseCase)

        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Filled.Close, "Close", tint = Color.White)
        }

        if (state is BarcodeScanState.Scanning) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(0.7f)) {
                Text("Point at a barcode", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(16.dp))
            }
        }

        if (state is BarcodeScanState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        }

        if (state is BarcodeScanState.Result) {
            val item = (state as BarcodeScanState.Result).item
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = Cream) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Found: ${item.name}", style = MaterialTheme.typography.headlineMedium)
                    item.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    item.category?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.rescan() }, Modifier.weight(1f)) { Text("Rescan") }
                        Button(onClick = { vm.saveItem(item) }, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add to Pantry") }
                    }
                }
            }
        }

        if (state is BarcodeScanState.Error) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = Cream) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text((state as BarcodeScanState.Error).message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { vm.rescan() }, Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Try Again") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update `PantryScreen.kt` — add scan params and multi-FAB**

Change `PantryScreen` signature:
```kotlin
@Composable
fun PantryScreen(
    onBarcodeScan : () -> Unit = {},
    onReceiptScan : () -> Unit = {},
    vm            : PantryViewModel = hiltViewModel(),
)
```

Replace the single FAB with:
```kotlin
floatingActionButton = {
    var menuOpen by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (menuOpen) {
            SmallFloatingActionButton(onClick = { onReceiptScan(); menuOpen = false }, containerColor = Navy, contentColor = Color.White) {
                Icon(Icons.Filled.Receipt, "Receipt")
            }
            SmallFloatingActionButton(onClick = { onBarcodeScan(); menuOpen = false }, containerColor = Navy, contentColor = Color.White) {
                Icon(Icons.Filled.QrCodeScanner, "Barcode")
            }
        }
        FloatingActionButton(
            onClick = { if (menuOpen) menuOpen = false else menuOpen = true },
            containerColor = Green, contentColor = Color.White, shape = CircleShape,
        ) { Icon(if (menuOpen) Icons.Filled.Close else Icons.Filled.Add, "Menu") }
    }
},
```

Add imports:
```kotlin
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
```

Also add camera permission request at top of `PantryScreen` body (before `Scaffold`):
```kotlin
val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
LaunchedEffect(Unit) { if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest() }
```
Add import: `import com.google.accompanist.permissions.*`

- [ ] **Step 4: Wire `Screen.Barcode` in `NavHost.kt` — replace placeholder**

```kotlin
composable(Screen.Barcode.route) {
    BarcodeScanScreen(
        onDismiss = { navController.popBackStack() },
        onSaved   = { navController.popBackStack() },
    )
}
```

- [ ] **Step 5: Build and run. Pantry → FAB → barcode icon → point at barcode → item shown → Add to Pantry → item in list.**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/souspantry/app/ui/pantry/BarcodeScan* \
        app/src/main/java/com/souspantry/app/ui/pantry/PantryScreen.kt \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt
git commit -m "feat: barcode scan — CameraX ML Kit continuous scan + API lookup + pantry save"
```

---

## Task 5: Receipt Scan Flow

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/pantry/ReceiptScanViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/pantry/ReceiptScanScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`

- [ ] **Step 1: Create `ReceiptScanViewModel.kt`**

```kotlin
package com.souspantry.app.ui.pantry

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

sealed interface ReceiptScanState {
    data object Ready                                            : ReceiptScanState
    data object Loading                                          : ReceiptScanState
    data class  Results(val items: List<ReceiptLineItem>)        : ReceiptScanState
    data class  Error(val message: String)                       : ReceiptScanState
    data object Saved                                            : ReceiptScanState
}

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Ready)
    val state = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) = viewModelScope.launch {
        _state.value = ReceiptScanState.Loading
        val baos   = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        runCatching { api.parseReceiptImage(mapOf("image" to base64)) }
            .onSuccess { _state.value = ReceiptScanState.Results(it) }
            .onFailure { _state.value = ReceiptScanState.Error("Couldn't read receipt. Try again.") }
    }

    fun saveAll(items: List<ReceiptLineItem>) = viewModelScope.launch {
        repo.addAll(items.map { PantryItem(name = it.name, category = it.category) })
        _state.value = ReceiptScanState.Saved
    }

    fun retry() { _state.value = ReceiptScanState.Ready }
}
```

- [ ] **Step 2: Create `ReceiptScanScreen.kt`**

```kotlin
package com.souspantry.app.ui.pantry

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.camera.CameraPreview
import com.souspantry.app.ui.theme.*
import java.io.File

@Composable
fun ReceiptScanScreen(
    onDismiss : () -> Unit,
    onSaved   : () -> Unit,
    vm        : ReceiptScanViewModel = hiltViewModel(),
) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    var capture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(state) { if (state is ReceiptScanState.Saved) onSaved() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (state is ReceiptScanState.Ready) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = { capture = it })

            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White)
            }

            FloatingActionButton(
                onClick = {
                    val file    = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture?.takePicture(options, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    ?.let { vm.processImage(it) }
                            }
                            override fun onError(e: ImageCaptureException) {
                                /* will surface as Error state on retry */
                            }
                        })
                },
                containerColor = Green,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            ) { Icon(Icons.Filled.CameraAlt, "Capture") }

            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(0.6f)) {
                Text("Point at receipt and tap capture",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    modifier = Modifier.padding(12.dp))
            }
        }

        if (state is ReceiptScanState.Loading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Reading receipt…", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                }
            }
        }

        if (state is ReceiptScanState.Results) {
            val items = (state as ReceiptScanState.Results).items
            Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
                Column(Modifier.padding(20.dp)) {
                    Text("Found ${items.size} items", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(items) { line ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(line.name, style = MaterialTheme.typography.titleMedium)
                                    line.category?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.retry() }, Modifier.weight(1f)) { Text("Retake") }
                        Button(onClick = { vm.saveAll(items) }, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add All") }
                    }
                }
            }
        }

        if (state is ReceiptScanState.Error) {
            Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((state as ReceiptScanState.Error).message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.retry() }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Wire `Screen.Receipt` in `NavHost.kt`**

```kotlin
composable(Screen.Receipt.route) {
    ReceiptScanScreen(
        onDismiss = { navController.popBackStack() },
        onSaved   = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: Build and run. Pantry → FAB → receipt icon → capture → parsed items list → Add All → items in pantry.**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/souspantry/app/ui/pantry/ReceiptScan* \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt
git commit -m "feat: receipt scan — capture image + base64 API call + bulk pantry save"
```

---

## Task 6: FCM Push Notifications

**Prerequisite:** Set up Firebase project:
1. [console.firebase.google.com](https://console.firebase.google.com) → Create project "SousPantry"
2. Add Android app → package name `com.souspantry.app`
3. Download `google-services.json` → place at `app/google-services.json`
4. Then proceed with steps below.

**Files:**
- Create: `app/src/main/java/com/souspantry/app/services/messaging/FcmService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `app/src/main/java/com/souspantry/app/MainActivity.kt`

- [ ] **Step 1: Re-enable Firebase in `app/build.gradle.kts`** — uncomment these 3 lines:

```kotlin
alias(libs.plugins.google.services)           // in plugins block
implementation(platform(libs.firebase.bom))   // in dependencies block
implementation(libs.firebase.messaging)        // in dependencies block
```

Also uncomment in root `build.gradle.kts`:
```kotlin
alias(libs.plugins.google.services) apply false
```

- [ ] **Step 2: Create `FcmService.kt`**

```kotlin
package com.souspantry.app.services.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.souspantry.app.MainActivity
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var prefs: UserPreferencesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            val deviceId = prefs.deviceId.first()
            android.util.Log.d("FCM", "token for device=$deviceId: $token")
            // Stage 2: POST /api/fcm/register { deviceId, token }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notifOn = runCatching { kotlinx.coroutines.runBlocking { prefs.notifEnabled.first() } }.getOrDefault(true)
        if (!notifOn) return
        val title = message.notification?.title ?: message.data["title"] ?: "Sous Pantry"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "sous_pantry_general"
        val mgr       = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(NotificationChannel(channelId, "Sous Pantry", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pi     = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif  = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title).setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true).setContentIntent(pi).build()
        mgr.notify(System.currentTimeMillis().toInt(), notif)
    }

    override fun onDestroy() { super.onDestroy(); scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
}
```

- [ ] **Step 3: Register service + permission in `AndroidManifest.xml`**

Add inside `<manifest>` before `<application>`:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Add inside `<application>` after the `<activity>` block:
```xml
<service android:name=".services.messaging.FcmService" android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

- [ ] **Step 4: Request notification permission in `MainActivity.kt`**

Replace `MainActivity.kt` with:
```kotlin
package com.souspantry.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.souspantry.app.navigation.SousPantryNavHost
import com.souspantry.app.ui.theme.SousPantryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent { SousPantryTheme { SousPantryNavHost() } }
    }
}
```

- [ ] **Step 5: Build and run. Check Logcat tag `FCM` — should see `token for device=<uuid>: <token>` on first launch.**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/souspantry/app/services/messaging/ \
        app/src/main/AndroidManifest.xml \
        app/src/main/java/com/souspantry/app/MainActivity.kt \
        app/build.gradle.kts build.gradle.kts \
        app/google-services.json
git commit -m "feat: FCM push notifications — token registration + notification display"
```

---

## Task 7: Supabase Auth (Stage 2)

**Files:**
- Modify: `gradle/libs.versions.toml`
- Create: `app/src/main/java/com/souspantry/app/data/auth/SessionManager.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/auth/AuthViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/auth/AuthScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/services/NetworkModule.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`

- [ ] **Step 1: Add Supabase to `gradle/libs.versions.toml`**

In `[versions]`:
```toml
supabase = "3.0.2"
ktor     = "3.0.3"
```

In `[libraries]`:
```toml
supabase-auth  = { group = "io.github.jan-tennert.supabase", name = "auth-kt",           version.ref = "supabase" }
ktor-android   = { group = "io.ktor",                         name = "ktor-client-android", version.ref = "ktor" }
```

In `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.supabase.auth)
implementation(libs.ktor.android)
```

- [ ] **Step 2: Add Supabase config to `local.properties`**

```properties
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

Add to `app/build.gradle.kts` `defaultConfig {}`:
```kotlin
buildConfigField("String", "SUPABASE_URL",      "\"${localProps.getProperty("SUPABASE_URL") ?: ""}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY") ?: ""}\"")
```

- [ ] **Step 3: Create `SessionManager.kt`**

```kotlin
package com.souspantry.app.data.auth

import com.souspantry.app.BuildConfig
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val supabase : SupabaseClient,
    private val prefs    : UserPreferencesRepository,
) {
    fun currentUser() = supabase.auth.currentUserOrNull()
    fun currentToken() = supabase.auth.currentSessionOrNull()?.accessToken

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) { this.email = email; this.password = password }
        persistSession()
    }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) { this.email = email; this.password = password }
        persistSession()
    }

    suspend fun signOut() {
        supabase.auth.signOut()
        prefs.clearSupabaseSession()
    }

    suspend fun restoreSession() {
        val token = prefs.supabaseToken.first() ?: return
        runCatching { supabase.auth.retrieveUser(token) }
    }

    private suspend fun persistSession() {
        val s = supabase.auth.currentSessionOrNull() ?: return
        prefs.setSupabaseSession(s.accessToken, s.user?.id ?: "")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl     = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
    ) { install(Auth) }

    @Provides @Singleton
    fun provideSessionManager(client: SupabaseClient, prefs: UserPreferencesRepository): SessionManager =
        SessionManager(client, prefs)
}
```

- [ ] **Step 4: Create `AuthViewModel.kt`**

```kotlin
package com.souspantry.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(val isLoading: Boolean = false, val error: String? = null, val success: Boolean = false)

@HiltViewModel
class AuthViewModel @Inject constructor(private val session: SessionManager) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { session.signIn(email, password) }
            .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") } }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { session.signUp(email, password) }
            .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Sign up failed") } }
    }
}
```

- [ ] **Step 5: Create `AuthScreen.kt`**

```kotlin
package com.souspantry.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val state        by vm.state.collectAsState()
    var tab          by remember { mutableIntStateOf(0) }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }

    LaunchedEffect(state.success) { if (state.success) onAuthSuccess() }

    Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sous Pantry", style = MaterialTheme.typography.headlineLarge)
                TabRow(selectedTabIndex = tab, containerColor = Color.White) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sign In") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Create Account") })
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                state.error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodyMedium) }
                Button(
                    onClick  = { if (tab == 0) vm.signIn(email, password) else vm.signUp(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Green),
                    enabled  = !state.isLoading && email.isNotBlank() && password.length >= 6,
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(if (tab == 0) "Sign In" else "Create Account")
                }
            }
        }
    }
}
```

- [ ] **Step 6: Add JWT interceptor to `NetworkModule.kt`**

Replace `provideOkHttpClient()`:
```kotlin
@Provides @Singleton
fun provideOkHttpClient(sessionManager: dagger.Lazy<SessionManager>): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token   = runCatching { sessionManager.get().currentToken() }.getOrNull()
            val request = if (token != null)
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            else chain.request()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
```

Add `import com.souspantry.app.data.auth.SessionManager` to `NetworkModule.kt`.

- [ ] **Step 7: Wire `Screen.Auth` in `NavHost.kt`**

```kotlin
composable(Screen.Auth.route) {
    AuthScreen(onAuthSuccess = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Auth.route) { inclusive = true }
        }
    })
}
```

- [ ] **Step 8: Build and run. Navigate to Auth (Home → Settings → "Sign In" button — add one to SettingsScreen for testing). Sign in with a Supabase test account. Verify JWT appears in Logcat on API calls.**

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/souspantry/app/data/auth/ \
        app/src/main/java/com/souspantry/app/ui/auth/ \
        app/src/main/java/com/souspantry/app/services/NetworkModule.kt \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt \
        gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: Supabase auth — email/password login + JWT interceptor for API calls"
```

---

## Task 8: Google Play Billing / Paywall (Stage 2)

**Prerequisite:** Play Console → your app → Monetize → Subscriptions → create product ID `sous_pantry_premium_monthly`. Requires an internal testing track build uploaded first.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Create: `app/src/main/java/com/souspantry/app/data/billing/BillingManager.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/paywall/PaywallViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/paywall/PaywallScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`

- [ ] **Step 1: Add GPB to `gradle/libs.versions.toml`**

In `[versions]`: `billing = "7.1.1"`
In `[libraries]`: `billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }`
In `app/build.gradle.kts` dependencies: `implementation(libs.billing)`

- [ ] **Step 2: Create `BillingManager.kt`**

```kotlin
package com.souspantry.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

const val PRODUCT_ID = "sous_pantry_premium_monthly"

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private lateinit var client: BillingClient

    fun init() {
        client = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) { if (r.responseCode == BillingClient.BillingResponseCode.OK) queryPurchases() }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryPurchases() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { _, purchases ->
            _isPremium.value = purchases.any { it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    suspend fun getProductDetails(): ProductDetails? = suspendCancellableCoroutine { cont ->
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.SUBS).build()
            )).build()
        ) { _, details -> cont.resume(details.firstOrNull()) }
    }

    fun launchBillingFlow(activity: Activity, details: ProductDetails) {
        val token = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(token).build()
            )).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                ?.forEach { client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) {} }
            queryPurchases()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides @Singleton
    fun provideBillingManager(@ApplicationContext ctx: Context): BillingManager = BillingManager(ctx).also { it.init() }
}
```

- [ ] **Step 3: Create `PaywallViewModel.kt`**

```kotlin
package com.souspantry.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.souspantry.app.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallState(
    val isPremium     : Boolean         = false,
    val productDetails: ProductDetails? = null,
    val isLoading     : Boolean         = false,
    val error         : String?         = null,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(private val billing: BillingManager) : ViewModel() {
    private val _extra = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = combine(billing.isPremium, _extra) { premium, e -> e.copy(isPremium = premium) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaywallState())

    init { viewModelScope.launch {
        _extra.update { it.copy(isLoading = true) }
        runCatching { billing.getProductDetails() }
            .onSuccess { d -> _extra.update { it.copy(productDetails = d, isLoading = false) } }
            .onFailure { _extra.update { it.copy(error = "Couldn't load pricing.", isLoading = false) } }
    } }

    fun purchase(activity: Activity) { billing.launchBillingFlow(activity, _extra.value.productDetails ?: return) }
}
```

- [ ] **Step 4: Create `PaywallScreen.kt`**

```kotlin
package com.souspantry.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(onBack: () -> Unit, vm: PaywallViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsState()
    val activity  = LocalContext.current as Activity
    LaunchedEffect(state.isPremium) { if (state.isPremium) onBack() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sous Pantry Premium") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = White, navigationIconContentColor = White)) },
        containerColor = Cream,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Navy, Green)), RoundedCornerShape(16.dp)).padding(24.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", style = MaterialTheme.typography.headlineLarge)
                    Text("Unlock Everything", style = MaterialTheme.typography.headlineMedium.copy(color = White))
                    Text("Unlimited AI meal plans, receipt scans & more", style = MaterialTheme.typography.bodyMedium.copy(color = White.copy(0.8f)))
                }
            }
            listOf("Unlimited AI meal planning", "Unlimited receipt scanning", "Smart shopping lists", "Priority support").forEach {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Check, null, tint = Green)
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.weight(1f))
            state.productDetails?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                ?.let { Text("$it / month", style = MaterialTheme.typography.headlineMedium, color = Navy) }
            state.error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodyMedium) }
            Button(
                onClick  = { vm.purchase(activity) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                enabled  = !state.isLoading && state.productDetails != null,
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Subscribe Now", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Maybe later", color = Slate) }
        }
    }
}
```

- [ ] **Step 5: Wire `Screen.Paywall` in `NavHost.kt`**

```kotlin
composable(Screen.Paywall.route) { PaywallScreen(onBack = { navController.popBackStack() }) }
```

- [ ] **Step 6: ⚠️ Testing note — billing only works on a signed build from Play Console internal track. Build signed release APK, upload to Play Console → Internal Testing, install via Play Store app on device. Then test purchase flow.**

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/souspantry/app/data/billing/ \
        app/src/main/java/com/souspantry/app/ui/paywall/ \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt \
        gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: Google Play Billing v7 paywall — subscription check + purchase flow"
```

---

## Task 9: Onboarding (Stage 3)

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/onboarding/OnboardingViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`

- [ ] **Step 1: Create `OnboardingViewModel.kt`**

```kotlin
package com.souspantry.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    /** true = skip onboarding, false = show it. Defaults to true while loading so we don't flash onboarding. */
    val onboardingDone = prefs.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    fun complete() = viewModelScope.launch { prefs.setOnboardingDone() }
}
```

- [ ] **Step 2: Create `OnboardingScreen.kt`**

```kotlin
package com.souspantry.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

private data class OnboardingPage(val emoji: String, val title: String, val description: String)

private val pages = listOf(
    OnboardingPage("🥘", "Your AI Kitchen",    "Sous Pantry uses AI to suggest meals from what you already have."),
    OnboardingPage("📷", "Scan & Track",       "Scan barcodes or receipts to instantly add items to your pantry."),
    OnboardingPage("🛒", "Smart Shopping",     "Get a personalised shopping list to fill the gaps in your pantry."),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete : () -> Unit,
    vm         : OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Green)))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.emoji, fontSize = 72.sp)
                Spacer(Modifier.height(32.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium.copy(color = White), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(p.description, style = MaterialTheme.typography.bodyLarge.copy(color = White.copy(0.8f)), textAlign = TextAlign.Center)
            }
        }

        // Dots
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                Box(Modifier.size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == pagerState.currentPage) White else White.copy(0.4f)))
            }
        }

        Button(
            onClick  = {
                if (pagerState.currentPage == pages.size - 1) { vm.complete(); onComplete() }
                // Swipe to advance — button only triggers on last page
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp).height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = White),
        ) {
            Text(if (pagerState.currentPage < pages.size - 1) "Swipe to continue" else "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(color = Navy))
        }
    }
}
```

- [ ] **Step 3: Update `NavHost.kt` — read onboarding flag and set start destination**

In `SousPantryNavHost()`, add at the top of the composable:
```kotlin
val onboardingVm: OnboardingViewModel = hiltViewModel()
val onboardingDone by onboardingVm.onboardingDone.collectAsState()
val startDest = if (onboardingDone) Screen.Home.route else Screen.Onboarding.route
```

Pass `startDest` to `NavHost`:
```kotlin
NavHost(navController = navController, startDestination = startDest, modifier = Modifier.padding(innerPadding)) {
```

Wire `Screen.Onboarding`:
```kotlin
composable(Screen.Onboarding.route) {
    OnboardingScreen(onComplete = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
        }
    })
}
```

Add imports:
```kotlin
import com.souspantry.app.ui.onboarding.OnboardingScreen
import com.souspantry.app.ui.onboarding.OnboardingViewModel
import androidx.hilt.navigation.compose.hiltViewModel
```

- [ ] **Step 4: Test — clear app data (Settings → Apps → Sous Pantry → Clear Data). Launch → onboarding shows. Swipe pages → "Get Started" → Home. Re-launch → Home directly (no onboarding).**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/souspantry/app/ui/onboarding/ \
        app/src/main/java/com/souspantry/app/navigation/NavHost.kt
git commit -m "feat: 3-page onboarding with HorizontalPager — shown once on first launch"
```

---

## Task 10: Day 10 — Polish & Internal Test Build

- [ ] **Replace deprecated Unsplash source URL** in `HomeScreen.kt` and `PlanCookScreen.kt`. Change `https://source.unsplash.com/400x300/?${query},food` to `https://loremflickr.com/400/300/${query},food` (no API key needed, stable service).

- [ ] **Remove `usesCleartextTraffic="true"` from `AndroidManifest.xml`** — replace with a network security config that only allows cleartext for the emulator IP. Create `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

In `AndroidManifest.xml` `<application>`:
```xml
android:networkSecurityConfig="@xml/network_security_config"
```
Remove `android:usesCleartextTraffic="true"`.

- [ ] **Generate signed release keystore** (once, store outside repo):

```bash
keytool -genkey -v -keystore ~/sous-pantry-release.jks \
  -alias sous_pantry -keyalg RSA -keysize 2048 -validity 10000
```

Add `signingConfigs` to `app/build.gradle.kts` `android {}`:
```kotlin
signingConfigs {
    create("release") {
        storeFile     = file("${System.getProperty("user.home")}/sous-pantry-release.jks")
        storePassword = localProps.getProperty("KEYSTORE_PASSWORD") ?: ""
        keyAlias      = "sous_pantry"
        keyPassword   = localProps.getProperty("KEY_PASSWORD") ?: ""
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // existing proguard config …
    }
}
```

Add to `local.properties`:
```properties
KEYSTORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```

- [ ] **Build signed release APK:**

```bash
./gradlew assembleRelease
# APK at: app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

- [ ] **Manual smoke test on device (signed release build):**
  - [ ] Cold launch — correct start screen (onboarding if fresh install, home if returning)
  - [ ] Home — all 4 recipe sections load
  - [ ] Pantry — add item manually → appears in list
  - [ ] Pantry — barcode scan → item identified → added
  - [ ] Pantry — receipt scan → items parsed → Add All → pantry updated
  - [ ] Shopping — generate list → items appear → toggle check off items
  - [ ] Plan & Cook — Generate Meal Plan → meal cards expand/collapse with ingredients + instructions
  - [ ] Settings — save name → restart → name persists; toggle notifications
  - [ ] Auth — sign in with Supabase test account → home screen
  - [ ] Paywall — opens → pricing displayed (requires Play Console internal track build)

- [ ] **Final commit:**

```bash
git add -A
git commit -m "chore: day 10 — network security config, signed release build, smoke test pass"
```

---

## Spec Coverage

| Spec requirement | Task |
|---|---|
| Pantry CRUD | Existing scaffold + Tasks 4, 5 |
| Barcode scan | Task 4 |
| Receipt scan | Task 5 |
| Home (4 recipe sections) | Existing scaffold |
| Shopping list | Existing scaffold |
| Plan & Cook | Existing scaffold |
| Settings/Profile | Task 2 |
| FCM push | Task 6 |
| Supabase Auth | Task 7 |
| Google Play Billing | Task 8 |
| Onboarding | Task 9 |
| No dark mode | ✅ absent |
| No geofence | ✅ absent |
