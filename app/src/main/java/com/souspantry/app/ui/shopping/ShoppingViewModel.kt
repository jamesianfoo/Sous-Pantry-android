package com.souspantry.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.data.repository.ShoppingRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingState(
    val items      : List<ShoppingItem> = emptyList(),
    val loading    : Boolean            = false,
    val error      : String?            = null,
    val pantryEmpty: Boolean            = true,
)

/** Transient (non-persisted) UI flags layered over the persisted shopping list. */
private data class ShoppingTransient(
    val loading : Boolean = false,
    val error   : String? = null,
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val api      : ApiService,
    private val pantry   : PantryRepository,
    private val shopping : ShoppingRepository,
) : ViewModel() {

    private val _transient = MutableStateFlow(ShoppingTransient())

    val state: StateFlow<ShoppingState> = combine(
        shopping.items, pantry.items, _transient,
    ) { items, pantryItems, t ->
        ShoppingState(
            items       = items,
            loading     = t.loading,
            error       = t.error,
            pantryEmpty = pantryItems.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingState())

    // ── AI generation ────────────────────────────────────────────────────────

    fun generate() = viewModelScope.launch {
        _transient.update { it.copy(loading = true, error = null) }
        val pantryItems = pantry.items.first()

        val result = if (pantryItems.isEmpty()) {
            runCatching { api.generateStaples(emptyMap()) }
        } else {
            val body = mapOf("pantryItems" to pantryItems.map { mapOf("name" to it.name) })
            runCatching { api.generateShoppingList(body) }
        }

        result
            .onSuccess { newItems ->
                // Merge — skip names already on the list (case-insensitive).
                // Assign a fresh id per item so the Room primary key is never null
                // (Gson doesn't apply the Kotlin default id when the field is absent).
                val fresh = newItems
                    .filterNot { shopping.exists(it.name) }
                    .map { it.copy(id = java.util.UUID.randomUUID().toString()) }
                shopping.upsertAll(fresh)
                _transient.update { it.copy(loading = false) }
            }
            .onFailure { _transient.update { it.copy(error = "Couldn't generate list. Try again.", loading = false) } }
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    fun add(name: String, quantity: String? = null, priority: String = "essential") = viewModelScope.launch {
        if (name.isBlank() || shopping.exists(name)) return@launch
        shopping.upsert(
            ShoppingItem(
                name     = name.trim(),
                category = null,
                quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
                priority = priority,
                reason   = null,
            )
        )
    }

    fun update(itemId: String, name: String, quantity: String?, priority: String) = viewModelScope.launch {
        val existing = state.value.items.firstOrNull { it.id == itemId } ?: return@launch
        shopping.upsert(
            existing.copy(
                name     = name.trim(),
                quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
                priority = priority,
            )
        )
    }

    fun delete(item: ShoppingItem) = viewModelScope.launch { shopping.delete(item.id) }

    fun toggle(item: ShoppingItem) = viewModelScope.launch {
        shopping.upsert(item.copy(checked = !item.checked))
    }

    // ── Basket actions ──────────────────────────────────────────────────────

    /** Moves checked items into the pantry (qty 1) and removes them from the list. */
    fun moveCheckedToPantry() = viewModelScope.launch {
        val checked = shopping.getChecked()
        if (checked.isEmpty()) return@launch
        pantry.addAll(checked.map { PantryItem(name = it.name, category = it.category, quantity = 1) })
        shopping.deleteChecked()
    }

    fun clearChecked() = viewModelScope.launch { shopping.deleteChecked() }
}
