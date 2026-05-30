package com.ritesh.cashiro.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable

// Centralized transition definitions
object CashiroTransitions {
    
    // Horizontal slide transitions for sub-screens
    val horizontalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val horizontalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    // Vertical slide transitions
    val verticalSlideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    val verticalSlideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it / 4},
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }

    val verticalSlidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }

    val verticalSlidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
    }
    
    // FAB to screen scale transitions
    val fabScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(
                targetScale = 1.1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(
                initialScale = 1.1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    val fabScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(
                targetScale = 0.8f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
    }
    
    // Scale transitions for detail screens
    val scaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy))
    }
    
    val scaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
            scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy))
    }
    
    // None transitions - for screens using shared element transitions entirely
    val noneEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    }
    
    val noneExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
    }
}

@Serializable object AppLock

@Serializable object OnBoarding

@Serializable object Home

@Serializable
data class Transactions(
    val category: String? = null,
    val merchant: String? = null,
    val period: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val focusSearch: Boolean = false
)


@Serializable object Settings
@Serializable object Subscriptions
@Serializable object Categories

@Serializable object Analytics

@Serializable object Chat

@Serializable data class TransactionDetail(val transactionId: Long, val sharedElementKey: String? = null)

@Serializable data class AddTransaction(val initialTab: Int = 0, val subscriptionId: Long? = null, val type: String? = null)

@Serializable data class AccountDetail(val bankName: String, val accountLast4: String)

@Serializable object UnrecognizedSms

@Serializable object Faq

@Serializable object Rules

@Serializable data class CreateRule(val ruleId: String? = null)

@Serializable object Appearance

@Serializable object ManageAccounts

@Serializable object Profile

@Serializable object SmsSettings

@Serializable object DataPrivacy


@Serializable object NotificationSettings
@Serializable object Webhooks
@Serializable data class WebhookEditor(val profileId: String? = null)

@Serializable data class Budgets(val sharedElementPrefix: Long? = null)

@Serializable data class BudgetDetail(
    val budgetId: Long, 
    val sharedElementKey: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable data class BudgetHistory(val budgetId: Long)

@Serializable object DeveloperOptions

@Serializable object AddAccount

@Serializable object About

@Serializable object ImportStatement

@Serializable object Licenses

// Routes where bottom navigation should be visible
val BOTTOM_NAV_ROUTES = setOf(
    Home::class.qualifiedName,
    Analytics::class.qualifiedName
)
