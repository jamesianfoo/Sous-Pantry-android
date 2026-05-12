package com.souspantry.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val userName     : String  = "",
    val notifEnabled : Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        prefs.userName,
        prefs.notifEnabled,
    ) { name, notif -> SettingsState(userName = name, notifEnabled = notif) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setUserName(name: String)       = viewModelScope.launch { prefs.setUserName(name) }
    fun setNotifEnabled(on: Boolean)    = viewModelScope.launch { prefs.setNotifEnabled(on) }
}
