package com.souspantry.app.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.souspantry.app.BuildConfig
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val loadingGoogle : Boolean = false,
    val error         : String? = null,
)

/**
 * Google Sign-In only, mirroring iOS (Apple + Google there; Google here).
 *
 * The provider owns the credentials: on success we keep the name and email in
 * device preferences and nothing else. Sous Pantry has no user database and no
 * email system, so there is deliberately no email/password sign-up.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun clearError() = _state.update { it.copy(error = null) }

    /**
     * Runs the real Google account picker. The user is only signed in when Google
     * returns an authenticated account — cancelling or failing leaves them here.
     */
    fun continueWithGoogle(context: Context, onSignedIn: () -> Unit) = viewModelScope.launch {
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (serverClientId.isBlank()) {
            _state.update { it.copy(error = "Google sign-in isn't configured in this build.") }
            return@launch
        }

        _state.update { it.copy(loadingGoogle = true, error = null) }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        val outcome = runCatching { CredentialManager.create(context).getCredential(context, request) }
        _state.update { it.copy(loadingGoogle = false) }

        outcome
            .onSuccess { response ->
                val credential = response.credential
                val isGoogle = credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                if (!isGoogle) {
                    _state.update { it.copy(error = "Couldn't read that Google account. Try again.") }
                    return@onSuccess
                }
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                // `id` is the account's email address.
                prefs.setUserEmail(google.id)
                prefs.setUserName(google.displayName?.takeIf { it.isNotBlank() } ?: google.id.substringBefore("@"))
                prefs.setSignedIn(true)
                // TODO(billing): identify this email with the subscription layer once
                // Play Billing is wired up, so entitlements follow the account (iOS
                // does this with RevenueCat).
                onSignedIn()
            }
            .onFailure { e ->
                Log.e("Auth", "Google sign-in failed", e)
                _state.update {
                    it.copy(
                        error = when (e) {
                            is GetCredentialCancellationException -> null   // user backed out; no scolding
                            is NoCredentialException -> "No Google account found on this device. Add one in Settings, then try again."
                            else -> "Couldn't sign in with Google. Check your connection and try again."
                        }
                    )
                }
            }
    }
}
