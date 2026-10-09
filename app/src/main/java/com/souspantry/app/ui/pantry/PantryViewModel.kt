package com.souspantry.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.NavSignals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repo    : PantryRepository,
    private val prefs   : UserPreferencesRepository,
    private val signals : NavSignals,
) : ViewModel() {

    val items = repo.items

    /** Home "View Expiring Items" asked for the Expiring filter (iOS session.showExpiringFilter). */
    val showExpiringFilter = signals.showExpiringFilter
    fun clearExpiringFilter() { signals.showExpiringFilter.value = false }

    /** iOS stamps sp_lastAuditDate whenever the "Expiring" filter is selected. */
    fun markAuditChecked() = viewModelScope.launch { prefs.setLastAuditDate() }

    fun add(item: PantryItem) = viewModelScope.launch { repo.add(item) }

    fun update(item: PantryItem) = viewModelScope.launch { repo.update(item) }

    fun delete(item: PantryItem) = viewModelScope.launch { repo.delete(item) }
}
