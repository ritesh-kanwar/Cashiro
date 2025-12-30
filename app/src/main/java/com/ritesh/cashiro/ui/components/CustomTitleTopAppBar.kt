package com.ritesh.cashiro.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.HazeDefaults.tint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CustomTitleTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    previousScreenTitle: String = "",
    onBackClick : () -> Unit = {},
    hasBackButton: Boolean = false,
    onEditClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSearchButtonClick: () -> Unit = {},
    onFilterButtonClick: () -> Unit = {},
    hasFilterButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    greetingCard: @Composable () -> Unit = {},
    profilePhoto: @Composable () -> Unit = {},
    hazeState: HazeState = HazeState(),
) {
    val collapsedFraction = scrollBehaviorLarge.state.collapsedFraction
    val displayTitle = remember { mutableStateOf(previousScreenTitle.ifEmpty { title }) }

    // LargeTopAppBar
    LargerTopAppBar(
        scrollBehaviorLarge = scrollBehaviorLarge,
        title = title,
        hasBackButton = hasBackButton,
        collapsedFraction = collapsedFraction,
        actionContent = actionContent,
        navigationContent = navigationContent,
        greetingCard = greetingCard,
        hazeState = hazeState,
        themeColors = MaterialTheme.colorScheme
    )

    // Regular TopAppBar
    RegularTopAppBar(
        scrollBehaviorSmall = scrollBehaviorSmall,
        title = title,
        onBackClick = onBackClick,
        hasBackButton = hasBackButton,
        onEditClick = onEditClick,
        onSearchButtonClick = onSearchButtonClick,
        onFilterButtonClick = onFilterButtonClick,
        hasFilterButton = hasFilterButton,
        actionContent = actionContent,
        navigationContent = navigationContent,
        collapsedFraction = collapsedFraction,
        profilePhoto = profilePhoto,
        modifier = modifier,
        hazeState = hazeState
    )

}


@Composable
private fun Modifier.animatedOffsetModifier(
    hasBackButton: Boolean,
    hasOnlyActionButtons: Boolean = false,
    isProfileScreen: Boolean = false,
    isTransactionScreen : Boolean = false,
    isHomeScreen: Boolean = false,
    isCategoryScreen: Boolean = false,
    isRuleScreen: Boolean = false,
    isCreateRuleScreen: Boolean = false,
): Modifier {
    // Define the target offset based on conditions
    val targetOffsetX = when {
        hasBackButton && isProfileScreen-> 0.dp
        isTransactionScreen -> (0).dp
        hasOnlyActionButtons && isHomeScreen-> (26).dp
        hasBackButton && isCategoryScreen-> 0.dp
        hasBackButton && isRuleScreen-> 0.dp
        hasBackButton -> (-26).dp
        else -> (-10).dp
    }

    // Convert to pixels for animation
    val density = LocalDensity.current
    val targetOffsetXPx = with(density) { targetOffsetX.toPx() }

    val transition = updateTransition(
        targetState = Triple(hasBackButton, false, targetOffsetXPx), // false for isInSelectionMode
        label = "offsetTransition"
    )

    val animatedOffsetX by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "offsetX"
    ) { (_, _, offset) -> offset }

    // Apply offset directly as a float value instead of rounding to Int
    return this
        .fillMaxWidth()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                // Use the exact float value for positioning
                placeable.placeRelative(x = animatedOffsetX.toInt(), y = 0)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun LargerTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorLarge: TopAppBarScrollBehavior,
    title: String,
    hasBackButton: Boolean = false,
    collapsedFraction: Float,
    greetingCard: @Composable () -> Unit = {} ,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    hazeState: HazeState,
    themeColors: ColorScheme,

    ){
    LargeTopAppBar(
        title = {
            TitleForLargeTopAppBar(
                title = title,
                modifier = modifier ,
                greetingCard = greetingCard,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor =  Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        navigationIcon = {
            NavigationForLargeTopAppBar(
                hasBackButton = hasBackButton,
                navigationContent = navigationContent
            )
        },
        actions = {
            ActionForLargeTopAppBar(
                actionContent = actionContent
            )
        },
        collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight,
        expandedHeight = if (title == "Cashiro") 150.dp else 110.dp,
        windowInsets = WindowInsets(0.dp),
        scrollBehavior = scrollBehaviorLarge,
        modifier = Modifier
            .fillMaxWidth()
            .hazeEffect(
                state = hazeState,
                block = fun HazeEffectScope.() {
                    this.inputScale = HazeInputScale.Auto
                    style = HazeDefaults.style(
                        backgroundColor = Color.Transparent,
                        tint = tint(backgroundColor),
                        blurRadius = 10.dp,
                        noiseFactor = -1f,
                    )
                    progressive =
                        HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.background,
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .alpha(1f - collapsedFraction)
    )
}

@Composable
private fun TitleForLargeTopAppBar(
    modifier: Modifier = Modifier,
    title: String,
    greetingCard: @Composable () -> Unit = {},
){
    BlurredAnimatedVisibility(
        visible = title != "Home",
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 10.dp)
        )
    }

    if (title == "Cashiro"){
        greetingCard()
    }
}

@Composable
private fun NavigationForLargeTopAppBar(
    hasBackButton: Boolean = false,
    navigationContent: @Composable () -> Unit = {},
){
    BlurredAnimatedVisibility(
        visible = hasBackButton,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        navigationContent()
    }
}

@Composable
private fun ActionForLargeTopAppBar(
    actionContent: @Composable () -> Unit = {},
){
    BlurredAnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        actionContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun RegularTopAppBar(
    modifier: Modifier = Modifier,
    scrollBehaviorSmall: TopAppBarScrollBehavior,
    title: String,
    onBackClick : () -> Unit = {},
    hasBackButton: Boolean = false,
    onEditClick: () -> Unit = {},
    onSearchButtonClick: () -> Unit = {},
    onFilterButtonClick: () -> Unit = {},
    hasFilterButton: Boolean = false,
    actionContent: @Composable () -> Unit = {},
    navigationContent: @Composable () -> Unit = {},
    profilePhoto: @Composable () -> Unit = {},
    collapsedFraction: Float,
    hazeState: HazeState,
){
    BlurredAnimatedVisibility(
        visible = collapsedFraction > 0.01f,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val isProfileScreen = title == "Profile"
        val isTransactionScreen = title == "Transactions"
        val isSearchTransactionScreen = title == "Search Transactions"

        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.animatedOffsetModifier(
                        hasBackButton = hasBackButton,
                        isProfileScreen = isProfileScreen,
                        isTransactionScreen = isTransactionScreen,
                        isHomeScreen = title == "Cashiro",
                        hasOnlyActionButtons =  title == "Cashiro",
                        isCategoryScreen = title == "Categories",
                        isRuleScreen = title == "Smart Rules",
                        isCreateRuleScreen = title == "Create Rule",
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            navigationIcon = {
                BlurredAnimatedVisibility(
                    visible = hasBackButton,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    navigationContent()
                }
            },
            actions = {
                actionContent()
            },
            scrollBehavior = scrollBehaviorSmall,
            windowInsets = WindowInsets(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            blurRadius = 10.dp,
                            noiseFactor = -1f,
                        )
                        progressive =
                            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .alpha(collapsedFraction)
        )
    }
}

