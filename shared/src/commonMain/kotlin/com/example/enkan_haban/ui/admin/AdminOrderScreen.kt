package com.example.enkan_haban.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.ALL_CATEGORIES
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.ui.components.ProductImage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.formatDate
import com.example.enkan_haban.util.formatPrice

/** Admin only: decide by hand which posts come first in each category. */
@Composable
fun AdminOrderScreen(
    onBack: () -> Unit,
    viewModel: AdminOrderViewModel = viewModel {
        AdminOrderViewModel(AppContainer.productRepository, AppContainer.adminRepository)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var datePickerFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            SimpleTopBar(title = "🛠️ ምሕደራ መስርዕ", onBack = onBack)

            CategoryPicker(state, onSelect = viewModel::selectCategory)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionTitle("📌 ዝተሰርዑ (${state.placed.size})") }
                if (state.placed.isEmpty()) {
                    item { Hint("ገና ዝተሰርዐ የለን። ካብ ታሕቲ \"➕ ናብ መስርዕ\" ጠውቕ።") }
                }
                itemsIndexed(state.placed, key = { _, row -> "placed-${row.product.id}" }) { index, row ->
                    PlacedRowCard(
                        row = row,
                        isFirst = index == 0,
                        isLast = index == state.placed.lastIndex,
                        onUp = { viewModel.moveUp(index) },
                        onDown = { viewModel.moveDown(index) },
                        onPin = { datePickerFor = index },
                        onRemove = { viewModel.remove(index) },
                    )
                }

                item { SectionTitle("🆕 ካልኦት ፖስትታት (${state.others.size})") }
                if (state.others.isEmpty()) {
                    item { Hint("ካልእ ፖስት የለን።") }
                }
                items(state.others, key = { "other-${it.id}" }) { product ->
                    ProductRowCard(product, dimmed = false) {
                        TextButton(onClick = { viewModel.addToOrder(product.id) }) { Text("➕ ናብ መስርዕ") }
                    }
                }
            }

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::discard,
                    enabled = state.hasChanges && !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) { Text("ሰርዝ") }
                Button(
                    onClick = viewModel::save,
                    enabled = state.hasChanges && !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.isSaving) "ይዕቀብ ኣሎ…" else "ዕቀብ") }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }

    datePickerFor?.let { index ->
        val row = state.placed.getOrNull(index)
        if (row != null) {
            PinUntilDialog(
                initial = row.pinnedUntilEpochMs,
                onDismiss = { datePickerFor = null },
                onPicked = { endMs ->
                    viewModel.setPinnedUntil(index, endMs)
                    datePickerFor = null
                },
            )
        }
    }
}

@Composable
private fun CategoryPicker(state: AdminOrderUiState, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = state.categoryKey == ALL_CATEGORIES,
                onClick = { onSelect(ALL_CATEGORIES) },
                label = { Text("🏠 ኩሉ (ገጽ መበገሲ)") },
            )
        }
        items(state.categories, key = { it.id }) { category ->
            FilterChip(
                selected = state.categoryKey == category.id,
                onClick = { onSelect(category.id) },
                label = { Text("${category.emoji} ${category.localName}") },
            )
        }
    }
}

@Composable
private fun PlacedRowCard(
    row: PlacedRow,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onPin: () -> Unit,
    onRemove: () -> Unit,
) {
    ProductRowCard(
        product = row.product,
        dimmed = row.isExpired,
        number = row.position,
        note = when {
            row.isExpired -> "⌛ ግዜኡ ኣኺሉ"
            row.pinnedUntilEpochMs != null -> "📌 ክሳብ ${formatDate(row.pinnedUntilEpochMs)}"
            else -> null
        },
    ) {
        TextButton(onClick = onUp, enabled = !isFirst) { Text("⬆️") }
        TextButton(onClick = onDown, enabled = !isLast) { Text("⬇️") }
        TextButton(onClick = onPin) { Text("📌") }
        TextButton(onClick = onRemove) { Text("✖") }
    }
}

/** Photo/emoji, title, price and seller, with [actions] in a row underneath (fits phone width). */
@Composable
private fun ProductRowCard(
    product: Product,
    dimmed: Boolean,
    number: Int? = null,
    note: String? = null,
    actions: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth().alpha(if (dimmed) 0.5f else 1f)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (number != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$number",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer8()
                }
                ProductImage(product, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), emojiSize = 22)
                Spacer8()
                Column(Modifier.weight(1f)) {
                    Text(
                        product.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatPrice(product.price),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "👤 ${product.seller.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    note?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinUntilDialog(initial: Long?, onDismiss: () -> Unit, onPicked: (Long?) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    // Keep the post placed until the end of the chosen day.
                    onPicked(pickerState.selectedDateMillis?.let { it + DAY_MS - 1 })
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("ሓሪ") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onPicked(null) }) { Text("ብዘይ ዕለት") }
                TextButton(onClick = onDismiss) { Text("ሰርዝ") }
            }
        },
    ) {
        DatePicker(state = pickerState, title = { Text("📌 ክሳብ መዓስ?", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Spacer8() {
    Spacer(Modifier.width(8.dp))
}

private const val DAY_MS = 86_400_000L
