package com.souspantry.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingState(
    val items      : List<ShoppingItem> = emptyList(),
    val loading    : Boolean            = false,
    val error      : String?            = null,
    val pantryEmpty: Boolean            = true,
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

    // ── AI generation ────────────────────────────────────────────────────────

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
            .onSuccess { newItems ->
                _state.update {
                    // Merge new AI items with existing (avoid duplicates by name, case-insensitive)
                    val existingNames = it.items.map { i -> i.name.trim().lowercase() }.toSet()
                    val freshFromAi   = newItems.filterNot { item -> item.name.trim().lowercase() in existingNames }
                    it.copy(items = it.items + freshFromAi, loading = false)
                }
            }
            .onFailure { _state.update { it.copy(error = "Couldn't generate list. Try again.", loading = false) } }
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    fun add(name: String, quantity: String? = null, priority: String = "essential") {
        if (name.isBlank() || existsExact(name)) return
        val item = ShoppingItem(
            name     = name.trim(),
            category = null,
            quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
            priority = priority,
            reason   = null,
        )
        _state.update { it.copy(items = it.items + item) }
    }

    fun update(itemId: String, name: String, quantity: String?, priority: String) {
        _state.update { state ->
            state.copy(items = state.items.map {
                if (it.id == itemId) it.copy(
                    name     = name.trim(),
                    quantity = quantity?.trim()?.takeIf { q -> q.isNotEmpty() },
                    priority = priority,
                ) else it
            })
        }
    }

    fun delete(item: ShoppingItem) {
        _state.update { it.copy(items = it.items.filterNot { i -> i.id == item.id }) }
    }

    fun toggle(item: ShoppingItem) {
        _state.update { state ->
            state.copy(items = state.items.map {
                if (it.id == item.id) it.copy(checked = !it.checked) else it
            })
        }
    }

    /** Returns true if the list already has an item with the exact same name (trimmed, case-insensitive). */
    fun existsExact(name: String): Boolean {
        val needle = name.trim().lowercase()
        return _state.value.items.any { it.name.trim().lowercase() == needle }
    }

    // ── Basket actions ──────────────────────────────────────────────────────

    /**
     * Moves all currently-checked items into the pantry (adding fresh PantryItems
     * with quantity = 1) and removes them from the shopping list.
     */
    fun moveCheckedToPantry() = viewModelScope.launch {
        val checked = _state.value.items.filter { it.checked }
        if (checked.isEmpty()) return@launch

        val pantryItems = checked.map {
            PantryItem(name = it.name, category = it.category, quantity = 1)
        }
        repo.addAll(pantryItems)
        _state.update { it.copy(items = it.items.filterNot { i -> i.checked }) }
    }

    fun clearChecked() {
        _state.update { it.copy(items = it.items.filterNot { i -> i.checked }) }
    }
}
