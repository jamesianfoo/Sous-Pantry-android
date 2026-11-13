package com.souspantry.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.services.Plan
import com.souspantry.app.services.PurchaseOutcome
import com.souspantry.app.services.Subscriptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallState(
    val plans      : List<Plan> = emptyList(),
    val loading    : Boolean    = true,
    val selectedId : String     = "yearly",
    val processing : Boolean    = false,
    val error      : String?    = null,
) {
    val selected: Plan? get() = plans.firstOrNull { it.id == selectedId }
}

/** Live Google Play prices via RevenueCat; nothing here grants premium locally. */
@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val subscriptions: Subscriptions,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state = _state.asStateFlow()

    init { loadPlans() }

    fun loadPlans() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        if (!subscriptions.enabled) {
            _state.update { it.copy(loading = false, error = "Billing isn't configured in this build.") }
            return@launch
        }
        runCatching { subscriptions.plans() }
            .onSuccess { plans ->
                _state.update {
                    it.copy(
                        plans   = plans,
                        loading = false,
                        error   = if (plans.isEmpty()) "Plans aren't available right now. Please try again later." else null,
                    )
                }
            }
            .onFailure {
                _state.update { it.copy(loading = false, error = "Couldn't load plans. Check your connection and try again.") }
            }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = id, error = null) }

    fun purchase(activity: Activity, onPurchased: () -> Unit) = viewModelScope.launch {
        val plan = _state.value.selected ?: return@launch
        _state.update { it.copy(processing = true, error = null) }
        val outcome = subscriptions.purchase(activity, plan)
        _state.update { it.copy(processing = false) }
        when (outcome) {
            PurchaseOutcome.Success   -> onPurchased()
            PurchaseOutcome.Cancelled -> Unit
            is PurchaseOutcome.Failed -> _state.update { it.copy(error = outcome.message) }
        }
    }

    fun restore(onRestored: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(processing = true, error = null) }
        val result = subscriptions.restore()
        val found = result.getOrDefault(false)
        val error = result.exceptionOrNull()?.let { "Couldn't reach the store: ${it.message ?: "network error"}" }
            ?: if (found) null else "No active subscription found for this Google account."
        _state.update { it.copy(processing = false, error = error) }
        if (found) onRestored()
    }
}
