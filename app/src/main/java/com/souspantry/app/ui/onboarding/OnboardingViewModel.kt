package com.souspantry.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
        val funnelDone      : Boolean,
        val founderNoteSeen : Boolean,
    ) : AppGate
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    init {
        // ⚠️ Grandfather rule (mirrors the iOS emergency fix): an existing user
        // whose legacy onboarding flag is true but who predates the funnel must
        // NEVER be re-gated. Persist sp_hasCompletedFunnel=true for them once.
        viewModelScope.launch {
            if (!prefs.funnelDoneWritten.first() && prefs.onboardingDone.first()) {
                prefs.setFunnelDone()
            }
        }
    }

    val gate: StateFlow<AppGate> = combine(
        prefs.signedIn, prefs.onboardingDone, prefs.funnelDone, prefs.funnelDoneWritten, prefs.founderNoteSeen,
    ) { signedIn, done, funnelDone, funnelWritten, founderSeen ->
        // Grandfather applied inline too, so the gate is race-free even before
        // the init write above lands.
        val effectiveFunnelDone = funnelDone || (!funnelWritten && done)
        AppGate.Ready(signedIn = signedIn, onboardingDone = done, funnelDone = effectiveFunnelDone, founderNoteSeen = founderSeen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppGate.Loading)

    fun complete() = viewModelScope.launch { prefs.setOnboardingDone() }
}
