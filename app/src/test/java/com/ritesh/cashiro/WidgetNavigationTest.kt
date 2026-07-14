package com.ritesh.cashiro

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetNavigationTest {

    @Test
    fun `external destination waits for app lock initialization`() {
        assertFalse(
            canOpenExternalDestination(
                appLockInitialized = false,
                isLocked = false,
                isOnLockScreen = false,
            )
        )
    }

    @Test
    fun `external destination stays blocked while locked or on lock screen`() {
        assertFalse(canOpenExternalDestination(true, isLocked = true, isOnLockScreen = false))
        assertFalse(canOpenExternalDestination(true, isLocked = false, isOnLockScreen = true))
    }

    @Test
    fun `external destination opens after authentication leaves lock screen`() {
        assertTrue(canOpenExternalDestination(true, isLocked = false, isOnLockScreen = false))
    }
}
