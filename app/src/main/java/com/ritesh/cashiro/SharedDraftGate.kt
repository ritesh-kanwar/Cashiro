package com.ritesh.cashiro

internal fun canOpenSharedDraft(onboardingFinished: Boolean, checks: Long, baseline: Long, isLocked: Boolean, routeReady: Boolean): Boolean =
    onboardingFinished && checks > baseline && !isLocked && routeReady
