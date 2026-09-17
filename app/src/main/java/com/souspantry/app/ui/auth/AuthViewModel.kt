package com.souspantry.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val loadingGoogle : Boolean = false,
    val loadingEmail  : Boolean = false,
    val emailMode     : Boolean = false,   // false = social buttons, true = email form
    val error         : String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    fun toggleEmailMode() = _state.update { it.copy(emailMode = !it.emailMode, error = null) }
    fun clearError()      = _state.update { it.copy(error = null) }

    /**
     * Continue with Google.
     *
     * TODO(auth): swap this local sign-in for a real Google credential flow.
     *   1. Android Credential Manager → Google ID token (needs a Google Cloud
     *      OAuth *Web* client ID + the app's SHA-1 registered as an Android client).
     *   2. supabase.auth.signInWith(IDToken) { idToken = … } using the Supabase
     *      *anon* key (clients use anon, not the service-role key in backend/.env).
     * For now this captures the user locally so the sign-in gate works end-to-end.
     */
    fun continueWithGoogle(onSignedIn: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loadingGoogle = true, error = null) }
        delay(450) // brief feedback; real flow will replace this
        prefs.setUserName("Chef")
        prefs.setSignedIn(true)
        _state.update { it.copy(loadingGoogle = false) }
        onSignedIn()
    }

    /**
     * Continue with email + password.
     *
     * TODO(auth): swap for supabase.auth.signInWith(Email)/signUpWith(Email)
     *   once the Supabase anon key is wired into BuildConfig.
     */
    fun continueWithEmail(email: String, password: String, onSignedIn: () -> Unit) = viewModelScope.launch {
        val trimmed = email.trim()
        if (!trimmed.contains("@") || password.length < 6) {
            _state.update { it.copy(error = "Enter a valid email and a password of at least 6 characters.") }
            return@launch
        }
        _state.update { it.copy(loadingEmail = true, error = null) }
        delay(450)
        prefs.setUserEmail(trimmed)
        // Derive a friendly display name from the email's local part if none set.
        prefs.setUserName(trimmed.substringBefore("@").replaceFirstChar { it.uppercase() })
        prefs.setSignedIn(true)
        _state.update { it.copy(loadingEmail = false) }
        onSignedIn()
    }
}
