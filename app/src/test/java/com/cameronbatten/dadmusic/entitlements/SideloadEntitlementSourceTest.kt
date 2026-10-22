package com.cameronbatten.dadmusic.entitlements

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SideloadEntitlementSourceTest {

    @Test
    fun entitlements_emitsProEnabledTrueImmediately() = runTest {
        val source = SideloadEntitlementSource()
        val snapshot = source.entitlements.first()
        assertTrue("sideload build must always have proEnabled = true", snapshot.proEnabled)
    }

    @Test
    fun entitlements_valueIsStableAcrossReads() = runTest {
        val source = SideloadEntitlementSource()
        val first = source.entitlements.first()
        val second = source.entitlements.first()
        assertEquals(first, second)
    }
}
