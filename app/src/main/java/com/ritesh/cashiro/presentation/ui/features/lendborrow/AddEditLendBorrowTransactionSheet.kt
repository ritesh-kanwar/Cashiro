package com.ritesh.cashiro.presentation.ui.features.lendborrow

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.LendBorrowType
import com.ritesh.cashiro.data.service.AttachmentService
import com.ritesh.cashiro.domain.model.LendBorrowPerson
import com.ritesh.cashiro.domain.model.LendBorrowTransactionItem
import com.ritesh.cashiro.domain.model.PersonCategory
import com.ritesh.cashiro.presentation.ui.components.AccountSelectionSheet
import com.ritesh.cashiro.presentation.ui.components.AttachmentSection
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.presentation.ui.components.DatePicker
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.components.TimePicker
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.features.add.AmountInput
import com.ritesh.cashiro.presentation.ui.icons.Box2
import com.ritesh.cashiro.presentation.ui.icons.Calendar
import com.ritesh.cashiro.presentation.ui.icons.DocumentText2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Information
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ritesh.cashiro.utils.IconResolutionUtils
import com.ritesh.cashiro.utils.horizontalFadingEdge
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
fun AddEditLendBorrowTransactionSheet(
    personsList: List<LendBorrowPerson>,
    initialPerson: LendBorrowPerson? = null,
    accounts: List<AccountBalanceEntity> = emptyList(),
    categories: List<CategoryEntity> = emptyList(),
    attachmentService: AttachmentService,
    showPersonSelection: Boolean = true,
    transactionToEdit: LendBorrowTransactionItem? = null,
    initialType: LendBorrowType? = null,
    initialAmount: BigDecimal? = null,
    initialTitle: String? = null,
    blurEffects: Boolean = LocalBlurEffects.current,
    onDismiss: () -> Unit,
    onAddPerson: (name: String, phone: String?, notes: String?, color: String, avatar: String?, category: PersonCategory?) -> Unit,
    onSave: (personId: Long, type: LendBorrowType, amount: BigDecimal, title: String, dueDate: LocalDateTime?, accountId: Long?, category: String?, merchant: String?, attachments: List<String>) -> Unit,
    onUpdate: (transactionId: Long, type: LendBorrowType, amount: BigDecimal, title: String, dueDate: LocalDateTime?, date: LocalDateTime, accountId: Long?, category: String?, merchant: String?, attachments: List<String>) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> }
) {
    var selectedPerson by remember {
        mutableStateOf(
            transactionToEdit?.let { tx ->
                personsList.find { it.id == tx.personId } ?: initialPerson
            } ?: initialPerson ?: personsList.firstOrNull()
        )
    }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: initialType ?: LendBorrowType.LENT) }
    var amountText by remember {
        mutableStateOf(transactionToEdit?.originalAmount?.stripTrailingZeros()?.toPlainString()
            ?: initialAmount?.stripTrailingZeros()?.toPlainString()
            ?: "0")
    }

    var lastAddedPersonName by remember { mutableStateOf<String?>(null) }

    // Update selected person if list changes and nothing was selected,
    // or auto-select a person that was just added via the in-sheet Add Person flow
    LaunchedEffect(personsList) {
        val lastAdded = lastAddedPersonName
        if (lastAdded != null) {
            personsList.firstOrNull { it.name == lastAdded }?.let { selectedPerson = it }
            lastAddedPersonName = null
        } else if (selectedPerson == null && personsList.isNotEmpty()) {
            selectedPerson = personsList.first()
        }
    }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE) }
    val mainAccountKey = remember { sharedPrefs.getString("main_account", null) }

    var selectedAccount by remember {
        mutableStateOf(
            transactionToEdit?.let { tx ->
                tx.accountId?.let { id -> accounts.find { it.id == id } }
            } ?: accounts.find { "${it.bankName}_${it.accountLast4}" == mainAccountKey } ?: accounts.firstOrNull()
        )
    }

    fun getDefaultCategory(type: LendBorrowType): CategoryEntity? {
        val targetName = if (type == LendBorrowType.LENT) "Lent" else "Borrowed"
        return categories.find { it.name.equals(targetName, ignoreCase = true) }
    }

    var selectedCategory by remember {
        mutableStateOf(
            transactionToEdit?.let { tx ->
                tx.category?.let { name -> categories.find { it.name == name } }
            } ?: getDefaultCategory(selectedType)
        )
    }
    var titleText by remember { mutableStateOf(transactionToEdit?.title ?: initialTitle ?: "") }
    var attachments by remember { mutableStateOf(transactionToEdit?.attachments ?: emptyList()) }
    var dateTime by remember { mutableStateOf(transactionToEdit?.date ?: LocalDateTime.now()) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(transactionToEdit?.dueDate) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNumberPad by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAddPersonSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    // Update category when type changes
    LaunchedEffect(selectedType, transactionToEdit) {
        if (transactionToEdit == null) {
            selectedCategory = getDefaultCategory(selectedType)
        }
    }

    // Handle dismissal with animation when parent state changes
    LaunchedEffect(Unit) {
        // Optional: ensure sheet is expanded
    }

    fun animateDismiss() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(modifier = Modifier.fillMaxWidth().hazeSource(hazeState)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(if (transactionToEdit != null) R.string.edit_record else R.string.add_record),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )

                // Type Switcher
                GenericTypeSwitcher(
                    selectedIndex = if (selectedType == LendBorrowType.LENT) 0 else 1,
                    onIndexChange = { index ->
                        selectedType =
                            if (index == 0) LendBorrowType.LENT else LendBorrowType.BORROWED
                    },
                    options = listOf(
                        stringResource(R.string.type_lent),
                        stringResource(R.string.type_borrowed)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.Radius.md),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Iconax.Information,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (selectedType == LendBorrowType.LENT)
                                stringResource(R.string.amount_they_owe_me)
                            else
                                stringResource(R.string.amount_i_owe_them),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Amount Input
                AmountInput(
                    amount = amountText,
                    currencySymbol = CurrencyFormatter.getCurrencySymbol(
                        selectedAccount?.currency ?: "INR"
                    ),
                    onClick = { showNumberPad = true },
                    modifier = Modifier.fillMaxWidth()
                )

                // Date and Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(Dimensions.Radius.md)
                            )
                            .clip(RoundedCornerShape(Dimensions.Radius.md))
                            .clickable { showDatePicker = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val themeColors = MaterialTheme.colorScheme
                            Icon(
                                imageVector = Iconax.Calendar,
                                contentDescription = stringResource(R.string.date_picker_desc),
                                tint = themeColors.onSurface
                            )
                            Spacer(Modifier.size(8.dp))

                            val dateLabel = dateTime.format(DateTimeFormatter.ofPattern("dd MMMM"))
                            val yearLabel = dateTime.format(DateTimeFormatter.ofPattern("yyyy"))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = yearLabel,
                                    fontSize = 10.sp,
                                    color = themeColors.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = dateLabel,
                                    fontSize = 14.sp,
                                    color = themeColors.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }

                    // Time Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Dimensions.Radius.md))
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
                            val minute = dateTime.minute
                            val amPm =
                                if (dateTime.hour < 12) stringResource(R.string.am_lbl) else stringResource(
                                    R.string.pm_lbl
                                )

                            Box(
                                modifier = Modifier.padding(5.dp).background(
                                    color = MaterialTheme.colorScheme.primary.copy(0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    text = String.format("%02d", hour),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                            Text(
                                text = ":",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                            Box(
                                modifier = Modifier.padding(5.dp).background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                Text(
                                    text = String.format("%02d", minute),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                            Box(modifier = Modifier.padding(5.dp)) {
                                Text(
                                    text = amPm,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Due Date Selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(Dimensions.Radius.md)
                        )
                        .clip(RoundedCornerShape(Dimensions.Radius.md))
                        .clickable { showDueDatePicker = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Iconax.Calendar,
                            contentDescription = null,
                            tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.due_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dueDate?.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy"))
                                    ?: stringResource(R.string.no_due_date),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f
                                )
                            )
                        }
                        if (dueDate != null) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.clear_due_date),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { dueDate = null },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Person Selection Carousel
                if (showPersonSelection) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_person),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val personLazyListState = rememberLazyListState()
                        LazyRow(
                            state = personLazyListState,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalFadingEdge(
                                    canScrollBackward = personLazyListState.canScrollBackward,
                                    canScrollForward = personLazyListState.canScrollForward
                                )
                        ) {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showAddPersonSheet = true }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_person),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.add_new),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(personsList, key = { it.id }) { person ->
                                val isSelected = selectedPerson?.id == person.id
                                val colorInt = try {
                                    android.graphics.Color.parseColor(person.color)
                                } catch (_: Exception) {
                                    android.graphics.Color.parseColor("#4CAF50")
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPerson = person }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (person.avatar != null) {
                                            AsyncImage(
                                                model = person.avatar,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = person.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = person.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(56.dp)
                                            .padding(top = 4.dp),
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Account and Category
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Card(
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BrandIcon(
                                merchantName = selectedAccount?.bankName ?: "",
                                accountIconResId = selectedAccount?.iconResId ?: 0,
                                accountIconName = selectedAccount?.iconName,
                                size = 26.dp,
                                showBackground = false
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAccount?.bankName
                                        ?: stringResource(R.string.select_account),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedAccount != null) {
                                    Text(
                                        text = if (selectedAccount?.accountLast4 == "wallet") "${selectedAccount?.accountLast4}" else "••${selectedAccount?.accountLast4}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            label = {
                                Text(
                                    stringResource(R.string.category_label),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            leadingIcon = {
                                val context = LocalContext.current
                                val resolvedResId = remember(selectedCategory) {
                                    selectedCategory?.let { cat ->
                                        if (!cat.iconName.isNullOrEmpty()) {
                                            val res = IconResolutionUtils.nameToResId(
                                                context,
                                                cat.iconName
                                            )
                                            if (res != 0) res else cat.iconResId
                                        } else cat.iconResId
                                    } ?: 0
                                }

                                if (resolvedResId != 0) {
                                    Icon(
                                        painter = painterResource(id = resolvedResId),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(Iconax.Box2, contentDescription = null)
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            },
                            enabled = false,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                ),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                disabledIndicatorColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .clickable { showCategoryMenu = true }
                        )
                    }
                }

                TextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.description)) },
                    leadingIcon = { Icon(Iconax.DocumentText2, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Attachments
                AttachmentSection(
                    attachments = attachments,
                    attachmentService = attachmentService,
                    onAddAttachment = { attachments = attachments + it },
                    onRemoveAttachment = { path ->
                        attachments = attachments - path
                    },
                    onAttachmentClick = { path ->
                        if (attachmentService.isUrl(path)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle error
                            }
                        } else {
                            val uri = attachmentService.getAttachmentUri(path)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, attachmentService.getAttachmentMimeType(path))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle error
                                }
                            }
                        }
                    },
                    isEditable = true
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        val parsedAmount = amountText.toBigDecimalOrNull()
                        val personId = selectedPerson?.id
                        val merchant = transactionToEdit?.merchant ?: selectedPerson?.name

                        if (personId != null && parsedAmount != null && parsedAmount > BigDecimal.ZERO) {
                            scope.launch {
                                sheetState.hide()
                                val tx = transactionToEdit
                                if (tx != null) {
                                    onUpdate(
                                        tx.id,
                                        selectedType,
                                        parsedAmount,
                                        titleText,
                                        dueDate,
                                        dateTime,
                                        selectedAccount?.id,
                                        selectedCategory?.name,
                                        merchant,
                                        attachments
                                    )
                                } else {
                                    onSave(
                                        personId,
                                        selectedType,
                                        parsedAmount,
                                        titleText,
                                        dueDate,
                                        selectedAccount?.id,
                                        selectedCategory?.name,
                                        merchant,
                                        attachments
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = Dimensions.Padding.content)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(56.dp),
                    enabled = titleText.isNotBlank(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showNumberPad) {
        ModalBottomSheet(
            onDismissRequest = { showNumberPad = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NumberPad(
                initialValue = amountText,
                onDone = { 
                    amountText = it
                    showNumberPad = false 
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = { 
                datePickerState.selectedDateMillis?.let { millis ->
                    dateTime = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                }
                showDatePicker = false 
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate ?: LocalDateTime.now()).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePicker(
            onDismiss = { showDueDatePicker = false },
            onConfirm = { 
                datePickerState.selectedDateMillis?.let { millis ->
                    dueDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                }
                showDueDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute
        )
        TimePicker(
            onDismiss = { showTimePicker = false },
            onConfirm = { 
                dateTime = dateTime.withHour(timePickerState.hour).withMinute(timePickerState.minute)
                showTimePicker = false 
            },
            timePickerState = timePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AccountSelectionSheet(
                accounts = accounts,
                selectedAccount = selectedAccount,
                showNoneOption = false,
                onAccountSelected = { 
                    selectedAccount = it 
                    showAccountSheet = false 
                }
            )
        }
    }

    if (showCategoryMenu) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategorySelectionSheet(
                categories = categories,
                subcategoriesMap = emptyMap(),
                onSelectionComplete = { cat, _ -> 
                    selectedCategory = cat 
                    showCategoryMenu = false 
                },
                onDismiss = { showCategoryMenu = false }
            )
        }
    }

    if (showAddPersonSheet) {
        AddEditPersonSheet(
            attachmentService = attachmentService,
            onDismiss = { showAddPersonSheet = false },
            onSave = { name, phone, notes, color, avatar, category ->
                lastAddedPersonName = name
                onAddPerson(name, phone, notes, color, avatar, category)
                showAddPersonSheet = false
            }
        )
    }
}
