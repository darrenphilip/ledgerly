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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.Money

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetSheet(
    existingCategoryKey: String?,
    existingLimitCents: Long?,
    availableCategories: List<Category>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
    onDelete: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedKey by remember {
        mutableStateOf(existingCategoryKey ?: availableCategories.firstOrNull()?.key)
    }
    var amountText by remember {
        mutableStateOf(existingLimitCents?.let { Money.plain(it, currency) } ?: "")
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val amountCents = Money.parseToCents(amountText, currency)
    val existingCategory = existingCategoryKey?.let { Categories.of(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
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
                text = if (existingCategory != null) "Edit budget" else "New category budget",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            if (existingCategory != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category = existingCategory, size = 46.dp, emojiSize = 21.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = existingCategory.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Monthly limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
                    availableCategories.forEach { category ->
                        BudgetCategoryOption(
                            category = category,
                            selected = category.key == selectedKey,
                            onClick = { selectedKey = category.key }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.length <= 14 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                        amountText = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly limit") },
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
                isError = amountText.isNotEmpty() && amountCents == null,
                supportingText = if (amountText.isNotEmpty() && amountCents == null) {
                    { Text("Enter an amount greater than zero") }
                } else {
                    { Text("You can change this whenever you like.") }
                }
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val key = selectedKey ?: return@Button
                    val cents = amountCents ?: return@Button
                    onSave(key, cents)
                },
                enabled = amountCents != null && selectedKey != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (existingCategory != null) "Save budget" else "Create budget",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (existingCategory != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Remove budget",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && existingCategoryKey != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove this budget?") },
            text = { Text("Spending in this category will no longer be tracked against a limit.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existingCategoryKey)
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BudgetCategoryOption(category: Category, selected: Boolean, onClick: () -> Unit) {
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
        Text(text = category.emoji, fontSize = 20.sp)
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

@Composable
fun BudgetAmountDialog(
    title: String,
    description: String,
    initialCents: Long,
    currency: String,
    removeLabel: String = "Remove monthly budget",
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (initialCents > 0L) Money.plain(initialCents, currency) else "")
    }
    val amountCents = Money.parseToCents(amountText, currency)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.length <= 14 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        Text(
                            text = Currencies.symbolOf(currency),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = { Text(Money.placeholder(currency)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (initialCents > 0L) {
                    TextButton(onClick = { onSave(0L) }) {
                        Text(
                            text = removeLabel,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountCents != null,
                onClick = { amountCents?.let(onSave) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
