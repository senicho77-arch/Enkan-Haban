package com.example.enkan_haban.data

import com.example.enkan_haban.domain.CartItem
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    /** Adds one unit of [product]. Classified ads can't be bought through the cart. */
    fun add(product: Product) {
        require(product.listingType == ListingType.STORE) { "Only store products can be added to the cart" }
        _items.update { items ->
            if (items.any { it.product.id == product.id }) {
                items.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                items + CartItem(product, 1)
            }
        }
    }

    /** Sets the quantity of a line; a quantity of 0 or less removes it. */
    fun setQuantity(productId: String, quantity: Int) {
        _items.update { items ->
            if (quantity <= 0) items.filterNot { it.product.id == productId }
            else items.map { if (it.product.id == productId) it.copy(quantity = quantity) else it }
        }
    }

    fun remove(productId: String) = setQuantity(productId, 0)

    fun clear() {
        _items.value = emptyList()
    }
}

val List<CartItem>.subtotal: Long get() = sumOf { it.total }
val List<CartItem>.itemCount: Int get() = sumOf { it.quantity }
