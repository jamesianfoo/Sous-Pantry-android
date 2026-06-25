package com.souspantry.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the app's entry gate. Loading is a distinct state so the NavHost is
 * only composed once the persisted flags are actually known — its
 * startDestination is captured once, so it must not be computed from
 * still-loading defaults (that would let first-timers skip the wizard or flash
 * the wrong screen). Returning users who completed setup have a persisted
 * onboardingDone=true and therefore never see the wizard again.
 */
sealed interface AppGate {
    data object Loading : AppGate
    data class Ready(
        val signedIn        : Boolean,
        val onboardingDone  : Boolean,
        val founderNoteSeen : Boolean,
    ) : AppGate
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val gate: StateFlow<AppGate> = combine(
        prefs.signedIn, prefs.onboardingDone, prefs.founderNoteSeen,
    ) { signedIn, done, founderSeen ->
        AppGate.Ready(signedIn = signedIn, onboardingDone = done, founderNoteSeen = founderSeen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppGate.Loading)

    fun complete() = viewModelScope.launch { prefs.setOnboardingDone() }
}
