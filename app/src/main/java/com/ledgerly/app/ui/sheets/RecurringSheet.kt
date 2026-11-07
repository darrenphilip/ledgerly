package com.ledgerly.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.TxType
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.ui.components.ImeBackHandler
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringSheet(
    editing: Recurring?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Recurring) -> Unit,
    onDelete: (Recurring) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(editing?.type ?: TxType.EXPENSE) }
    var categoryKey by remember {
        mutableStateOf(editing?.categoryKey ?: Categories.defaultForType(TxType.EXPENSE).key)
    }
    var amountText by remember {
        mutableStateOf(editing?.let { Money.plain(it.amountCents, currency) } ?: "")
    }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var cadence by remember { mutableStateOf(editing?.cadence ?: Cadence.MONTHLY) }
    var nextDue by remember { mutableStateOf(editing?.nextDue ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val amountCents = Money.parseToCents(amountText, currency)
    val categories = Categories.forType(type)
    val valid = amountCents != null && categoryKey.isNotEmpty()


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // Back is handled by ImeBackHandler below so the keyboard closes first.
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        ImeBackHandler(onDismiss)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = if (editing == null) "New recurring entry" else "Edit recurring entry",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(TxType.EXPENSE, TxType.INCOME).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            categoryKey = Categories.defaultForType(option).key
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(
                            text = if (option == TxType.EXPENSE) "Expense" else "Income",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.length <= 14 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                        amountText = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = Currencies.symbolOf(currency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text(Money.placeholder(currency)) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountText.isNotEmpty() && amountCents == null
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                placeholder = { Text("Rent, salary, Netflix…") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    RecurringCategoryOption(
                        category = category,
                        selected = category.key == categoryKey,
                        onClick = { categoryKey = category.key }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Repeats",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Cadence.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = cadence == option,
                        onClick = { cadence = option },
                        shape = SegmentedButtonDefaults.itemShape(index, Cadence.entries.size)
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Next due",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateFormats.fullDate(nextDue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val cents = amountCents ?: return@Button
                    onSave(
                        (editing ?: Recurring(
                            amountCents = cents,
                            categoryKey = categoryKey,
                            note = note.trim(),
                            nextDue = nextDue
                        )).copy(
                            type = type,
                            amountCents = cents,
                            categoryKey = categoryKey,
                            note = note.trim(),
                            cadence = cadence,
                            nextDue = nextDue
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (editing == null) "Add recurring entry" else "Save changes",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (editing != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete recurring entry",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextDue.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            nextDue = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteConfirm && editing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete recurring entry?") },
            text = { Text("It stops appearing in the due list. Transactions already logged stay put.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(editing)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecurringCategoryOption(category: Category, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) category.color.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) category.color else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryBadge(category = category, size = 30.dp, emojiSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}