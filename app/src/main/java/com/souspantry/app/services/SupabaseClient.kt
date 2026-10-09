package com.souspantry.app.services

import android.util.Log
import com.souspantry.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase bridge, mirroring iOS `SupabaseService.swift`: the Google sign-in is
 * exchanged for a Supabase session so RLS policies and `auth.uid()` RPCs work,
 * and Delete Account erases the server side via `delete_my_account`.
 *
 * Unconfigured (blank URL or key) = every call is a logged no-op, so Google
 * sign-in keeps working without Supabase. Failures are logged under
 * [TAG] and rethrown; callers decide whether they are fatal.
 */
@Singleton
class SupabaseClient @Inject constructor() {

    val enabled: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    // Lazy: building the client parses the URL, which would throw in an unconfigured build.
    private val client by lazy {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }

    /**
     * Exchanges a Google ID token for a Supabase session (provider Google). Same
     * call iOS makes; like iOS no nonce is sent, because Credential Manager issues
     * the token without one. iOS also passes Google's access token for the
     * `at_hash` check; Credential Manager does not expose one, and Supabase only
     * requires it when the token carries `at_hash`.
     */
    suspend fun signInWithGoogle(idToken: String) {
        if (!enabled) { Log.w(TAG, "Not configured; skipping Google -> Supabase bridge"); return }
        try {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider     = Google
            }
            Log.i(TAG, "Google -> Supabase session established")
        } catch (e: Exception) {
            Log.e(TAG, "Google bridge failed", e)
            throw e
        }
    }

    /**
     * Throws when there is no Supabase session. The SDK refreshes an expired
     * token itself on the next request, so only the presence of a session matters.
     */
    suspend fun ensureSession() {
        check(enabled) { "Supabase isn't configured in this build." }
        client.auth.awaitInitialization()
        checkNotNull(client.auth.currentSessionOrNull()) { "No Supabase session. Sign in again." }
    }

    /**
     * Erases ALL of the user's Supabase data and their auth user via the
     * `delete_my_account` RPC, then drops the local session. Must run while
     * still authenticated. On failure the session is kept so a retry can succeed.
     */
    suspend fun deleteMyAccount() {
        if (!enabled) { Log.w(TAG, "Not configured; skipping server-side account deletion"); return }
        try {
            ensureSession()
            client.postgrest.rpc("delete_my_account")
            Log.i(TAG, "account data + auth user deleted")
        } catch (e: Exception) {
            Log.e(TAG, "account deletion failed", e)
            throw e
        }
        signOut()
    }

    /** Drops the Supabase session; the local copy is cleared even if the server call fails. */
    suspend fun signOut() {
        if (!enabled) return
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "signOut failed; clearing local session anyway", e)
            client.auth.clearSession()
        }
    }

    companion object {
        private const val TAG = "SousSupabase"
    }
}
