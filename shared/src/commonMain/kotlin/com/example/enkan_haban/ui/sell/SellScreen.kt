package com.example.enkan_haban.ui.sell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.domain.Photo
import com.example.enkan_haban.ui.components.CurrencyToggle
import com.example.enkan_haban.ui.components.PhotoImage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.CurrencySettings

/**
 * Form for posting a new ad, or — with [editProductId] — for changing the price, photos,
 * text and other details of the seller's own ad. [onDone] gets the product id after saving.
 */
@Composable
fun SellScreen(
    onDone: (String) -> Unit,
    editProductId: String? = null,
    /** Keeps the edit form apart from the new-ad form (and from earlier edits). */
    formKey: String = "new",
    onBack: (() -> Unit)? = null,
    viewModel: SellViewModel = viewModel(key = formKey) {
        SellViewModel(AppContainer.productRepository, AppContainer.currentUser, editProductId)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openPicker = rememberPhotoPicker(onPicked = viewModel::addPhotos)
    val isEditing = editProductId != null

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = if (isEditing) "ኣቕሓ ኣመሓይሽ" else "ኣቕሓ ንሽያጥ ኣእትው", onBack = onBack)
        val loadError = state.loadError
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            isEditing && state.editing == null && loadError != null ->
                Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) { Text(loadError) }
            else -> SellFormContent(state, viewModel, isEditing, openPicker, onDone)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SellFormContent(
    state: SellUiState,
    viewModel: SellViewModel,
    isEditing: Boolean,
    openPicker: (maxItems: Int) -> Unit,
    onDone: (String) -> Unit,
) {
    val form = state.form
    val errors = state.errors
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("ፎቶታት (${form.photos.size}/$MAX_PHOTOS)", errors["photos"])
        PhotoStrip(
            photos = form.photos,
            canAddMore = state.photoSlotsLeft > 0,
            onAdd = { openPicker(state.photoSlotsLeft) },
            onRemove = viewModel::removePhoto,
        )
        state.photoMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        FormField("ኣርእስቲ", form.title, errors["title"]) { v -> viewModel.update { it.copy(title = v) } }
        FormField("መግለጺ", form.description, errors["description"], singleLine = false) { v ->
            viewModel.update { it.copy(description = v) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ገንዘብ ዋጋ", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            CurrencyToggle()
        }
        FormField("ዋጋ (${CurrencySettings.current.code})", form.price, errors["price"], keyboardType = KeyboardType.Decimal) { v ->
            viewModel.update { it.copy(price = v) }
        }

        SectionLabel("ዓይነት", errors["category"])
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.categories.forEach { category ->
                FilterChip(
                    selected = form.categoryId == category.id,
                    onClick = { viewModel.update { it.copy(categoryId = category.id) } },
                    label = { Text("${category.emoji} ${category.localName}") },
                )
            }
        }

        SectionLabel("ኩነታት", null)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Condition.entries.forEach { condition ->
                FilterChip(
                    selected = form.condition == condition,
                    onClick = { viewModel.update { it.copy(condition = condition) } },
                    label = { Text(condition.label) },
                )
            }
        }

        FormField("ቦታ (ከተማ፡ ከባቢ)", form.location, errors["location"]) { v -> viewModel.update { it.copy(location = v) } }
        FormField("ቁጽሪ ተሌፎን", form.phone, errors["phone"], keyboardType = KeyboardType.Phone) { v ->
            viewModel.update { it.copy(phone = v) }
        }

        state.loadError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { viewModel.submit(onDone) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(if (isEditing) "ለውጢ ዓቅብ" else "ኣቕሓ ለጥፍ") }
    }
}

/** Picked photos in a scrollable row, with an "add" tile until [MAX_PHOTOS] is reached. */
@Composable
private fun PhotoStrip(
    photos: List<Photo>,
    canAddMore: Boolean,
    onAdd: () -> Unit,
    onRemove: (Photo) -> Unit,
) {
    val tileShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canAddMore) {
            Box(
                modifier = Modifier.size(88.dp).clip(tileShape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), tileShape)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", style = MaterialTheme.typography.titleLarge)
                    Text("ፎቶ ወስኽ", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        photos.forEachIndexed { index, photo ->
            Box(Modifier.size(88.dp)) {
                PhotoImage(photo, Modifier.fillMaxSize().clip(tileShape))
                if (index == 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 10.dp),
                        modifier = Modifier.align(Alignment.BottomStart),
                    ) {
                        Text(
                            "ቀዳማይ",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                        .clip(CircleShape).clickable { onRemove(photo) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, error: String?) {
    Column {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: String?,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
