package com.cameronbatten.dadmusic.entitlements

import com.cameronbatten.dadmusic.di.ApplicationScope
import com.cameronbatten.dadmusic.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sideload-build [EntitlementSource]: Pro is unlocked by default; the admin
 * screen can force Pro / Free for QA via [UserPreferences.adminProOverride].
 *
 * In the shipped sideload UX there is no trial countdown, no upsell UI, and no
 * visible Pro badge — v1 sideload runs with everything on (spec §9). The
 * admin override only materialises in the hidden admin screen (spec §5.12).
 */
@Singleton
class SideloadEntitlementSource @Inject constructor(
    prefs: UserPreferences,
    @ApplicationScope scope: CoroutineScope,
) : EntitlementSource {
    override val entitlements: StateFlow<Entitlements> =
        prefs.adminProOverride
            .map { override -> Entitlements(proEnabled = override ?: true) }
            .stateIn(scope, SharingStarted.Eagerly, Entitlements(proEnabled = true))
}
