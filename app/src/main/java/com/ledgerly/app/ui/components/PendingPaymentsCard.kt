package com.ledgerly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledgerly.app.data.CaptureStore
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.data.TxType
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money

/**
 * Shows the amounts caught from bank/wallet notifications and offers a one-tap way to file
 * them. Category chips default to the most likely bucket for the transaction type.
 */
@Composable
fun PendingPaymentsCard(
    suggestions: List<CaptureStore.Suggestion>,
    currency: String,
    categories: List<Category>,
    onAccept: (CaptureStore.Suggestion, String) -> Unit,
    onDismiss: (String) -> Unit
) {
    if (suggestions.isEmpty()) return

    LedgerCard(contentPadding = PaddingValues(14.dp)) {
        Text(
            text = if (suggestions.size == 1) "1 payment found" else "${suggestions.size} payments found",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "From your notifications. Nothing is saved until you tap Add.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        suggestions.forEach { suggestion ->
            var selected by remember(suggestion.id) {
                mutableStateOf(defaultCategory(suggestion, categories))
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (suggestion.type == TxType.INCOME) "↓" else "↑",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Money.format(suggestion.amountCents, currency),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${suggestion.source} · ${DateFormats.fullDate(suggestion.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onDismiss(suggestion.id) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Ignore this payment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                val pool = categories.filter { it.type == suggestion.type && !it.hidden }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pool.take(4).forEach { category ->
                        AssistChip(
                            onClick = { selected = category },
                            label = {
                                Text(
                                    text = "${category.emoji} ${category.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected == category) {
                                    category.color.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        )
                    }
                }

                Button(
                    onClick = { onAccept(suggestion, selected.key) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add to ${selected.label}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Best guess: the same category the user last used for that kind of notification, else a default. */
private fun defaultCategory(
    suggestion: CaptureStore.Suggestion,
    categories: List<Category>
): Category {
    val pool = categories.filter { it.type == suggestion.type && !it.hidden }
    val text = suggestion.rawText.lowercase()
    val bySource = when {
        "qris" in text || "bayar" in text -> "food"
        "transfer" in text -> "other"
        "tagihan" in text || "bill" in text -> "bills"
        "gaji" in text || "salary" in text -> "salary"
        else -> null
    }
    return pool.firstOrNull { it.key == bySource }
        ?: pool.firstOrNull { it.key == "other" }
        ?: pool.firstOrNull()
        ?: Categories.of("other")
}