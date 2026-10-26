package com.example.enkan_haban.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.ui.components.ProductCard

@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.productRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchHeader(query = state.query, onQueryChange = viewModel::onQueryChange)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = category.id == state.selectedCategoryId,
                            onClick = { viewModel.onCategoryClick(category.id) },
                            label = { Text("${category.emoji} ${category.name}") },
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val title = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Recommended for you"
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.products.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No products found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            }
            items(state.products, key = { it.id }) { product ->
                ProductCard(product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
private fun SearchHeader(query: String, onQueryChange: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
            Text(
                "EnkanHaban",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("🔍  Search products, brands and ads") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
