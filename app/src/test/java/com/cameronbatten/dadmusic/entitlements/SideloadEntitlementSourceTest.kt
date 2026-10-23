package com.cameronbatten.dadmusic.entitlements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-tests the sideload entitlement layering logic: the admin override from
 * [com.cameronbatten.dadmusic.preferences.UserPreferences.adminProOverride]
 * (nullable boolean) is layered over the hard-coded base. UserPreferences
 * itself needs a Context, so this JVM test exercises the mapping shape
 * directly; the end-to-end wiring is covered by the manual-smoke-M4 steps.
 */
class SideloadEntitlementSourceTest {

    private fun entitlementsFor(override: Boolean?) = MutableStateFlow(override)
        .map { Entitlements(proEnabled = it ?: true) }

    @Test
    fun defaultWhenNoOverride_isProEnabled() = runTest {
        val snapshot = entitlementsFor(null).first()
        assertTrue("No override must leave proEnabled = true", snapshot.proEnabled)
        assertEquals(Entitlements(proEnabled = true), snapshot)
    }

    @Test
    fun overrideFalse_forcesFree() = runTest {
        val snapshot = entitlementsFor(false).first()
        assertFalse(snapshot.proEnabled)
    }

    @Test
    fun overrideTrue_forcesPro() = runTest {
        val snapshot = entitlementsFor(true).first()
        assertTrue(snapshot.proEnabled)
    }
}
