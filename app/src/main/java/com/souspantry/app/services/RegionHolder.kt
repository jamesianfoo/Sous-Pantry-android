package com.souspantry.app.services

/**
 * Process-wide holder for the user's funnel region code ("GB", "AU", …).
 * Populated at startup from DataStore (UserPreferencesRepository init) and on
 * region selection in the funnel; read synchronously by the OkHttp interceptor
 * that stamps X-SP-Region on backend AI calls (the backend routes some regions
 * to local models).
 */
object RegionHolder {
    @Volatile var code: String = ""
}
