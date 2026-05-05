# Sous Pantry Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full native Android port of Sous Pantry with 4 core screens, Settings, FCM push (Stage 1), Supabase auth + Google Play Billing (Stage 2), and Onboarding (Stage 3).

**Architecture:** MVVM + Hilt DI + Jetpack Compose. ViewModels hold `StateFlow<UiState>` and call `ApiService` (Retrofit) or `PantryRepository` (Room). Screens are stateless Compose functions that observe state and dispatch events.

**Tech Stack:** Kotlin 2.1 · Jetpack Compose · Hilt · Retrofit + OkHttp · Room · DataStore · CameraX · ML Kit · FCM · Supabase Kotlin SDK · Google Play Billing v7

---

## File Map

```
app/src/main/java/com/souspantry/app/
  data/
    local/
      DataStoreModule.kt          ← NEW: DataStore Hilt provider
    models/
      Models.kt                   ← EXISTING: add UserPrefs data class
    repository/
      PantryDatabase.kt           ← EXISTING: no change
      PantryRepository.kt         ← EXISTING: no change
    auth/
      AuthRepository.kt           ← NEW (Stage 2): Supabase auth wrapper
    billing/
      BillingRepository.kt        ← NEW (Stage 2): GPB v7 wrapper
  services/
    ApiService.kt                 ← EXISTING: no change
    NetworkModule.kt              ← MODIFY (Stage 2): add AuthInterceptor
    AuthModule.kt                 ← NEW (Stage 2): Supabase client Hilt provider
    AuthInterceptor.kt            ← NEW (Stage 2): OkHttp JWT interceptor
    BillingModule.kt              ← NEW (Stage 2): BillingClient Hilt provider
    messaging/
      FcmService.kt               ← NEW: FirebaseMessagingService
  ui/
    home/
      HomeScreen.kt               ← MODIFY: polish retry buttons
      HomeViewModel.kt            ← EXISTING: no change
    pantry/
      PantryScreen.kt             ← MODIFY: add camera launch buttons
      PantryViewModel.kt          ← MODIFY: add scan methods
      CameraScreen.kt             ← NEW: reusable CameraX composable
    shopping/
      ShoppingScreen.kt           ← MODIFY: polish empty/error states
      ShoppingViewModel.kt        ← EXISTING: no change
    plancook/
      PlanCookScreen.kt           ← MODIFY: polish expand/collapse
      PlanCookViewModel.kt        ← EXISTING: no change
    settings/
      SettingsScreen.kt           ← NEW
      SettingsViewModel.kt        ← NEW
    auth/
      AuthViewModel.kt            ← NEW (Stage 2)
      LoginScreen.kt              ← NEW (Stage 2)
      SignUpScreen.kt             ← NEW (Stage 2)
    paywall/
      PaywallScreen.kt            ← NEW (Stage 2)
      PaywallViewModel.kt         ← NEW (Stage 2)
    onboarding/
      OnboardingScreen.kt         ← NEW (Stage 3)
    theme/
      Theme.kt                    ← EXISTING: no change
      Type.kt                     ← EXISTING: no change
  navigation/
    NavHost.kt                    ← MODIFY: add Settings, auth flow, onboarding gate
  MainActivity.kt                 ← MODIFY (Stage 3): onboarding check
  SousPantryApp.kt                ← EXISTING: no change

app/src/main/
  AndroidManifest.xml             ← MODIFY: FCM service, POST_NOTIFICATIONS permission
  res/values/strings.xml          ← NEW: app strings

app/src/test/java/com/souspantry/app/
  ui/settings/SettingsViewModelTest.kt   ← NEW
  ui/pantry/PantryViewModelTest.kt       ← MODIFY: add scan tests
  ui/auth/AuthViewModelTest.kt           ← NEW (Stage 2)
  ui/paywall/PaywallViewModelTest.kt     ← NEW (Stage 2)

gradle/libs.versions.toml               ← MODIFY: add test deps, supabase, billing
app/build.gradle.kts                    ← MODIFY: add test deps, supabase, billing
```

---

## Stage 1 — Days 2–6

---

### Task 1: Add test dependencies + DataStore module

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/souspantry/app/data/local/DataStoreModule.kt`

- [ ] **Step 1: Add test + DataStore versions to libs.versions.toml**

In `gradle/libs.versions.toml`, add to `[versions]`:
```toml
junit4            = "4.13.2"
mockk             = "1.13.12"
coroutines-test   = "1.9.0"
turbine           = "1.2.0"
```

Add to `[libraries]`:
```toml
# Testing
junit4              = { group = "junit",                    name = "junit",                          version.ref = "junit4" }
mockk               = { group = "io.mockk",                name = "mockk",                          version.ref = "mockk" }
coroutines-test     = { group = "org.jetbrains.kotlinx",   name = "kotlinx-coroutines-test",        version.ref = "coroutines-test" }
turbine             = { group = "app.cash.turbine",        name = "turbine",                        version.ref = "turbine" }
```

- [ ] **Step 2: Add test deps to app/build.gradle.kts**

Inside the `dependencies {}` block, append:
```kotlin
    // Testing
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
```

- [ ] **Step 3: Create DataStoreModule.kt**

Create `app/src/main/java/com/souspantry/app/data/local/DataStoreModule.kt`:
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
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sous_pantry_prefs")

object PrefKeys {
    val USER_NAME          = stringPreferencesKey("user_name")
    val DEVICE_ID          = stringPreferencesKey("device_id")
    val FCM_TOKEN          = stringPreferencesKey("fcm_token")
    val ONBOARDING_DONE    = booleanPreferencesKey("onboarding_done")
    val SUPABASE_SESSION   = stringPreferencesKey("supabase_session")
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        ctx.dataStore
}

class AppPreferences @javax.inject.Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val userName: Flow<String>    = dataStore.data.map { it[PrefKeys.USER_NAME] ?: "" }
    val deviceId: Flow<String>    = dataStore.data.map { it[PrefKeys.DEVICE_ID] ?: "" }
    val fcmToken: Flow<String>    = dataStore.data.map { it[PrefKeys.FCM_TOKEN] ?: "" }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[PrefKeys.ONBOARDING_DONE] ?: false }
    val supabaseSession: Flow<String> = dataStore.data.map { it[PrefKeys.SUPABASE_SESSION] ?: "" }

    suspend fun setUserName(name: String)        = dataStore.edit { it[PrefKeys.USER_NAME] = name }
    suspend fun setDeviceId(id: String)          = dataStore.edit { it[PrefKeys.DEVICE_ID] = id }
    suspend fun setFcmToken(token: String)       = dataStore.edit { it[PrefKeys.FCM_TOKEN] = token }
    suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[PrefKeys.ONBOARDING_DONE] = done }
    suspend fun setSupabaseSession(json: String) = dataStore.edit { it[PrefKeys.SUPABASE_SESSION] = json }
    suspend fun clearSession()                   = dataStore.edit { it.remove(PrefKeys.SUPABASE_SESSION) }
}
```

Also add `AppPreferences` to the Hilt graph by adding this to `DataStoreModule`:
```kotlin
    @Provides @Singleton
    fun provideAppPreferences(dataStore: DataStore<Preferences>): AppPreferences =
        AppPreferences(dataStore)
```

- [ ] **Step 4: Sync and verify no red errors in Android Studio**

Click **File → Sync Project with Gradle Files**. Build panel should show no errors.

- [ ] **Step 5: Commit**
```bash
git add -A && git commit -m "feat: add test deps and DataStore module"
```

---

### Task 2: Settings screen

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/test/java/com/souspantry/app/ui/settings/SettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write the failing SettingsViewModel test**

Create `app/src/test/java/com/souspantry/app/ui/settings/SettingsViewModelTest.kt`:
```kotlin
package com.souspantry.app.ui.settings

import app.cash.turbine.test
import com.souspantry.app.data.local.AppPreferences
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val prefs = mockk<AppPreferences>(relaxed = true)
    private lateinit var vm: SettingsViewModel

    @Before fun setUp() {
        every { prefs.userName } returns flowOf("Alice")
        every { prefs.fcmToken } returns flowOf("tok123")
        vm = SettingsViewModel(prefs)
    }

    @Test fun `userName emits from prefs`() = runTest {
        vm.userName.test {
            assertEquals("Alice", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `saveUserName calls prefs setUserName`() = runTest {
        coEvery { prefs.setUserName(any()) } just Runs
        vm.saveUserName("Bob")
        coVerify { prefs.setUserName("Bob") }
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (class missing)**
```bash
cd /Users/jf/Documents/SousPantry/SousPantry_Android
./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest" 2>&1 | tail -20
```
Expected: compilation error — `SettingsViewModel` not found.

- [ ] **Step 3: Create SettingsViewModel.kt**

Create `app/src/main/java/com/souspantry/app/ui/settings/SettingsViewModel.kt`:
```kotlin
package com.souspantry.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    val userName = prefs.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val fcmToken = prefs.fcmToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveUserName(name: String) = viewModelScope.launch {
        prefs.setUserName(name.trim())
    }
}
```

- [ ] **Step 4: Run test — expect PASS**
```bash
./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest" 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Create strings.xml**

Create `app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Sous Pantry</string>
    <string name="tab_home">Home</string>
    <string name="tab_pantry">Pantry</string>
    <string name="tab_plan_cook">Plan &amp; Cook</string>
    <string name="tab_shopping">Shopping</string>
    <string name="tab_settings">Settings</string>
    <string name="settings_title">Settings</string>
    <string name="settings_your_name">Your name</string>
    <string name="settings_save">Save</string>
    <string name="settings_fcm_debug">Push token (debug)</string>
    <string name="retry">Retry</string>
    <string name="empty_pantry_title">Your pantry is empty</string>
    <string name="empty_pantry_subtitle">Add items with the + button</string>
    <string name="generating">Generating…</string>
    <string name="something_went_wrong">Something went wrong. Tap to retry.</string>
</resources>
```

- [ ] **Step 6: Create SettingsScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/settings/SettingsScreen.kt`:
```kotlin
package com.souspantry.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.BuildConfig
import com.souspantry.app.ui.theme.Cream
import com.souspantry.app.ui.theme.Navy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val userName by vm.userName.collectAsState()
    val fcmToken by vm.fcmToken.collectAsState()
    var nameInput by remember(userName) { mutableStateOf(userName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Navy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        },
        containerColor = Cream
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Profile", style = MaterialTheme.typography.titleMedium, color = Navy)

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Your name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.saveUserName(nameInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = nameInput.isNotBlank() && nameInput != userName
            ) {
                Text("Save")
            }

            if (BuildConfig.DEBUG && fcmToken.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Debug", style = MaterialTheme.typography.titleMedium, color = Navy)
                Text(
                    text = "FCM token:\n$fcmToken",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 7: Add Settings tab to NavHost.kt**

Modify `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`.

Replace the imports and sealed class section:
```kotlin
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
```

Replace the `Screen` sealed class:
```kotlin
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home      : Screen("home",      "Home",         Icons.Filled.Home)
    data object Pantry    : Screen("pantry",    "Pantry",       Icons.Filled.Kitchen)
    data object PlanCook  : Screen("plancook",  "Plan & Cook",  Icons.Filled.MenuBook)
    data object Shopping  : Screen("shopping",  "Shopping",     Icons.Filled.ShoppingCart)
    data object Settings  : Screen("settings",  "Settings",     Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home, Screen.Pantry, Screen.PlanCook, Screen.Shopping, Screen.Settings
)
```

Add the import at the top:
```kotlin
import com.souspantry.app.ui.settings.SettingsScreen
```

Add inside the `NavHost` composable block:
```kotlin
            composable(Screen.Settings.route) { SettingsScreen() }
```

- [ ] **Step 8: Run on device — verify Settings tab appears with name field**

In Android Studio: Run → select device → confirm 5 tabs in bottom nav, Settings tab shows name field and save button.

- [ ] **Step 9: Commit**
```bash
git add -A && git commit -m "feat: add Settings screen with DataStore name persistence"
```

---

### Task 3: Polish HomeScreen error/retry states

**Files:**
- Modify: `app/src/main/java/com/souspantry/app/ui/home/HomeScreen.kt`

The existing HomeScreen renders sections with loading spinners but has no retry buttons on error. This task adds retry affordance.

- [ ] **Step 1: Read the current HomeScreen**
```bash
cat app/src/main/java/com/souspantry/app/ui/home/HomeScreen.kt
```

- [ ] **Step 2: Replace HomeScreen.kt with polished version**

Replace the full file at `app/src/main/java/com/souspantry/app/ui/home/HomeScreen.kt`:
```kotlin
package com.souspantry.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.souspantry.app.data.models.*
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sous Pantry", color = Navy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        },
        containerColor = Cream
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                RecipeSection(
                    title    = "Suggested for you",
                    loading  = state.suggestedLoading,
                    error    = state.suggestedError,
                    onRetry  = vm::loadSuggested,
                    content  = {
                        state.suggestedRecipes.forEach { SuggestedMealCard(it) }
                    }
                )
            }
            item {
                RecipeSection(
                    title    = "Trending",
                    loading  = state.trendingLoading,
                    error    = state.trendingError,
                    onRetry  = vm::loadTrending,
                    content  = {
                        state.trendingRecipes.forEach { TrendingCard(it) }
                    }
                )
            }
            item {
                RecipeSection(
                    title    = "From your pantry",
                    loading  = state.pantryLoading,
                    error    = state.pantryError,
                    onRetry  = vm::loadPantryMeals,
                    content  = {
                        state.pantryRecipes.forEach { SuggestedMealCard(it) }
                    }
                )
            }
            item {
                RecipeSection(
                    title    = "Adventurous picks",
                    loading  = state.adventurousLoading,
                    error    = state.adventurousError,
                    onRetry  = vm::loadAdventurous,
                    content  = {
                        state.adventurousRecipes.forEach { AdventurousCard(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecipeSection(
    title   : String,
    loading : Boolean,
    error   : Boolean,
    onRetry : () -> Unit,
    content : @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Navy)
        when {
            loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green, modifier = Modifier.size(32.dp))
            }
            error -> ErrorRetryRow(onRetry)
            else  -> content()
        }
    }
}

@Composable
private fun ErrorRetryRow(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Couldn't load", style = MaterialTheme.typography.bodyMedium, color = Slate)
        TextButton(onClick = onRetry) { Text("Retry", color = Green) }
    }
}

@Composable
private fun RecipeCard(
    title      : String,
    description: String,
    imageQuery : String,
    badge      : String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model             = "https://source.unsplash.com/featured/800x400?$imageQuery,food",
                contentDescription = title,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (badge != null) {
                    Surface(color = SoftMint, shape = RoundedCornerShape(4.dp)) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = Green,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = Navy,
                    maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Slate,
                    maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable private fun SuggestedMealCard(meal: SuggestedMeal) =
    RecipeCard(meal.title, meal.description, meal.imageQuery, badge = meal.cuisine)

@Composable private fun TrendingCard(recipe: TrendingRecipe) =
    RecipeCard(recipe.title, recipe.description, recipe.imageQuery, badge = recipe.platform)

@Composable private fun AdventurousCard(recipe: AdventurousRecipe) =
    RecipeCard(recipe.title, recipe.description, recipe.imageQuery, badge = "${recipe.matchPercent}% match")
```

- [ ] **Step 3: Run on device — verify retry buttons appear when network is off**

Turn off Wi-Fi on device, open app, confirm each section shows "Couldn't load / Retry" row. Re-enable Wi-Fi, tap Retry, confirm recipes load.

- [ ] **Step 4: Commit**
```bash
git add -A && git commit -m "feat: polish Home screen with retry affordance"
```

---

### Task 4: Polish ShoppingScreen + PlanCookScreen

**Files:**
- Modify: `app/src/main/java/com/souspantry/app/ui/shopping/ShoppingScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/ui/plancook/PlanCookScreen.kt`

- [ ] **Step 1: Read current ShoppingScreen**
```bash
cat app/src/main/java/com/souspantry/app/ui/shopping/ShoppingScreen.kt
```

- [ ] **Step 2: Replace ShoppingScreen.kt**

Replace full file at `app/src/main/java/com/souspantry/app/ui/shopping/ShoppingScreen.kt`:
```kotlin
package com.souspantry.app.ui.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(vm: ShoppingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List", color = Navy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
                actions = {
                    TextButton(onClick = vm::generate, enabled = !state.loading) {
                        Text(if (state.loading) "Generating…" else "Generate", color = Green)
                    }
                }
            )
        },
        containerColor = Cream
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(
                    color = Green,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = Slate)
                    Button(onClick = vm::generate) { Text("Retry") }
                }
                state.items.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No list yet", style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text(
                        if (state.pantryEmpty) "Add items to your pantry first, then generate a list."
                        else "Tap Generate to create a shopping list from your pantry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::generate) { Text("Generate list") }
                }
                else -> {
                    val essential   = state.items.filter { it.priority == "essential" }
                    val niceToHave  = state.items.filter { it.priority != "essential" }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (essential.isNotEmpty()) {
                            item { SectionHeader("Essential") }
                            items(essential) { ShoppingItemRow(it, vm::toggle) }
                        }
                        if (niceToHave.isNotEmpty()) {
                            item { SectionHeader("Nice to have") }
                            items(niceToHave) { ShoppingItemRow(it, vm::toggle) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = Navy,
        modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, onToggle: (ShoppingItem) -> Unit) {
    Card(
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.checked) SoftMint else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { onToggle(item) }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector        = if (item.checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = "toggle",
                    tint               = if (item.checked) Green else Slate
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text            = item.name,
                    style           = MaterialTheme.typography.bodyMedium,
                    color           = if (item.checked) Slate else Navy,
                    textDecoration  = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (!item.reason.isNullOrBlank()) {
                    Text(item.reason, style = MaterialTheme.typography.bodySmall, color = Slate)
                }
            }
            item.quantity?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Slate)
            }
        }
    }
}
```

- [ ] **Step 3: Read current PlanCookScreen**
```bash
cat app/src/main/java/com/souspantry/app/ui/plancook/PlanCookScreen.kt
```

- [ ] **Step 4: Replace PlanCookScreen.kt**

Replace full file at `app/src/main/java/com/souspantry/app/ui/plancook/PlanCookScreen.kt`:
```kotlin
package com.souspantry.app.ui.plancook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanCookScreen(vm: PlanCookViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan & Cook", color = Navy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
                actions = {
                    TextButton(onClick = vm::generate, enabled = !state.loading) {
                        Text(if (state.loading) "Generating…" else "Generate", color = Green)
                    }
                }
            )
        },
        containerColor = Cream
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(
                    color    = Green,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.error != null -> Column(
                    modifier                  = Modifier.align(Alignment.Center),
                    horizontalAlignment       = Alignment.CenterHorizontally,
                    verticalArrangement       = Arrangement.spacedBy(8.dp)
                ) {
                    Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = Slate)
                    Button(onClick = vm::generate) { Text("Retry") }
                }
                state.meals.isEmpty() -> Column(
                    modifier            = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No meal plan yet", style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text("Tap Generate to plan meals from your pantry.",
                        style = MaterialTheme.typography.bodyMedium, color = Slate)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = vm::generate) { Text("Generate plan") }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.meals) { MealCard(it) }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: SuggestedMeal) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier            = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(meal.title, style = MaterialTheme.typography.titleSmall, color = Navy)
                    Text("${meal.cuisine} · ${meal.prepTime} · ${meal.difficulty}",
                        style = MaterialTheme.typography.labelSmall, color = Slate)
                }
                Icon(
                    imageVector        = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint               = Slate
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(meal.description, style = MaterialTheme.typography.bodySmall, color = Slate)

                    Text("Ingredients", style = MaterialTheme.typography.labelMedium, color = Navy)
                    meal.ingredients.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = Navy)
                    }

                    Text("Instructions", style = MaterialTheme.typography.labelMedium, color = Navy)
                    meal.instructions.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, color = Navy)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run on device — verify expand/collapse works and empty states show**

Open Plan & Cook tab, tap Generate, wait for results, tap a card to expand/collapse.

- [ ] **Step 6: Commit**
```bash
git add -A && git commit -m "feat: polish Shopping and PlanCook screens"
```

---

### Task 5: CameraX composable + barcode/receipt scan in Pantry

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/pantry/CameraScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/ui/pantry/PantryViewModel.kt`
- Modify: `app/src/main/java/com/souspantry/app/ui/pantry/PantryScreen.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add POST_NOTIFICATIONS and READ_MEDIA permissions to AndroidManifest.xml**

Open `app/src/main/AndroidManifest.xml`. After the existing `<uses-permission>` tags, add:
```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Also in the `<application>` tag, ensure `android:usesCleartextTraffic="true"` is present (it already is in the scaffold).

- [ ] **Step 2: Create CameraScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/pantry/CameraScreen.kt`:
```kotlin
package com.souspantry.app.ui.pantry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.souspantry.app.ui.theme.Cream
import com.souspantry.app.ui.theme.Green
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

enum class CameraMode { BARCODE, RECEIPT, IDENTIFY }

@Composable
fun CameraScreen(
    mode    : CameraMode,
    onResult: (String) -> Unit,   // base64 jpeg for RECEIPT/IDENTIFY; raw barcode for BARCODE
    onClose : () -> Unit,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick  = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Cream)
        }

        val label = when (mode) {
            CameraMode.BARCODE  -> "Point at barcode"
            CameraMode.RECEIPT  -> "Capture receipt"
            CameraMode.IDENTIFY -> "Point at item"
        }

        Column(
            modifier            = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(label, color = Cream, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button
                    capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer = image.planes[0].buffer
                            val bytes  = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val bmp    = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val out    = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            val b64    = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                            image.close()
                            onResult(b64)
                        }
                        override fun onError(exc: ImageCaptureException) { onClose() }
                    })
                }
            ) {
                Text("Capture", color = Cream)
            }
        }
    }
}
```

- [ ] **Step 3: Expand PantryViewModel with scan methods**

Replace full file at `app/src/main/java/com/souspantry/app/ui/pantry/PantryViewModel.kt`:
```kotlin
package com.souspantry.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.*
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanState(
    val loading : Boolean   = false,
    val error   : String?   = null,
)

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repo: PantryRepository,
    private val api : ApiService,
) : ViewModel() {

    val items = repo.items

    private val _scanState = MutableStateFlow(ScanState())
    val scanState = _scanState.asStateFlow()

    fun add(item: PantryItem)    = viewModelScope.launch { repo.add(item) }
    fun update(item: PantryItem) = viewModelScope.launch { repo.update(item) }
    fun delete(item: PantryItem) = viewModelScope.launch { repo.delete(item) }

    fun identifyFromImage(base64Jpeg: String, onResult: (PantryItem) -> Unit) =
        viewModelScope.launch {
            _scanState.update { it.copy(loading = true, error = null) }
            runCatching { api.identifyItem(mapOf("image" to base64Jpeg)) }
                .onSuccess { r ->
                    _scanState.update { it.copy(loading = false) }
                    onResult(PantryItem(
                        name     = r.name ?: "Unknown item",
                        brand    = r.brand,
                        category = r.category,
                        quantity = r.quantity?.toIntOrNull() ?: 1,
                    ))
                }
                .onFailure { _scanState.update { it.copy(loading = false, error = "Couldn't identify item") } }
        }

    fun lookupBarcode(code: String, onResult: (PantryItem) -> Unit) =
        viewModelScope.launch {
            _scanState.update { it.copy(loading = true, error = null) }
            runCatching { api.lookupBarcode(code) }
                .onSuccess { r ->
                    _scanState.update { it.copy(loading = false) }
                    onResult(PantryItem(
                        name     = r.name ?: "Unknown item",
                        brand    = r.brand,
                        category = r.category,
                        quantity = r.quantity?.toIntOrNull() ?: 1,
                    ))
                }
                .onFailure { _scanState.update { it.copy(loading = false, error = "Barcode lookup failed") } }
        }

    fun parseReceiptImage(base64Jpeg: String, onResult: (List<PantryItem>) -> Unit) =
        viewModelScope.launch {
            _scanState.update { it.copy(loading = true, error = null) }
            runCatching { api.parseReceiptImage(mapOf("image" to base64Jpeg)) }
                .onSuccess { lines ->
                    _scanState.update { it.copy(loading = false) }
                    onResult(lines.map { PantryItem(name = it.name, category = it.category, quantity = it.quantity?.toIntOrNull() ?: 1) })
                }
                .onFailure { _scanState.update { it.copy(loading = false, error = "Receipt parsing failed") } }
        }

    fun clearScanError() = _scanState.update { it.copy(error = null) }
}
```

- [ ] **Step 4: Read current PantryScreen.kt to understand add sheet location**
```bash
cat app/src/main/java/com/souspantry/app/ui/pantry/PantryScreen.kt
```

- [ ] **Step 5: Add camera launch buttons to PantryScreen**

At the top of `PantryScreen.kt`, after existing imports, add:
```kotlin
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
```

Inside the `PantryScreen` composable, before the existing `Scaffold`, add state:
```kotlin
    var cameraMode    by remember { mutableStateOf<CameraMode?>(null) }
    val scanState     by vm.scanState.collectAsState()
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
```

In the `TopAppBar` actions area, add three icon buttons:
```kotlin
                actions = {
                    IconButton(onClick = {
                        if (cameraPermission.status.isGranted) cameraMode = CameraMode.IDENTIFY
                        else cameraPermission.launchPermissionRequest()
                    }) { Icon(Icons.Filled.CameraAlt, "Identify item", tint = Green) }
                    IconButton(onClick = {
                        if (cameraPermission.status.isGranted) cameraMode = CameraMode.BARCODE
                        else cameraPermission.launchPermissionRequest()
                    }) { Icon(Icons.Filled.QrCodeScanner, "Scan barcode", tint = Green) }
                    IconButton(onClick = {
                        if (cameraPermission.status.isGranted) cameraMode = CameraMode.RECEIPT
                        else cameraPermission.launchPermissionRequest()
                    }) { Icon(Icons.Filled.Receipt, "Scan receipt", tint = Green) }
                }
```

After the `Scaffold { }` block, add the camera overlay:
```kotlin
    cameraMode?.let { mode ->
        CameraScreen(
            mode     = mode,
            onClose  = { cameraMode = null },
            onResult = { result ->
                cameraMode = null
                when (mode) {
                    CameraMode.IDENTIFY -> vm.identifyFromImage(result) { item -> vm.add(item) }
                    CameraMode.BARCODE  -> vm.lookupBarcode(result)    { item -> vm.add(item) }
                    CameraMode.RECEIPT  -> vm.parseReceiptImage(result) { items -> items.forEach { vm.add(it) } }
                }
            }
        )
    }

    if (scanState.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green)
        }
    }

    scanState.error?.let { err ->
        LaunchedEffect(err) {
            kotlinx.coroutines.delay(3000)
            vm.clearScanError()
        }
        Snackbar(modifier = Modifier.padding(16.dp)) { Text(err) }
    }
```

Also add the `@OptIn(ExperimentalPermissionsApi::class)` annotation to the `PantryScreen` function.

- [ ] **Step 6: Run on device — test camera flows**

Build and run. In Pantry tab: tap camera icon → allow permission → see camera preview → tap Capture → item added to pantry list.

- [ ] **Step 7: Commit**
```bash
git add -A && git commit -m "feat: add camera identify/barcode/receipt scan to Pantry"
```

---

### Task 6: FCM Push Notifications

**Files:**
- Create: `app/src/main/java/com/souspantry/app/services/messaging/FcmService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts` (re-enable firebase)
- Modify: `build.gradle.kts` (re-enable google-services)

> **Prerequisite:** You need a Firebase project with an Android app registered.
> 1. Go to [console.firebase.google.com](https://console.firebase.google.com)
> 2. Create project "SousPantry" (or use existing)
> 3. Add Android app → package name: `com.souspantry.app`
> 4. Download `google-services.json` → place at `app/google-services.json`
> Only proceed once `app/google-services.json` exists.

- [ ] **Step 1: Re-enable google-services plugin in build.gradle.kts**

In `build.gradle.kts` (root), uncomment:
```kotlin
    alias(libs.plugins.google.services)     apply false
```

In `app/build.gradle.kts`, uncomment:
```kotlin
    alias(libs.plugins.google.services)
```

Also uncomment the Firebase deps:
```kotlin
    // Firebase (FCM push notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
```

- [ ] **Step 2: Create FcmService.kt**

Create `app/src/main/java/com/souspantry/app/services/messaging/FcmService.kt`:
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
import com.souspantry.app.R
import com.souspantry.app.data.local.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var prefs: AppPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { prefs.setFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Sous Pantry"
        val body  = message.notification?.body  ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "sous_pantry_default"
        val manager   = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Sous Pantry", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

- [ ] **Step 3: Register FcmService in AndroidManifest.xml**

Inside the `<application>` tag, add after the `<activity>` block:
```xml
        <service
            android:name=".services.messaging.FcmService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
```

- [ ] **Step 4: Request notification permission on first launch in MainActivity.kt**

Replace `MainActivity.kt`:
```kotlin
package com.souspantry.app

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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user chose allow or deny — no action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SousPantryTheme {
                SousPantryNavHost()
            }
        }
    }
}
```

- [ ] **Step 5: Sync project and run on device**

Sync Gradle (google-services plugin now active). Build and run. On first launch, device should prompt for notification permission. In Settings tab, confirm FCM token appears in debug section.

- [ ] **Step 6: Commit**
```bash
git add -A && git commit -m "feat: integrate FCM push notifications"
```

---

## Stage 2 — Days 7–8

---

### Task 7: Supabase Auth

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/souspantry/app/services/AuthModule.kt`
- Create: `app/src/main/java/com/souspantry/app/services/AuthInterceptor.kt`
- Modify: `app/src/main/java/com/souspantry/app/services/NetworkModule.kt`
- Create: `app/src/main/java/com/souspantry/app/data/auth/AuthRepository.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/auth/AuthViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/auth/LoginScreen.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/auth/SignUpScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`
- Create: `app/src/test/java/com/souspantry/app/ui/auth/AuthViewModelTest.kt`

> **Prerequisite:** Get your Supabase project URL and anon key from the iOS project or Supabase dashboard. They go in `local.properties`:
> ```
> SUPABASE_URL=https://xxxx.supabase.co
> SUPABASE_ANON_KEY=eyJ...
> ```

- [ ] **Step 1: Add Supabase deps to libs.versions.toml**

Add to `[versions]`:
```toml
supabase            = "2.5.4"
ktor                = "2.3.12"
```

Add to `[libraries]`:
```toml
# Supabase
supabase-gotrue     = { group = "io.github.jan-tennert.supabase", name = "gotrue-kt",          version.ref = "supabase" }
supabase-postgrest  = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt",       version.ref = "supabase" }
ktor-android        = { group = "io.ktor",                        name = "ktor-client-android", version.ref = "ktor" }
```

- [ ] **Step 2: Add Supabase deps to app/build.gradle.kts**

Add Supabase URL/key buildConfigFields (after BASE_URL field):
```kotlin
        val supabaseUrl     = localProps.getProperty("SUPABASE_URL")     ?: ""
        val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL",      "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
```

Add to `dependencies {}`:
```kotlin
    // Supabase auth
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.android)
```

- [ ] **Step 3: Create AuthModule.kt**

Create `app/src/main/java/com/souspantry/app/services/AuthModule.kt`:
```kotlin
package com.souspantry.app.services

import com.souspantry.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl  = BuildConfig.SUPABASE_URL,
            supabaseKey  = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(GoTrue)
        }
}
```

- [ ] **Step 4: Create AuthInterceptor.kt**

Create `app/src/main/java/com/souspantry/app/services/AuthInterceptor.kt`:
```kotlin
package com.souspantry.app.services

import com.souspantry.app.data.local.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val prefs: AppPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { prefs.supabaseSession.first() }
        val request = if (session.isNotEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $session")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

- [ ] **Step 5: Modify NetworkModule.kt to add AuthInterceptor**

Replace `provideOkHttpClient` in `NetworkModule.kt`:
```kotlin
    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
```

Add the `AuthInterceptor` import: `import com.souspantry.app.services.AuthInterceptor`

- [ ] **Step 6: Create AuthRepository.kt**

Create `app/src/main/java/com/souspantry/app/data/auth/AuthRepository.kt`:
```kotlin
package com.souspantry.app.data.auth

import com.souspantry.app.data.local.AppPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val prefs   : AppPreferences,
) {
    suspend fun signUp(email: String, password: String) {
        supabase.gotrue.signUpWith(Email) {
            this.email    = email
            this.password = password
        }
        persistSession()
    }

    suspend fun signIn(email: String, password: String) {
        supabase.gotrue.signInWith(Email) {
            this.email    = email
            this.password = password
        }
        persistSession()
    }

    suspend fun signOut() {
        supabase.gotrue.logout()
        prefs.clearSession()
    }

    fun isLoggedIn(): Boolean =
        supabase.gotrue.currentSessionOrNull() != null

    private suspend fun persistSession() {
        val token = supabase.gotrue.currentSessionOrNull()?.accessToken ?: return
        prefs.setSupabaseSession(token)
    }
}
```

- [ ] **Step 7: Write AuthViewModel test**

Create `app/src/test/java/com/souspantry/app/ui/auth/AuthViewModelTest.kt`:
```kotlin
package com.souspantry.app.ui.auth

import app.cash.turbine.test
import com.souspantry.app.data.auth.AuthRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val repo = mockk<AuthRepository>(relaxed = true)
    private lateinit var vm: AuthViewModel

    @Before fun setUp() { vm = AuthViewModel(repo) }

    @Test fun `initial state is idle`() = runTest {
        vm.uiState.test {
            val s = awaitItem()
            assertFalse(s.loading)
            assertNull(s.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `signIn sets loading then success`() = runTest {
        coEvery { repo.signIn(any(), any()) } just Runs
        vm.uiState.test {
            vm.signIn("a@b.com", "pass")
            val loading = awaitItem()
            assertTrue(loading.loading)
            val done = awaitItem()
            assertFalse(done.loading)
            assertTrue(done.success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `signIn error populates error field`() = runTest {
        coEvery { repo.signIn(any(), any()) } throws RuntimeException("Invalid credentials")
        vm.uiState.test {
            vm.signIn("a@b.com", "wrong")
            awaitItem() // loading
            val err = awaitItem()
            assertFalse(err.loading)
            assertEquals("Invalid credentials", err.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 8: Run test — expect FAIL**
```bash
./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest" 2>&1 | tail -10
```

- [ ] **Step 9: Create AuthViewModel.kt**

Create `app/src/main/java/com/souspantry/app/ui/auth/AuthViewModel.kt`:
```kotlin
package com.souspantry.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading : Boolean = false,
    val success : Boolean = false,
    val error   : String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null, success = false) }
        runCatching { repo.signIn(email, password) }
            .onSuccess { _uiState.update { it.copy(loading = false, success = true) } }
            .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, error = null, success = false) }
        runCatching { repo.signUp(email, password) }
            .onSuccess { _uiState.update { it.copy(loading = false, success = true) } }
            .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
```

- [ ] **Step 10: Run test — expect PASS**
```bash
./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest" 2>&1 | tail -10
```

- [ ] **Step 11: Create LoginScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/auth/LoginScreen.kt`:
```kotlin
package com.souspantry.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess : () -> Unit,
    onGoToSignUp   : () -> Unit,
    vm             : AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.success) {
        if (state.success) onLoginSuccess()
    }

    Scaffold(containerColor = Cream) { padding ->
        Column(
            modifier            = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign in", style = MaterialTheme.typography.headlineMedium, color = Navy)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("Email") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value           = password,
                onValueChange   = { password = it },
                label           = { Text("Password") },
                singleLine      = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier        = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick  = { vm.signIn(email, password) },
                enabled  = email.isNotBlank() && password.isNotBlank() && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Cream, strokeWidth = 2.dp)
                else Text("Sign in")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onGoToSignUp) {
                Text("Don't have an account? Sign up", color = Green)
            }
        }
    }
}
```

- [ ] **Step 12: Create SignUpScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/auth/SignUpScreen.kt`:
```kotlin
package com.souspantry.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess : () -> Unit,
    onGoToLogin     : () -> Unit,
    vm              : AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }

    LaunchedEffect(state.success) {
        if (state.success) onSignUpSuccess()
    }

    Scaffold(containerColor = Cream) { padding ->
        Column(
            modifier            = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create account", style = MaterialTheme.typography.headlineMedium, color = Navy)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirm password") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = confirm.isNotEmpty() && confirm != password)

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick  = { vm.signUp(email, password) },
                enabled  = email.isNotBlank() && password.isNotBlank() && password == confirm && !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Cream, strokeWidth = 2.dp)
                else Text("Create account")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onGoToLogin) {
                Text("Already have an account? Sign in", color = Green)
            }
        }
    }
}
```

- [ ] **Step 13: Add auth routes to NavHost.kt**

Add auth route constants to the `Screen` sealed class:
```kotlin
    data object Login   : Screen("login",   "Login",   Icons.Filled.Home)
    data object SignUp  : Screen("signup",  "Sign Up", Icons.Filled.Home)
```

At the top of `SousPantryNavHost()`, check if user is logged in and set start destination:
```kotlin
    // Auth guard: start at login if no session
    // (inject AuthRepository or check prefs — for now use a simple remembered flag)
```

Add composable routes inside `NavHost`:
```kotlin
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo("login") { inclusive = true } } },
                    onGoToSignUp   = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignUpScreen(
                    onSignUpSuccess = { navController.navigate(Screen.Home.route) { popUpTo("signup") { inclusive = true } } },
                    onGoToLogin     = { navController.popBackStack() }
                )
            }
```

Add a Sign Out button to SettingsScreen (add `authRepo: AuthRepository` param, show sign-out button that calls `authRepo.signOut()` then navigates to login). This wiring is done in the next task.

- [ ] **Step 14: Run on device — test auth flow**

Build and run. Navigate to Settings, find sign-out option. Test sign-up with a test email, confirm it reaches Supabase dashboard → Authentication → Users.

- [ ] **Step 15: Commit**
```bash
git add -A && git commit -m "feat: add Supabase auth (login/signup/signout)"
```

---

### Task 8: Google Play Billing + Paywall

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/souspantry/app/services/BillingModule.kt`
- Create: `app/src/main/java/com/souspantry/app/data/billing/BillingRepository.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/paywall/PaywallViewModel.kt`
- Create: `app/src/main/java/com/souspantry/app/ui/paywall/PaywallScreen.kt`
- Create: `app/src/test/java/com/souspantry/app/ui/paywall/PaywallViewModelTest.kt`

> **Prerequisite for testing purchases:** GPB test purchases only work on a signed APK/AAB published to the Play Console internal testing track, with a test account added as a tester. Set up the Play Console listing during this task. You can test the billing UI (product display, paywall screen) without this, but actual purchase flow requires it.

- [ ] **Step 1: Add billing dep to libs.versions.toml**

Add to `[versions]`:
```toml
billing             = "7.0.0"
```

Add to `[libraries]`:
```toml
billing-ktx         = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }
```

- [ ] **Step 2: Add billing to app/build.gradle.kts**
```kotlin
    // Google Play Billing
    implementation(libs.billing.ktx)
```

- [ ] **Step 3: Create BillingModule.kt**

Create `app/src/main/java/com/souspantry/app/services/BillingModule.kt`:
```kotlin
package com.souspantry.app.services

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides @Singleton
    fun provideBillingClient(
        @ApplicationContext ctx: Context,
        listener: PurchasesUpdatedListener,
    ): BillingClient =
        BillingClient.newBuilder(ctx)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
}
```

- [ ] **Step 4: Create BillingRepository.kt**

Create `app/src/main/java/com/souspantry/app/data/billing/BillingRepository.kt`:
```kotlin
package com.souspantry.app.data.billing

import android.app.Activity
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

const val SUBSCRIPTION_ID = "sous_pantry_premium_monthly"

@Singleton
class BillingRepository @Inject constructor(
    private val billingClient: BillingClient,
) : PurchasesUpdatedListener {

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchases ->
            _isPremium.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    suspend fun getSubscriptionDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUBSCRIPTION_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )).build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { _, details ->
                cont.resume(details.firstOrNull())
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR).build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )).build()
        return billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { acknowledgePurchase(it) }
            _isPremium.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) {}
    }
}
```

- [ ] **Step 5: Write PaywallViewModel test**

Create `app/src/test/java/com/souspantry/app/ui/paywall/PaywallViewModelTest.kt`:
```kotlin
package com.souspantry.app.ui.paywall

import app.cash.turbine.test
import com.souspantry.app.data.billing.BillingRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val repo = mockk<BillingRepository>(relaxed = true)
    private lateinit var vm: PaywallViewModel

    @Before fun setUp() {
        every { repo.isPremium } returns MutableStateFlow(false)
        vm = PaywallViewModel(repo)
    }

    @Test fun `isPremium reflects repository state`() = runTest {
        vm.isPremium.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 6: Run test — expect FAIL**
```bash
./gradlew :app:testDebugUnitTest --tests "*.PaywallViewModelTest" 2>&1 | tail -10
```

- [ ] **Step 7: Create PaywallViewModel.kt**

Create `app/src/main/java/com/souspantry/app/ui/paywall/PaywallViewModel.kt`:
```kotlin
package com.souspantry.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.souspantry.app.data.billing.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallState(
    val loading        : Boolean        = false,
    val productDetails : ProductDetails? = null,
    val error          : String?        = null,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billing: BillingRepository,
) : ViewModel() {

    val isPremium = billing.isPremium

    private val _state = MutableStateFlow(PaywallState())
    val state = _state.asStateFlow()

    init { loadProduct() }

    private fun loadProduct() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { billing.getSubscriptionDetails() }
            .onSuccess { d -> _state.update { it.copy(loading = false, productDetails = d) } }
            .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
    }

    fun subscribe(activity: Activity) {
        val details = _state.value.productDetails ?: return
        billing.launchBillingFlow(activity, details)
    }
}
```

- [ ] **Step 8: Run test — expect PASS**
```bash
./gradlew :app:testDebugUnitTest --tests "*.PaywallViewModelTest" 2>&1 | tail -10
```

- [ ] **Step 9: Create PaywallScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/paywall/PaywallScreen.kt`:
```kotlin
package com.souspantry.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onDismiss : () -> Unit,
    vm        : PaywallViewModel = hiltViewModel()
) {
    val state     by vm.state.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val context   = LocalContext.current

    LaunchedEffect(isPremium) {
        if (isPremium) onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Go Premium", color = Navy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        },
        containerColor = Cream
    ) { padding ->
        Column(
            modifier            = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sous Pantry Premium", style = MaterialTheme.typography.headlineSmall, color = Navy,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            listOf(
                "Unlimited AI meal suggestions",
                "Receipt & barcode scanning",
                "Smart shopping lists",
                "Plan & Cook meal planner"
            ).forEach { feature ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("✓  $feature", style = MaterialTheme.typography.bodyMedium, color = Navy)
                }
            }

            Spacer(Modifier.height(32.dp))

            when {
                state.loading -> CircularProgressIndicator(color = Green)
                state.productDetails != null -> {
                    val price = state.productDetails!!
                        .subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        ?.formattedPrice ?: "—"
                    Text(price + " / month", style = MaterialTheme.typography.titleLarge, color = Navy)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick  = { vm.subscribe(context as Activity) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Subscribe") }
                }
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                else -> Text("Subscription unavailable", color = Slate)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) { Text("Maybe later", color = Slate) }
        }
    }
}
```

- [ ] **Step 10: Add paywall route to NavHost.kt and trigger from Settings**

Add import: `import com.souspantry.app.ui.paywall.PaywallScreen`

Add composable:
```kotlin
            composable("paywall") {
                PaywallScreen(onDismiss = { navController.popBackStack() })
            }
```

In `SettingsScreen`, add a "Go Premium" button that navigates to `"paywall"` (pass `navController` down or use a callback).

- [ ] **Step 11: Run on device — verify paywall screen appears**

Navigate to Settings → tap "Go Premium" → PaywallScreen should appear with feature list. (Actual purchase only works on signed Play Store build.)

- [ ] **Step 12: Commit**
```bash
git add -A && git commit -m "feat: add Google Play Billing paywall screen"
```

---

## Stage 3 — Day 9

---

### Task 9: Onboarding

**Files:**
- Create: `app/src/main/java/com/souspantry/app/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/souspantry/app/navigation/NavHost.kt`
- Modify: `app/src/main/java/com/souspantry/app/MainActivity.kt`

- [ ] **Step 1: Create OnboardingScreen.kt**

Create `app/src/main/java/com/souspantry/app/ui/onboarding/OnboardingScreen.kt`:
```kotlin
package com.souspantry.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.souspantry.app.ui.theme.*

private data class OnboardingPage(val title: String, val body: String, val emoji: String)

private val pages = listOf(
    OnboardingPage("Welcome to Sous Pantry",   "Your AI-powered kitchen assistant.",                     "🧑‍🍳"),
    OnboardingPage("Track your pantry",         "Scan barcodes or receipts to add items instantly.",     "📦"),
    OnboardingPage("Cook smarter",              "Get meal suggestions based on what you already have.",   "🍽️"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Scaffold(containerColor = Cream) { padding ->
        Column(
            modifier            = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier            = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(page.emoji, style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(32.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineSmall, color = Navy,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(page.body, style = MaterialTheme.typography.bodyLarge, color = Slate,
                        textAlign = TextAlign.Center)
                }
            }

            Row(
                modifier            = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { i ->
                    Surface(
                        shape = CircleShape,
                        color = if (i == pagerState.currentPage) Green else SoftMint,
                        modifier = Modifier.size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                    ) {}
                }
            }

            Button(
                onClick  = onFinish,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 32.dp).height(52.dp)
            ) {
                Text(if (isLastPage) "Get started" else "Next")
            }
        }
    }
}
```

- [ ] **Step 2: Add onboarding gate to MainActivity.kt**

Replace `MainActivity.kt`:
```kotlin
package com.souspantry.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.souspantry.app.data.local.AppPreferences
import com.souspantry.app.navigation.SousPantryNavHost
import com.souspantry.app.ui.theme.SousPantryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val onboardingDone = runBlocking { prefs.onboardingDone.first() }

        setContent {
            SousPantryTheme {
                SousPantryNavHost(startOnboarding = !onboardingDone)
            }
        }
    }
}
```

- [ ] **Step 3: Update SousPantryNavHost to accept startOnboarding param**

Modify `NavHost.kt` — change function signature:
```kotlin
@Composable
fun SousPantryNavHost(startOnboarding: Boolean = false) {
```

Change `startDestination`:
```kotlin
        startDestination = if (startOnboarding) "onboarding" else Screen.Home.route,
```

Add import: `import com.souspantry.app.ui.onboarding.OnboardingScreen`

Add composable (inject prefs via hiltViewModel or pass a lambda):
```kotlin
            composable("onboarding") {
                val prefs: AppPreferences = androidx.hilt.navigation.compose.hiltViewModel<androidx.lifecycle.ViewModel>()
                    .let { /* workaround: use coroutine scope */ }
                // Simpler: use a dedicated OnboardingViewModel
                OnboardingScreen(onFinish = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
```

> **Note:** To persist `onboarding_done = true` when the user taps "Get started", create a minimal `OnboardingViewModel`:
```kotlin
// ui/onboarding/OnboardingViewModel.kt
package com.souspantry.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {
    fun markDone() = viewModelScope.launch { prefs.setOnboardingDone(true) }
}
```

Update `OnboardingScreen` signature to accept `vm: OnboardingViewModel = hiltViewModel()` and call `vm.markDone()` in `onFinish`.

Update the NavHost composable for onboarding to inject the ViewModel:
```kotlin
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
```

- [ ] **Step 4: Run on device — verify onboarding shows on first launch only**

Clear app data (Settings → Apps → Sous Pantry → Clear data) then launch app → onboarding should appear. Complete it → main app. Kill and relaunch → onboarding should NOT appear.

- [ ] **Step 5: Commit**
```bash
git add -A && git commit -m "feat: add 3-screen onboarding with DataStore persistence"
```

---

## Day 10 — Polish + Internal Test Build

### Task 10: Final polish and signed APK

**Files:**
- Modify: `app/build.gradle.kts` (versionCode/versionName)
- Modify: `app/src/main/AndroidManifest.xml` (add any missing permissions)

- [ ] **Step 1: Bump version**

In `app/build.gradle.kts`:
```kotlin
        versionCode   = 2
        versionName   = "1.0.0"
```

- [ ] **Step 2: Run full test suite**
```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```
Expected: all tests pass.

- [ ] **Step 3: Build release APK**

In Android Studio: Build → Generate Signed Bundle/APK → APK → create or use existing keystore → release build variant → Finish.

Or via CLI (requires keystore setup in `local.properties`):
```bash
./gradlew :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

- [ ] **Step 4: Install on device and smoke test**
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Smoke test checklist:
- [ ] Onboarding shows on first launch
- [ ] Home loads 4 recipe sections
- [ ] Pantry CRUD works (add, edit, delete item)
- [ ] Barcode scan opens camera and adds item
- [ ] Receipt scan opens camera and adds multiple items
- [ ] Shopping list generates from pantry
- [ ] Plan & Cook generates and cards expand/collapse
- [ ] Settings saves name
- [ ] FCM token appears in Settings (debug)
- [ ] Login / Sign up flow completes

- [ ] **Step 5: Final commit**
```bash
git add -A && git commit -m "release: v1.0.0 internal test build"
```

---

## Self-Review Against Spec

| Spec Requirement | Task |
|---|---|
| Pantry CRUD + barcode + receipt scan | Task 5 |
| Home (4 recipe sections) + retry | Task 3 |
| Shopping list + empty states | Task 4 |
| Plan & Cook + expand/collapse | Task 4 |
| Settings/Profile screen | Task 2 |
| FCM push notifications | Task 6 |
| Supabase auth (login/signup) | Task 7 |
| Google Play Billing paywall | Task 8 |
| Onboarding (3 screens, first-launch only) | Task 9 |
| DataStore for device prefs + session | Task 1 |
| No dark mode | ✓ (not implemented) |
| No geofence | ✓ (not implemented) |
| Backend API contract (no auth for core routes) | Existing scaffold + AuthInterceptor in Task 7 |
