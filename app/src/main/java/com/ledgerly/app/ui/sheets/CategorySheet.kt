package com.ledgerly.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.data.TxType
import com.ledgerly.app.ui.components.ImeBackHandler

/** Emoji choices for the picker. Grouped loosely: money, life, fun. */
private val emojiChoices = listOf(
    "🍔", "🛒", "🍜", "☕", "🚌", "🚗", "⛽", "🛍️", "👕", "💊", "🏥", "🏠",
    "🧾", "📱", "💡", "🎬", "🎮", "🎵", "✈️", "🏖️", "📚", "🎓", "🔁", "💳",
    "🏦", "📈", "💰", "🎁", "🐶", "🧸", "🔧", "✨"
)

/** Palette used for new categories, kept distinct from the built-in colours. */
private val palette = listOf(
    Color(0xFFF97316), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFA855F7),
    Color(0xFFEAB308), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF06B6D4),
    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFF94A3B8), Color(0xFF64748B)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategorySheet(
    editing: Category?,
    usageCount: Int,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBuiltIn = editing != null && Categories.isBuiltIn(editing.key)

    var type by remember { mutableStateOf(editing?.type ?: TxType.EXPENSE) }
    var label by remember { mutableStateOf(editing?.label ?: "") }
    var emoji by remember { mutableStateOf(editing?.emoji ?: emojiChoices.first()) }
    var color by remember { mutableStateOf(editing?.color ?: palette.first()) }
    var bucket by remember { mutableStateOf(editing?.bucket) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val validLabel = label.trim().isNotEmpty()
    val expenseType = type == TxType.EXPENSE


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
                text = when {
                    editing == null -> "New category"
                    isBuiltIn -> "Edit category"
                    else -> "Edit category"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isBuiltIn) {
                    "Built-in categories can be renamed and re-sorted, but not deleted."
                } else {
                    "Categories decide where your money is counted."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 24) label = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Name") },
                    placeholder = { Text("Coffee, Gym, Rent…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(TxType.EXPENSE, TxType.INCOME).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            if (option == TxType.INCOME) bucket = null
                            else if (bucket == null) bucket = Bucket.WANTS
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        enabled = editing == null
                    ) {
                        Text(
                            text = if (option == TxType.EXPENSE) "Expense" else "Income",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Income has no bucket: it's not part of the 50/30/20 split.
            if (expenseType) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Counts as",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Used by the 50:30:20 plan to decide which pot this spending comes from.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Bucket.entries.forEach { option ->
                        BucketOption(
                            bucket = option,
                            selected = bucket == option,
                            onClick = { bucket = option }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                emojiChoices.forEach { option ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                if (emoji == option) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { emoji = option },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = option, fontSize = 19.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Colour",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { option ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(option)
                            .border(
                                width = if (color == option) 2.5.dp else 0.dp,
                                color = if (color == option) MaterialTheme.colorScheme.onSurface
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { color = option }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val trimmed = label.trim()
                    val key = editing?.key ?: newKey(trimmed)
                    onSave(
                        Category(
                            key = key,
                            label = trimmed,
                            emoji = emoji,
                            color = color,
                            bucket = if (expenseType) bucket ?: Bucket.WANTS else null,
                            type = type,
                            custom = editing?.custom ?: true,
                            hidden = false
                        )
                    )
                },
                enabled = validLabel,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (editing == null) "Create category" else "Save category",
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
                        text = if (isBuiltIn) "Hide this category" else "Delete category",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && editing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(if (isBuiltIn) "Hide ${editing.label}?" else "Delete ${editing.label}?")
            },
            text = {
                Text(
                    when {
                        isBuiltIn -> "It disappears from the picker. Anything already logged " +
                            "stays exactly as it is."
                        usageCount > 0 -> "$usageCount transaction" +
                            (if (usageCount == 1) " is" else "s are") +
                            " filed under this category. They keep their history and label, " +
                            "but you won't be able to pick it again."
                        else -> "Nothing is filed under it yet."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(editing)
                    }
                ) {
                    Text(
                        text = if (isBuiltIn) "Hide" else "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BucketOption(bucket: Bucket, selected: Boolean, onClick: () -> Unit) {
    val accent = when (bucket) {
        Bucket.NEEDS -> Color(0xFF3B82F6)
        Bucket.WANTS -> Color(0xFFEC4899)
        Bucket.SAVINGS -> Color(0xFF22C55E)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) accent.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = bucket.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${bucket.share}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = bucket.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                textAlign = TextAlign.End
            )
        }
    }
}

/** Stable key from the label, unique among the current catalogue. */
private fun newKey(label: String): String {
    val base = label.lowercase()
        .map { if (it.isLetterOrDigit()) it else '_' }
        .joinToString("")
        .trim('_')
        .ifBlank { "category" }
        .take(24)
    var candidate = base
    var suffix = 2
    while (Categories.existing(candidate) != null) {
        candidate = "${base.take(20)}_$suffix"
        suffix++
    }
    return candidate
}