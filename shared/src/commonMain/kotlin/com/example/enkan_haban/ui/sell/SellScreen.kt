package com.example.enkan_haban.ui.sell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.CURRENCY_CODE

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellScreen(
    onPosted: (String) -> Unit,
    viewModel: SellViewModel = viewModel { SellViewModel(AppContainer.productRepository, AppContainer.currentUser) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.form
    val errors = state.errors

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "Post an ad")
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField("Title", form.title, errors["title"]) { v -> viewModel.update { it.copy(title = v) } }
            FormField("Description", form.description, errors["description"], singleLine = false) { v ->
                viewModel.update { it.copy(description = v) }
            }
            FormField("Price ($CURRENCY_CODE)", form.price, errors["price"], keyboardType = KeyboardType.Decimal) { v ->
                viewModel.update { it.copy(price = v) }
            }

            SectionLabel("Category", errors["category"])
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.categories.forEach { category ->
                    FilterChip(
                        selected = form.categoryId == category.id,
                        onClick = { viewModel.update { it.copy(categoryId = category.id) } },
                        label = { Text("${category.emoji} ${category.name}") },
                    )
                }
            }

            SectionLabel("Condition", null)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Condition.entries.forEach { condition ->
                    FilterChip(
                        selected = form.condition == condition,
                        onClick = { viewModel.update { it.copy(condition = condition) } },
                        label = { Text(condition.label) },
                    )
                }
            }

            FormField("Location (city, area)", form.location, errors["location"]) { v -> viewModel.update { it.copy(location = v) } }
            FormField("Phone number", form.phone, errors["phone"], keyboardType = KeyboardType.Phone) { v ->
                viewModel.update { it.copy(phone = v) }
            }

            Button(
                onClick = { viewModel.submit(onPosted) },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Post ad") }
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
