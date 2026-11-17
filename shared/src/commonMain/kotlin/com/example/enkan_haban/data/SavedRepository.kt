package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Products a buyer picked and keeps in the ዘንቢል tab to contact the seller later.
 * No checkout: every product is bought by calling the seller. Maps to `public.favorites` later.
 */
class SavedRepository {
    private val _items = MutableStateFlow<List<Product>>(emptyList())

    /** Newest saved first. */
    val items: StateFlow<List<Product>> = _items.asStateFlow()

    fun contains(productId: String): Boolean = _items.value.any { it.id == productId }

    /** Saves [product], or takes it out again when it is already saved. */
    fun toggle(product: Product) {
        _items.update { items ->
            if (items.any { it.id == product.id }) items.filterNot { it.id == product.id } else listOf(product) + items
        }
    }

    fun remove(productId: String) {
        _items.update { items -> items.filterNot { it.id == productId } }
    }
}
