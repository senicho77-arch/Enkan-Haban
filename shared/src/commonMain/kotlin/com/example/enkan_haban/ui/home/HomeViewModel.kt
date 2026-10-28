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
    /** Discounted products for the "today's deals" row, independent of filters. */
    val deals: List<Product> = emptyList(),
) {
    /** Banners and deal rows only show on the plain home feed, not while filtering. */
    val isBrowsing: Boolean get() = query.isBlank() && selectedCategoryId == null
}

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
                deals = products.filter { it.discountPercent != null }.sortedByDescending { it.discountPercent },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(categories = repository.categories))

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Shows only [categoryId] (or everything when null), e.g. from a promo banner. */
    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    /** Selecting the active category again clears the filter. */
    fun onCategoryClick(categoryId: String) {
        selectedCategoryId.value = if (selectedCategoryId.value == categoryId) null else categoryId
    }
}
