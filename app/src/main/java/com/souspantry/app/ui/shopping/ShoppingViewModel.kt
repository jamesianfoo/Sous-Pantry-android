package com.souspantry.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingState(
    val items      : List<ShoppingItem> = emptyList(),
    val loading    : Boolean            = false,
    val error      : String?            = null,
    val pantryEmpty: Boolean            = false,
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShoppingState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.items.collect { items ->
                _state.update { it.copy(pantryEmpty = items.isEmpty()) }
            }
        }
    }

    fun generate() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val pantryItems = repo.items.first()

        val result = if (pantryItems.isEmpty()) {
            runCatching { api.generateStaples(emptyMap()) }
        } else {
            val body = mapOf("pantryItems" to pantryItems.map { mapOf("name" to it.name) })
            runCatching { api.generateShoppingList(body) }
        }

        result
            .onSuccess { items -> _state.update { it.copy(items = items, loading = false) } }
            .onFailure { err   -> _state.update { it.copy(error = "Couldn't generate list. Try again.", loading = false) } }
    }

    fun toggle(item: ShoppingItem) {
        _state.update { state ->
            state.copy(items = state.items.map { if (it.id == item.id) it.copy(checked = !it.checked) else it })
        }
    }
}
