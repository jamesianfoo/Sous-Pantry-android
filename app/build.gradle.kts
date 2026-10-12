import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    // alias(libs.plugins.google.services)  // re-enable after google-services.json is added
}

// Read local.properties for overrideable config
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace         = "com.souspantry.app"
    compileSdk        = 36

    defaultConfig {
        applicationId = "com.souspantry.app"
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 2
        versionName   = "1.0"

        val baseUrl = localProps.getProperty("BASE_URL") ?: "http://10.0.2.2:3000"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        // App secret for the shared Cloudflare Worker AI proxy. Provider keys
        // (Anthropic/ILMU) live in the Worker and never ship in the APK; this
        // secret is app-scoped, rate-limited and rotatable at the Worker.
        val proxySecret = localProps.getProperty("SP_PROXY_SECRET") ?: ""
        buildConfigField("String", "SP_PROXY_SECRET", "\"$proxySecret\"")

        // Unsplash key for the cuisine-generic image fallback (same key as iOS
        // APIConfig.unsplashAccessKey). Blank = no fallback photo; cards keep
        // their cuisine gradient until the accurate Worker image exists.
        val unsplashKey = localProps.getProperty("UNSPLASH_ACCESS_KEY") ?: ""
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"$unsplashKey\"")

        // Google Cloud OAuth *Web* client ID for Google Sign-In (Credential Manager).
        // The app's signing SHA-1 must also be registered as an Android client.
        val googleClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")

        // RevenueCat public SDK key for the Google Play app (starts "goog_"). It is
        // a public key by design; the secret key never ships. Blank = billing off.
        val rcKey = localProps.getProperty("REVENUECAT_ANDROID_KEY") ?: ""
        buildConfigField("String", "REVENUECAT_ANDROID_KEY", "\"$rcKey\"")

        // Supabase project URL + anon (public) key, same project as iOS APIConfig.
        // Blank = Supabase off: Google sign-in still works, no server-side bridge.
        val supabaseUrl = localProps.getProperty("SUPABASE_URL") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    // Upload-key signing from local.properties (git-ignored), so `bundleRelease`
    // produces a Play-ready AAB. Keys: RELEASE_STORE_FILE (absolute path),
    // RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD.
    // Absent = unsigned bundle, sign it manually with jarsigner.
    val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
    if (!storeFilePath.isNullOrBlank()) {
        signingConfigs {
            create("upload") {
                storeFile     = file(storeFilePath)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias      = localProps.getProperty("RELEASE_KEY_ALIAS") ?: "souspantry"
                keyPassword   = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (!storeFilePath.isNullOrBlank()) signingConfig = signingConfigs.getByName("upload")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"https://api.souspantry.com\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose      = true
        buildConfig  = true
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)

    // Hilt DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)
    ksp(libs.hilt.compiler)

    // Room (local database)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp (API calls)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coil (image loading)
    implementation(libs.coil.compose)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)

    // RevenueCat (Google Play Billing) — same dashboard and entitlement as iOS
    implementation(libs.revenuecat)

    // Supabase (Google session bridge + delete_my_account RPC, as on iOS)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.okhttp)

    // ML Kit (barcode + text recognition)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.mlkit)

    // WorkManager (trial-ending reminder)
    implementation(libs.workmanager)

    // DataStore
    implementation(libs.datastore.prefs)

    // Coroutines
    implementation(libs.coroutines.android)

    // Serialization
    implementation(libs.serialization.json)

    // Accompanist (runtime permissions)
    implementation(libs.accompanist.permissions)

    // Firebase (FCM push notifications) — re-enable after google-services.json is added
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.messaging)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.12")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
