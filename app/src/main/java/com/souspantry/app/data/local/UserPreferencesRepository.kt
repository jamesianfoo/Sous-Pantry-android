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
        // Identity
        val KEY_DEVICE_ID        = stringPreferencesKey("device_id")
        val KEY_USER_NAME        = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL       = stringPreferencesKey("user_email")
        val KEY_ONBOARDING_DONE  = booleanPreferencesKey("onboarding_done")
        val KEY_FOUNDER_NOTE_SEEN = booleanPreferencesKey("founder_note_seen")

        // Notifications (global + per-category)
        val KEY_NOTIF_ENABLED       = booleanPreferencesKey("notif_enabled")
        val KEY_ALERT_EXPIRING      = booleanPreferencesKey("alert_expiring")
        val KEY_ALERT_RECEIPT       = booleanPreferencesKey("alert_receipt")
        val KEY_ALERT_PANTRY_STALE  = booleanPreferencesKey("alert_pantry_stale")

        // Account / profile
        val KEY_GENDER              = stringPreferencesKey("gender")
        val KEY_COOKING_FOR         = intPreferencesKey("cooking_for")

        // Measurement
        val KEY_MEASUREMENT_SYSTEM  = stringPreferencesKey("measurement_system") // "metric" | "imperial"
        val KEY_TEMPERATURE_UNIT    = stringPreferencesKey("temperature_unit")   // "celsius" | "fahrenheit"

        // Dietary
        val KEY_DIETARY_TYPES       = stringSetPreferencesKey("dietary_types")
        val KEY_FOOD_RESTRICTIONS   = stringSetPreferencesKey("food_restrictions")
        val KEY_AVOID_INGREDIENTS   = stringSetPreferencesKey("avoid_ingredients")

        // Customisation
        val KEY_RECOMMENDED_SUBS    = booleanPreferencesKey("recommended_subs")
        val KEY_SOUS_AI_ENABLED     = booleanPreferencesKey("sous_ai_enabled")

        // Auth / session
        val KEY_SIGNED_IN        = booleanPreferencesKey("signed_in")
        val KEY_SUPABASE_TOKEN   = stringPreferencesKey("supabase_token")
        val KEY_SUPABASE_USER_ID = stringPreferencesKey("supabase_user_id")

        // Premium entitlement (set by the paywall; real billing replaces this later).
        val KEY_PREMIUM_ACTIVE   = booleanPreferencesKey("premium_active")
        // Debug override — only honoured in debug builds (gated at read-site).
        val KEY_FORCE_PREMIUM    = booleanPreferencesKey("debug_force_premium")

        // eReceipt custom stores — each entry is "name|url".
        val KEY_CUSTOM_STORES    = stringSetPreferencesKey("ereceipt_custom_stores")
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    val deviceId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: UUID.randomUUID().toString().also { id ->
            context.dataStore.edit { it[KEY_DEVICE_ID] = id }
        }
    }

    val userName: Flow<String>         = context.dataStore.data.map { it[KEY_USER_NAME]        ?: "" }
    val userEmail: Flow<String>        = context.dataStore.data.map { it[KEY_USER_EMAIL]       ?: "" }
    val onboardingDone: Flow<Boolean>  = context.dataStore.data.map { it[KEY_ONBOARDING_DONE]  ?: false }
    val founderNoteSeen: Flow<Boolean> = context.dataStore.data.map { it[KEY_FOUNDER_NOTE_SEEN] ?: false }

    val notifEnabled: Flow<Boolean>      = context.dataStore.data.map { it[KEY_NOTIF_ENABLED]      ?: true }
    val alertExpiring: Flow<Boolean>     = context.dataStore.data.map { it[KEY_ALERT_EXPIRING]     ?: true }
    val alertReceipt: Flow<Boolean>      = context.dataStore.data.map { it[KEY_ALERT_RECEIPT]      ?: true }
    val alertPantryStale: Flow<Boolean>  = context.dataStore.data.map { it[KEY_ALERT_PANTRY_STALE] ?: true }

    val gender: Flow<String>           = context.dataStore.data.map { it[KEY_GENDER]           ?: "" }
    val cookingFor: Flow<Int>          = context.dataStore.data.map { it[KEY_COOKING_FOR]      ?: 2 }

    val measurementSystem: Flow<String> = context.dataStore.data.map { it[KEY_MEASUREMENT_SYSTEM] ?: "metric" }
    val temperatureUnit: Flow<String>   = context.dataStore.data.map { it[KEY_TEMPERATURE_UNIT]   ?: "celsius" }

    val dietaryTypes: Flow<Set<String>>     = context.dataStore.data.map { it[KEY_DIETARY_TYPES]     ?: emptySet() }
    val foodRestrictions: Flow<Set<String>> = context.dataStore.data.map { it[KEY_FOOD_RESTRICTIONS] ?: emptySet() }
    val avoidIngredients: Flow<Set<String>> = context.dataStore.data.map { it[KEY_AVOID_INGREDIENTS] ?: emptySet() }

    val recommendedSubs: Flow<Boolean> = context.dataStore.data.map { it[KEY_RECOMMENDED_SUBS] ?: true }
    val sousAIEnabled: Flow<Boolean>   = context.dataStore.data.map { it[KEY_SOUS_AI_ENABLED]  ?: true }

    val signedIn: Flow<Boolean>        = context.dataStore.data.map { it[KEY_SIGNED_IN] ?: false }
    val supabaseToken: Flow<String?>   = context.dataStore.data.map { it[KEY_SUPABASE_TOKEN] }
    val supabaseUserId: Flow<String?>  = context.dataStore.data.map { it[KEY_SUPABASE_USER_ID] }

    val premiumActive: Flow<Boolean>   = context.dataStore.data.map { it[KEY_PREMIUM_ACTIVE] ?: false }
    val forcePremium: Flow<Boolean>    = context.dataStore.data.map { it[KEY_FORCE_PREMIUM] ?: false }

    val customStores: Flow<Set<String>> = context.dataStore.data.map { it[KEY_CUSTOM_STORES] ?: emptySet() }

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun setUserName(name: String)             = context.dataStore.edit { it[KEY_USER_NAME]  = name }
    suspend fun setUserEmail(email: String)           = context.dataStore.edit { it[KEY_USER_EMAIL] = email }
    suspend fun setOnboardingDone()                   = context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    suspend fun setFounderNoteSeen()                  = context.dataStore.edit { it[KEY_FOUNDER_NOTE_SEEN] = true }

    suspend fun setNotifEnabled(on: Boolean)          = context.dataStore.edit { it[KEY_NOTIF_ENABLED]      = on }
    suspend fun setAlertExpiring(on: Boolean)         = context.dataStore.edit { it[KEY_ALERT_EXPIRING]     = on }
    suspend fun setAlertReceipt(on: Boolean)          = context.dataStore.edit { it[KEY_ALERT_RECEIPT]      = on }
    suspend fun setAlertPantryStale(on: Boolean)      = context.dataStore.edit { it[KEY_ALERT_PANTRY_STALE] = on }

    suspend fun setGender(value: String)              = context.dataStore.edit { it[KEY_GENDER]      = value }
    suspend fun setCookingFor(count: Int)             = context.dataStore.edit { it[KEY_COOKING_FOR] = count.coerceIn(1, 12) }

    suspend fun setMeasurementSystem(value: String)   = context.dataStore.edit { it[KEY_MEASUREMENT_SYSTEM] = value }
    suspend fun setTemperatureUnit(value: String)     = context.dataStore.edit { it[KEY_TEMPERATURE_UNIT]   = value }

    suspend fun setDietaryTypes(values: Set<String>)     = context.dataStore.edit { it[KEY_DIETARY_TYPES]     = values }
    suspend fun setFoodRestrictions(values: Set<String>) = context.dataStore.edit { it[KEY_FOOD_RESTRICTIONS] = values }
    suspend fun setAvoidIngredients(values: Set<String>) = context.dataStore.edit { it[KEY_AVOID_INGREDIENTS] = values }

    suspend fun setRecommendedSubs(on: Boolean)       = context.dataStore.edit { it[KEY_RECOMMENDED_SUBS] = on }
    suspend fun setSousAIEnabled(on: Boolean)         = context.dataStore.edit { it[KEY_SOUS_AI_ENABLED]  = on }

    suspend fun setPremiumActive(on: Boolean)         = context.dataStore.edit { it[KEY_PREMIUM_ACTIVE] = on }
    suspend fun setForcePremium(on: Boolean)          = context.dataStore.edit { it[KEY_FORCE_PREMIUM] = on }

    suspend fun addCustomStore(name: String, url: String) = context.dataStore.edit { prefs ->
        val current = prefs[KEY_CUSTOM_STORES] ?: emptySet()
        prefs[KEY_CUSTOM_STORES] = current + "${name.trim()}|${url.trim()}"
    }
    suspend fun removeCustomStore(entry: String) = context.dataStore.edit { prefs ->
        prefs[KEY_CUSTOM_STORES] = (prefs[KEY_CUSTOM_STORES] ?: emptySet()) - entry
    }

    suspend fun setSignedIn(on: Boolean)              = context.dataStore.edit { it[KEY_SIGNED_IN] = on }

    suspend fun setSupabaseSession(token: String, userId: String) = context.dataStore.edit {
        it[KEY_SUPABASE_TOKEN]   = token
        it[KEY_SUPABASE_USER_ID] = userId
    }
    suspend fun clearSupabaseSession() = context.dataStore.edit {
        it.remove(KEY_SUPABASE_TOKEN)
        it.remove(KEY_SUPABASE_USER_ID)
    }

    /** Wipes every user-set preference. Used by Delete Account / Log Out flows. */
    suspend fun clearAll() = context.dataStore.edit { prefs ->
        // Keep deviceId stable across resets so FCM / analytics still work.
        val keepDeviceId = prefs[KEY_DEVICE_ID]
        prefs.clear()
        if (keepDeviceId != null) prefs[KEY_DEVICE_ID] = keepDeviceId
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideUserPrefs(@ApplicationContext ctx: Context): UserPreferencesRepository =
        UserPreferencesRepository(ctx)
}
