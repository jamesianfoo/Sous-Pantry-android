package com.souspantry.app.services

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot cross-tab signals Home raises before switching tabs, mirroring the
 * iOS `session.showExpiringFilter` / `pendingSousMessage` flags. The consumer
 * resets the value once it has acted on it.
 */
@Singleton
class NavSignals @Inject constructor() {
    /** Pantry opens on the "Expiring" filter when true (Home "View Expiring Items"). */
    val showExpiringFilter = MutableStateFlow(false)
    /** Plan & Cook sends this as the opening chat message (Home "See what to cook →"). */
    val pendingSousMessage = MutableStateFlow<String?>(null)
}
