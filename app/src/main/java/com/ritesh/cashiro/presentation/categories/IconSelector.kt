package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.ritesh.cashiro.R
import com.ritesh.cashiro.ui.components.SearchBarBox

data class IconItem(
        val id: Int,
        val name: String,
        val category: String,
        val resourceId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconSelector(
        selectedIconId: Int?,
        onIconSelected: (Int) -> Unit,
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    val allIcons = remember { getAllIcons() }

    val filteredIcons =
            remember(searchQuery.text) {
                if (searchQuery.text.isEmpty()) {
                    allIcons
                } else {
                    allIcons.filter {
                        it.name.contains(searchQuery.text, ignoreCase = true) ||
                                it.category.contains(searchQuery.text, ignoreCase = true)
                    }
                }
            }

    val labels = listOf("Search Fruits", "Search Shopping", "Search Fitness", "Search Sports")
    var currentLabelIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentLabelIndex = (currentLabelIndex + 1) % labels.size
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchBarBox(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                label = {
                    AnimatedContent(
                            targetState = labels[currentLabelIndex],
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                                        slideInVertically(
                                                initialOffsetY = { it },
                                                animationSpec = tween(400, delayMillis = 100)
                                        ))
                                        .togetherWith(
                                                fadeOut(animationSpec = tween(400)) +
                                                        slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400))
                                        )
                            },
                            label = "SearchBarLabelAnimation"
                    ) { labelText ->
                        Text(
                                text = labelText,
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                                modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
                targetState = filteredIcons,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                            scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(220, delayMillis = 90)
                            ) togetherWith fadeOut(animationSpec = tween(90))
                },
                label = "Filtered Icon animated"
        ) { iconsToDisplay ->
            IconFlowLayout(
                    icons = iconsToDisplay,
                    selectedIconId = selectedIconId,
                    onIconSelected = onIconSelected,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun IconFlowLayout(
    icons: List<IconItem>,
    selectedIconId: Int?,
    onIconSelected: (Int) -> Unit,
) {
    val themeColors = MaterialTheme.colorScheme
    val groupedIcons = icons.groupBy { it.category }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    LazyColumn(
            modifier = Modifier.height(screenHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        groupedIcons.forEach { (category, iconsInCategory) ->
            stickyHeader(key = "$category header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    themeColors.surface,
                                    themeColors.surface.copy(alpha = 0.9f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                            text = category.uppercase(),
                            color = themeColors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                    )
                }
            }

            item(key = category) {
                FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                ) {
                    iconsInCategory.forEach { icon ->
                        IconItemView(
                                icon = icon,
                                isSelected = icon.resourceId == selectedIconId,
                                onClick = { onIconSelected(icon.resourceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconItemView(
    icon: IconItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val themeColors = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "Selected Glow animation")

    val animatedColor by
            infiniteTransition.animateColor(
                initialValue = themeColors.primary.copy(alpha = 0.5f),
                targetValue = themeColors.secondary.copy(alpha = 0.5f),
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                label = "Selected Glow animation"
            )

    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(
                            8.dp,
                            RoundedCornerShape(16.dp),
                            spotColor = animatedColor
                        ).border(
                        2.dp,
                        animatedColor,
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))

            .clickable(onClick = onClick)
            .background(
                color = themeColors.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            )

            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
                painter = painterResource(id = icon.resourceId),
                contentDescription = icon.name,
                modifier = Modifier.size(40.dp)
        )
    }
}

private fun getAllIcons(): List<IconItem> {
    val icons = mutableListOf<IconItem>()

    // Helper to add icons
    fun addIcon(name: String, category: String, resId: Int) {
        icons.add(IconItem(icons.size, name, category, resId))
    }

    // Animals
    addIcon("Bear", "Animals", R.drawable.type_animal_bear)
    addIcon("Bird", "Animals", R.drawable.type_animal_bird)
    addIcon("Cat", "Animals", R.drawable.type_animal_cat_face)
    addIcon("Dog", "Animals", R.drawable.type_animal_dog_face)
    addIcon("Fox", "Animals", R.drawable.type_animal_fox)
    addIcon("Lion", "Animals", R.drawable.type_animal_lion)
    addIcon("Panda", "Animals", R.drawable.type_animal_panda)
    addIcon("Tiger", "Animals", R.drawable.type_animal_tiger_face)

    // Beverages
    addIcon("Beer", "Beverages", R.drawable.type_beverages_beer)
    addIcon("Coffee", "Beverages", R.drawable.type_beverages_coffee)
    addIcon("Tea", "Beverages", R.drawable.type_beverages_tea)
    addIcon("Wine", "Beverages", R.drawable.type_beverages_wine_glass)
    addIcon("Cocktail", "Beverages", R.drawable.type_beverages_cocktail_glass)

    // Food
    addIcon("Hamburger", "Food", R.drawable.type_food_hamburger)
    addIcon("Pizza", "Food", R.drawable.type_food_pizza)
    addIcon("Taco", "Food", R.drawable.type_food_taco)
    addIcon("Ramen", "Food", R.drawable.type_food_ramen)
    addIcon("Sushi", "Food", R.drawable.type_food_sushi)
    addIcon("Steak", "Food", R.drawable.type_groceries_cut_of_meat)

    // Fruits
    addIcon("Apple", "Fruits", R.drawable.type_fruit_red_apple)
    addIcon("Banana", "Fruits", R.drawable.type_fruit_banana)
    addIcon("Strawberry", "Fruits", R.drawable.type_fruit_strawberry)
    addIcon("Grapes", "Fruits", R.drawable.type_fruit_grapes)
    addIcon("Mango", "Fruits", R.drawable.type_fruit_mango)

    // Shopping
    addIcon("Cart", "Shopping", R.drawable.type_shopping_shopping_cart)
    addIcon("Bag", "Shopping", R.drawable.type_shopping_shopping_bags)
    addIcon("Dress", "Shopping", R.drawable.type_shopping_dress)
    addIcon("Shirt", "Shopping", R.drawable.type_shopping_t_shirt)

    // Finance
    addIcon("Bank", "Finance", R.drawable.type_finance_bank)
    addIcon("Wallet", "Finance", R.drawable.type_shopping_purse)
    addIcon("Bill", "Finance", R.drawable.type_finance_dollar_banknote)
    addIcon("Chart", "Finance", R.drawable.type_finance_bar_chart)

    // Travel
    addIcon("Airplane", "Travel", R.drawable.type_travel_transport_airplane)
    addIcon("Bus", "Travel", R.drawable.type_travel_transport_bus)
    addIcon("Car", "Travel", R.drawable.type_travel_transport_automobile)
    addIcon("Train", "Travel", R.drawable.type_travel_transport_bullet_train)

    // Health
    addIcon("Hospital", "Health", R.drawable.type_health_hospital)
    addIcon("Pill", "Health", R.drawable.type_health_pill)
    addIcon("Stethoscope", "Health", R.drawable.type_health_stethoscope)

    // Entertainment
    addIcon("Game", "Entertainment", R.drawable.type_tool_electronic_video_game)
    addIcon("Movie", "Entertainment", R.drawable.type_tool_electronic_movie_camera)
    addIcon("Music", "Entertainment", R.drawable.type_tool_electronic_headphone)

    return icons
}
