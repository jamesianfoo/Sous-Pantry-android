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
