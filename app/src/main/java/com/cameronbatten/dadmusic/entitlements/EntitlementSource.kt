package com.cameronbatten.dadmusic.entitlements

import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for whether Pro features are unlocked.
 *
 * The sideload build (M3) binds [SideloadEntitlementSource], which always
 * emits `proEnabled = true`. v2 swaps in a Play-Billing-backed implementation.
 * Feature code depends only on this interface so the swap is a one-line
 * DI change — no feature file ever touches billing code directly.
 *
 * Callers:
 * - Read the StateFlow once when a gated UI mounts; do not re-check on every
 *   recomposition. The flow re-emits only when entitlement state truly changes.
 */
interface EntitlementSource {
    val entitlements: StateFlow<Entitlements>
}
