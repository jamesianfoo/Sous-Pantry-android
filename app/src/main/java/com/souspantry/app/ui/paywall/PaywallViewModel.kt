package com.souspantry.app.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A subscription plan shown on the paywall. Pricing is static placeholder copy
 *  until Google Play Billing products are wired (Play Console + billing client). */
data class PaywallPlan(
    val id        : String,
    val period    : String,   // "Monthly" | "Yearly"
    val price     : String,   // billed amount, e.g. "$7.99"
    val subtitle  : String,   // "billed monthly" | "$4.17/month"
    val savings   : Int?,     // SAVE % badge, null = none
    val periodShort: String,  // "month" | "year"
)

/** TODO(billing): replace with live Google Play Billing ProductDetails. */
val PAYWALL_PLANS = listOf(
    PaywallPlan("yearly",  "Yearly",  "$49.99", "$4.17/month",  savings = 48, periodShort = "year"),
    PaywallPlan("monthly", "Monthly", "$7.99",  "billed monthly", savings = null, periodShort = "month"),
)

data class PaywallState(
    val selectedId : String  = "yearly",
    val processing  : Boolean = false,
    val error       : String? = null,
) {
    val selected: PaywallPlan? get() = PAYWALL_PLANS.firstOrNull { it.id == selectedId }
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state = _state.asStateFlow()

    fun select(id: String) = _state.update { it.copy(selectedId = id, error = null) }

    /**
     * Placeholder "purchase": grants premium locally so the app works end-to-end
     * for testing. Real Google Play Billing (BillingClient + Play Console
     * subscription products) replaces this — the entitlement write stays the same.
     */
    fun purchase(onPurchased: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(processing = true, error = null) }
        delay(600) // mimic the billing round-trip
        prefs.setPremiumActive(true)
        _state.update { it.copy(processing = false) }
        onPurchased()
    }

    /** Placeholder restore — real flow queries past Play purchases. */
    fun restore(onRestored: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(processing = true, error = null) }
        delay(400)
        _state.update { it.copy(processing = false, error = "No purchases to restore.") }
    }
}
