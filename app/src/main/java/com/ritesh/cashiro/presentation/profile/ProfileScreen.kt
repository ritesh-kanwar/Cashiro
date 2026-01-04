package com.ritesh.cashiro.presentation.profile

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.theme.blue_dark
import com.ritesh.cashiro.ui.theme.blue_light
import com.ritesh.cashiro.ui.theme.green_dark
import com.ritesh.cashiro.ui.theme.green_light
import com.ritesh.cashiro.ui.theme.orange_dark
import com.ritesh.cashiro.ui.theme.orange_light
import com.ritesh.cashiro.ui.theme.red_dark
import com.ritesh.cashiro.ui.theme.red_light
import com.ritesh.cashiro.utils.CurrencyUtils
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    profileViewModel: ProfileScreenViewModel = hiltViewModel()
) {
    val state by profileViewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorSmall,
                title = "Profile",
                hazeState = hazeState,
                hasBackButton = true,
                onBackClick = onNavigateBack,
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {
                    IconButton(
                        onClick = { profileViewModel.toggleEditSheet() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shapes =  IconButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ProfileContent(
            state = state,
            listState = listState,
            contentPadding = innerPadding
        )
    }

    if (state.isEditSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { profileViewModel.dismissEditSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            EditProfileSheet(
                state = state.editState,
                onNameChange = profileViewModel::updateEditUserName,
                onProfileImageChange = profileViewModel::updateEditProfileImage,
                onBackgroundColorChange = profileViewModel::updateEditProfileBackgroundColor,
                onBannerImageChange = profileViewModel::updateEditBannerImage,
                onSave = profileViewModel::saveProfileChanges,
                onCancel = profileViewModel::dismissEditSheet
            )
        }
    }
}

@Composable
fun ProfileContent(
    state: ProfileScreenState,
    listState: LazyListState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        //Banner image
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp)
        ){
            if ( state.bannerImageUri != null) {
                AsyncImage(
                    model =  state.bannerImageUri,
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize().alpha(0.5f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_bg_image),
                    contentDescription = "Banner",
                    modifier = Modifier.fillMaxSize().alpha(0.5f),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.align(Alignment.BottomCenter).height(80.dp).fillMaxWidth().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent,
                    MaterialTheme.colorScheme.background)
                )
            ))
        }
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().overScrollVertical(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content +
                        contentPadding.calculateTopPadding()
            ),
            flingBehavior = rememberOverscrollFlingBehavior { listState },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DisplayProfileImagesCard(
                    profileImageUri = state.profileImageUri,
                    profileBackgroundColor = state.profileBackgroundColor,
                    state = state
                )
            }

            item {
                FinancialOverviewCard(
                    netWorth = state.netWorth,
                    income = state.totalIncome,
                    expense = state.totalExpense,
                    activeSubscriptions = state.activeSubscriptions
                )
            }
        }
    }

}

@Composable
fun DisplayProfileImagesCard(
    state: ProfileScreenState,
    profileImageUri: Uri?,
    profileBackgroundColor: Color,
) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))) {

        Column(
            modifier = Modifier.fillMaxSize().padding(0.dp, bottom = 20.dp).align(Alignment.BottomStart),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .background(profileBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_1),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            DisplayUserNameAndSubtitles(
                userName = state.userName,
                totalTransactions = state.totalTransactions
            )
        }


    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayUserNameAndSubtitles(userName: String, totalTransactions: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enabled",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.rotate(-rotation).size(12.dp)
                )
            }
        }
        Text(
            text = "Transactions: $totalTransactions",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun FinancialOverviewCard(
    netWorth: java.math.BigDecimal,
    income: java.math.BigDecimal,
    expense: java.math.BigDecimal,
    activeSubscriptions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FinancialItem(
                    label = "Net Worth",
                    value = CurrencyUtils.formatCurrency(netWorth),
                    icon = Icons.Rounded.AccountBalance,
                    color = green_light,
                    iconColor = green_dark,
                    modifier = Modifier.weight(1f)
                )
                FinancialItem(
                    label = "Upcoming",
                    value =
                        if (activeSubscriptions == 1) "1 Sub"
                        else "$activeSubscriptions Subs",
                    icon = Icons.Rounded.CalendarToday,
                    color = orange_light,
                    iconColor = orange_dark,
                    modifier = Modifier.weight(1f)
                )

            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FinancialItem(
                    label = "Expense",
                    value = CurrencyUtils.formatCurrency(expense),
                    icon = Icons.Rounded.TrendingDown,
                    color = red_light,
                    iconColor = red_dark,
                    modifier = Modifier.weight(1f)
                )
                FinancialItem(
                    label = "Income",
                    value = CurrencyUtils.formatCurrency(income),
                    icon = Icons.Rounded.TrendingUp,
                    color = blue_light,
                    iconColor = blue_dark,
                    modifier = Modifier.weight(1f)
                )

            }
        }
    }
}

@Composable
fun FinancialItem(label: String, value: String, icon: ImageVector, color: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(Dimensions.Radius.md)
        ).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(RoundedCornerShape(Dimensions.Radius.md))
                    .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
