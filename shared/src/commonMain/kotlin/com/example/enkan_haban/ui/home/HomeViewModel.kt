package com.example.enkan_haban.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.data.filterProducts
import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.domain.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val query: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
)

class HomeViewModel(repository: ProductRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeProducts(), query, selectedCategoryId) { products, q, categoryId ->
            HomeUiState(
                query = q,
                selectedCategoryId = categoryId,
                categories = repository.categories,
                products = filterProducts(products, q, categoryId),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(categories = repository.categories))

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Selecting the active category again clears the filter. */
    fun onCategoryClick(categoryId: String) {
        selectedCategoryId.value = if (selectedCategoryId.value == categoryId) null else categoryId
    }
}
