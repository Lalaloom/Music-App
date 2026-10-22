package com.cameronbatten.dadmusic.entitlements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sideload-build [EntitlementSource]: everything is unlocked, always.
 *
 * This is the only entitlement source shipped in v1. No trial countdown, no
 * upsell UI, no Pro badges visible to users (per spec §9).
 */
@Singleton
class SideloadEntitlementSource @Inject constructor() : EntitlementSource {
    override val entitlements: StateFlow<Entitlements> =
        MutableStateFlow(Entitlements(proEnabled = true)).asStateFlow()
}
