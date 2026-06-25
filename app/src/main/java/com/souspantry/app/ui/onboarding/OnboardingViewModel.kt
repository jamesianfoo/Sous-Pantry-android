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

    /** true = past the sign-in gate. Defaults to true while loading so we don't flash the auth screen. */
    val signedIn = prefs.signedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    fun complete() = viewModelScope.launch { prefs.setOnboardingDone() }
}
