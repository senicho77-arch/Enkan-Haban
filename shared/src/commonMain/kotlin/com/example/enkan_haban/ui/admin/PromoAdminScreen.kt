package com.example.enkan_haban.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.PromoBanner
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.ui.home.PromoBannerCard
import com.example.enkan_haban.ui.home.hexColor
import com.example.enkan_haban.ui.sell.rememberPhotoPicker
import com.example.enkan_haban.util.formatDate
import com.example.enkan_haban.util.nowEpochMs

/** Gradient presets for banners without a picture (start, end). */
private val colorPresets = listOf(
    "#3FA9E8" to "#1B5E8C",
    "#7A5AF8" to "#3B2A99",
    "#F59E3D" to "#C2410C",
    "#10B981" to "#065F46",
    "#EF4444" to "#991B1B",
    "#111827" to "#374151",
)

/** Admin only: the advert banners at the top of the home screen. */
@Composable
fun PromoAdminScreen(
    onBack: () -> Unit,
    viewModel: PromoAdminViewModel = viewModel { PromoAdminViewModel(AppContainer.promoRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val form = state.form
        if (form == null) {
            BannerList(state, viewModel, onBack)
        } else {
            BannerForm(form, state, viewModel)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun BannerList(state: PromoAdminUiState, viewModel: PromoAdminViewModel, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf<PromoBanner?>(null) }
    val now = nowEpochMs()
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "📢 ምሕደራ ማስታወቂያ", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "እዞም ማስታወቂያታት ኣብ ላዕሊ ገጽ መበገሲ ይርኣዩ። ኣድሚን ጥራይ ክልጥፎም ይኽእል።",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(onClick = viewModel::startNew, modifier = Modifier.fillMaxWidth()) { Text("➕ ሓድሽ ማስታወቂያ") }
            }
            if (state.banners.isEmpty()) {
                item { Text("ገና ማስታወቂያ የለን።", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            itemsIndexed(state.banners, key = { _, b -> b.id }) { index, banner ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth()) {
                            PromoBannerCard(banner, onClick = { viewModel.edit(banner.id) })
                            if (!banner.isLive(now)) {
                                Box(Modifier.matchParentSize().clip(MaterialTheme.shapes.large).background(Color.White.copy(alpha = 0.55f)))
                            }
                        }
                        Text(statusText(banner, now), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = banner.isActive, onCheckedChange = { viewModel.toggleActive(banner.id) })
                            Text(if (banner.isActive) "  ይርአ" else "  ተሓቢኡ", modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.move(banner.id, up = true) }, enabled = index > 0) { Text("⬆️") }
                            TextButton(onClick = { viewModel.move(banner.id, up = false) }, enabled = index < state.banners.lastIndex) { Text("⬇️") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.edit(banner.id) }, modifier = Modifier.weight(1f)) { Text("✏️ ኣመሓይሽ") }
                            OutlinedButton(onClick = { confirmDelete = banner }, modifier = Modifier.weight(1f)) { Text("🗑️ ደምስስ") }
                        }
                    }
                }
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
    }
    confirmDelete?.let { banner ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("ማስታወቂያ ይደምሰስ?") },
            text = { Text("\"${banner.title.ifBlank { "ስእሊ" }}\" ካብ ገጽ መበገሲ ክእለ እዩ።") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(banner.id)
                    confirmDelete = null
                }) { Text("ደምስስ") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("ሰርዝ") } },
        )
    }
}

private fun statusText(banner: PromoBanner, now: Long): String = when {
    !banner.isActive -> "⏸ ተሓቢኡ"
    banner.startsAtMs != null && banner.startsAtMs > now -> "🕒 ካብ ${formatDate(banner.startsAtMs)} ይጅምር"
    banner.endsAtMs != null && banner.endsAtMs <= now -> "⌛ ግዜኡ ኣኺሉ"
    banner.endsAtMs != null -> "✅ ይርአ ኣሎ · ክሳብ ${formatDate(banner.endsAtMs)}"
    else -> "✅ ይርአ ኣሎ"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BannerForm(form: PromoForm, state: PromoAdminUiState, viewModel: PromoAdminViewModel) {
    val openPicker = rememberPhotoPicker { picked -> picked.firstOrNull()?.let { viewModel.setImage(it) } }
    var pickDate by remember { mutableStateOf<String?>(null) } // "start" or "end"
    val banner = form.banner
    val errors = state.errors

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = if (banner.id.isBlank()) "➕ ሓድሽ ማስታወቂያ" else "✏️ ማስታወቂያ ኣመሓይሽ", onBack = viewModel::cancel)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ቅድመ-ርእይ (ከምዚ ይርአ)", style = MaterialTheme.typography.labelLarge)
            PromoBannerCard(form.preview, onClick = {})

            Text("ስእሊ", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { openPicker(1) }) { Text(if (form.preview.hasPicture) "🖼️ ስእሊ ቀይር" else "🖼️ ስእሊ ምረጽ") }
                if (form.preview.hasPicture) OutlinedButton(onClick = { viewModel.setImage(null) }) { Text("ስእሊ ኣልግስ") }
            }
            Text(
                "ስእሊ ብጎኒ (ገፊሕ) ይሓይሽ፡ ንኣብነት 1200×600። ብዘይ ስእሊ እውን ሕብሪ ን ኢሞጂ ተጠቒምካ ይከኣል።",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Field("ርእሲ (ክሳብ 60)", banner.title, errors["title"]) { v -> viewModel.update { it.copy(title = v) } }
            Field("መግለጺ (ክሳብ 120)", banner.subtitle, errors["subtitle"], singleLine = false) { v -> viewModel.update { it.copy(subtitle = v) } }
            Field("ንእሽቶ ምልክት (ንኣብነት 🔥 ቅናሽ)", banner.tag, errors["tag"]) { v -> viewModel.update { it.copy(tag = v) } }
            Field("ጽሑፍ መልጎም (ንኣብነት ሕጂ ግዛእ)", banner.cta, errors["cta"]) { v -> viewModel.update { it.copy(cta = v) } }
            Field("ኢሞጂ (ብዘይ ስእሊ)", banner.emoji, null) { v -> viewModel.update { it.copy(emoji = v.take(8)) } }

            Text("ሕብሪ (ብዘይ ስእሊ)", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                colorPresets.forEach { (start, end) ->
                    val selected = banner.colorStart == start && banner.colorEnd == end
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(hexColor(start), hexColor(end))))
                            .border(
                                BorderStroke(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface),
                                CircleShape,
                            )
                            .clickable { viewModel.update { it.copy(colorStart = start, colorEnd = end) } },
                    )
                }
            }

            Text("ምስ ተጠውቀ እንታይ ይኽፈት?", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = banner.targetCategoryId == null && banner.targetProductId == null,
                    onClick = { viewModel.update { it.copy(targetCategoryId = null, targetProductId = null) } },
                    label = { Text("ዋላ ሓንቲ") },
                )
                AppContainer.productRepository.categories.forEach { category ->
                    FilterChip(
                        selected = banner.targetCategoryId == category.id,
                        onClick = { viewModel.update { it.copy(targetCategoryId = category.id, targetProductId = null) } },
                        label = { Text("${category.emoji} ${category.localName}") },
                    )
                }
            }

            Text("ዕለት (ኣማራጺ)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = "start" }, modifier = Modifier.weight(1f)) {
                    Text(banner.startsAtMs?.let { "ካብ ${formatDate(it)}" } ?: "ካብ: ሕጂ")
                }
                OutlinedButton(onClick = { pickDate = "end" }, modifier = Modifier.weight(1f)) {
                    Text(banner.endsAtMs?.let { "ክሳብ ${formatDate(it)}" } ?: "ክሳብ: ወትሩ")
                }
            }
            errors["dates"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = banner.isActive, onCheckedChange = { on -> viewModel.update { it.copy(isActive = on) } })
                Text(if (banner.isActive) "  ኣብ ገጽ መበገሲ ይርአ" else "  ተሓቢኡ (ኣይርአን)")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = viewModel::cancel, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text("ሰርዝ") }
            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                Text(if (state.isSaving) "ይዕቀብ ኣሎ…" else "ዕቀብ")
            }
        }
    }

    pickDate?.let { which ->
        BannerDateDialog(
            initial = if (which == "start") banner.startsAtMs else banner.endsAtMs,
            onDismiss = { pickDate = null },
            onPicked = { dayStartMs ->
                viewModel.update {
                    if (which == "start") it.copy(startsAtMs = dayStartMs)
                    else it.copy(endsAtMs = dayStartMs?.let { ms -> ms + DAY_MS - 1 }) // until the end of that day
                }
                pickDate = null
            },
        )
    }
}

@Composable
private fun Field(label: String, value: String, error: String?, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerDateDialog(initial: Long?, onDismiss: () -> Unit, onPicked: (Long?) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(pickerState.selectedDateMillis) }, enabled = pickerState.selectedDateMillis != null) {
                Text("ሓሪ")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onPicked(null) }) { Text("ብዘይ ዕለት") }
                TextButton(onClick = onDismiss) { Text("ሰርዝ") }
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

private const val DAY_MS = 86_400_000L
