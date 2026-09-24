package com.souspantry.app.services

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.souspantry.app.BuildConfig
import com.souspantry.app.data.local.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/** A purchasable plan, shaped for the paywall. */
data class Plan(
    val id          : String,     // "monthly" | "yearly"
    val period      : String,     // "Monthly" | "Yearly"
    val periodShort : String,     // "month" | "year"
    val price       : String,     // store-formatted, e.g. "A$7.99"
    val subtitle    : String,     // "billed monthly" | "A$4.17/month"
    val savings     : Int?,       // SAVE % vs monthly, yearly only
    val freeTrial   : String?,    // "7-day free trial" when Play offers one on this plan, else null
    val rcPackage   : Package,
)

sealed interface PurchaseOutcome {
    data object Success   : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Failed(val message: String) : PurchaseOutcome
}

/**
 * Subscriptions via RevenueCat over Google Play Billing, mirroring iOS
 * `SubscriptionService.swift`: same entitlement, same monthly/annual packages
 * from the current offering, same email-as-user-ID so one dashboard covers
 * both apps. Writes the entitlement into prefs so existing `premiumActive`
 * readers (Plan & Cook gates, Account) keep working untouched.
 */
@Singleton
class Subscriptions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** False when no RevenueCat key is configured; the paywall says so instead of faking a purchase. */
    val enabled: Boolean get() = BuildConfig.REVENUECAT_ANDROID_KEY.isNotBlank()

    /** Call once from Application.onCreate. */
    fun configure() {
        if (!enabled || Purchases.isConfigured) return
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        Purchases.configure(PurchasesConfiguration.Builder(context, BuildConfig.REVENUECAT_ANDROID_KEY).build())
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> scope.launch { apply(info) } }
        scope.launch { runCatching { apply(Purchases.sharedInstance.awaitCustomerInfo()) } }
    }

    /** The current offering's monthly and annual packages, yearly first (as on the paywall). */
    suspend fun plans(): List<Plan> {
        if (!enabled) return emptyList()
        val current = Purchases.sharedInstance.awaitOfferings().current ?: return emptyList()
        val monthly = current.monthly
        val annual  = current.annual
        val monthlyMicros = monthly?.product?.price?.amountMicros

        val plans = mutableListOf<Plan>()
        annual?.let { pkg ->
            val micros   = pkg.product.price.amountMicros
            val perMonth = format(micros / 12, pkg.product.price.currencyCode)
            val savings  = monthlyMicros?.takeIf { it > 0 }
                ?.let { m -> (100 - (micros / 12.0) / m * 100).toInt() }
                ?.takeIf { it > 0 }
            plans += Plan("yearly", "Yearly", "year", pkg.product.price.formatted, "$perMonth/month", savings, trialLabel(pkg), pkg)
        }
        monthly?.let { pkg ->
            plans += Plan("monthly", "Monthly", "month", pkg.product.price.formatted, "billed monthly", null, trialLabel(pkg), pkg)
        }
        return plans
    }

    suspend fun purchase(activity: Activity, plan: Plan): PurchaseOutcome {
        if (!enabled) return PurchaseOutcome.Failed("Billing isn't configured in this build.")
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, plan.rcPackage).build())
            apply(result.customerInfo)
            if (isPro(result.customerInfo)) PurchaseOutcome.Success
            else PurchaseOutcome.Failed("Payment went through but the subscription hasn't activated yet. Try Restore Purchase in a moment.")
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseOutcome.Cancelled
            else PurchaseOutcome.Failed(e.message ?: "Purchase failed. Please try again.")
        } catch (e: Exception) {
            Log.e("Subscriptions", "purchase failed", e)
            PurchaseOutcome.Failed("Purchase failed. Please try again.")
        }
    }

    /** True if an active subscription was found for this Google account. */
    suspend fun restore(): Boolean {
        if (!enabled) return false
        return runCatching { Purchases.sharedInstance.awaitRestore() }
            .onSuccess { apply(it) }
            .map { isPro(it) }
            .getOrDefault(false)
    }

    /** After sign-in: the email becomes the RevenueCat user ID, so purchases follow the account (as on iOS). */
    fun identify(email: String) {
        if (!enabled || email.isBlank()) return
        scope.launch {
            runCatching { Purchases.sharedInstance.awaitLogIn(email.trim()) }
                .onSuccess { apply(it.customerInfo) }
                .onFailure { Log.w("Subscriptions", "identify failed", it) }
        }
    }

    /** On sign-out / deletion: back to anonymous so the next user doesn't inherit this entitlement. */
    fun unidentify() {
        if (!enabled) return
        scope.launch {
            runCatching { Purchases.sharedInstance.awaitLogOut() }.onSuccess { apply(it) }
            prefs.setPremiumActive(false)
        }
    }

    /** Trial copy comes from the store product, never from hardcoded text (as on iOS). */
    private fun trialLabel(pkg: Package): String? {
        val period = pkg.product.subscriptionOptions?.freeTrial?.freePhase?.billingPeriod ?: return null
        val days = when (period.unit) {
            com.revenuecat.purchases.models.Period.Unit.DAY   -> period.value
            com.revenuecat.purchases.models.Period.Unit.WEEK  -> period.value * 7
            com.revenuecat.purchases.models.Period.Unit.MONTH -> period.value * 30
            else -> return null
        }
        return "$days-day free trial"
    }

    private suspend fun apply(info: CustomerInfo) = prefs.setPremiumActive(isPro(info))

    private fun isPro(info: CustomerInfo) = info.entitlements[ENTITLEMENT]?.isActive == true

    private fun format(micros: Long, currencyCode: String): String =
        runCatching {
            NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }
                .format(micros / 1_000_000.0)
        }.getOrDefault("%.2f".format(micros / 1_000_000.0))

    companion object {
        /** Must match the entitlement identifier in the RevenueCat dashboard (shared with iOS). */
        const val ENTITLEMENT = "Sous Pantry Pro"
    }
}
