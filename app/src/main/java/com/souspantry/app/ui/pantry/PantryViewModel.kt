package com.souspantry.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repo: PantryRepository,
) : ViewModel() {

    val items = repo.items

    fun add(item: PantryItem) = viewModelScope.launch { repo.add(item) }

    fun update(item: PantryItem) = viewModelScope.launch { repo.update(item) }

    fun delete(item: PantryItem) = viewModelScope.launch { repo.delete(item) }
}
