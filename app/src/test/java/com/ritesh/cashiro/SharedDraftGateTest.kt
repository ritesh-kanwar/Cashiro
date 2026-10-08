package com.ritesh.cashiro

import org.junit.Assert.*
import org.junit.Test

class SharedDraftGateTest {
    @Test fun requiresCompletedSetupFreshLockCheckAndUnblockedRoute() {
        assertTrue(canOpenSharedDraft(true, 2, 1, false, true))
        assertFalse(canOpenSharedDraft(false, 2, 1, false, true))
        assertFalse(canOpenSharedDraft(true, 1, 1, false, true))
        assertFalse(canOpenSharedDraft(true, 0, 0, false, true))
        assertFalse(canOpenSharedDraft(true, 2, 1, true, true))
        assertFalse(canOpenSharedDraft(true, 2, 1, false, false))
    }
}
