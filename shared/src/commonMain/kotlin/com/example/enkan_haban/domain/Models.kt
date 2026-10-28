package com.example.enkan_haban.domain

/**
 * How a product is sold.
 * - [STORE]: fulfilled through the app — added to cart and checked out (Jumia-style).
 * - [CLASSIFIED]: a seller's ad — buyers contact the seller directly (Jiji-style).
 */
enum class ListingType { STORE, CLASSIFIED }

enum class Condition(val label: String) {
    NEW("Brand new"),
    USED("Used"),
    REFURBISHED("Refurbished"),
}

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    /** Name shown in the app (Tigrinya); falls back to [name]. */
    val localName: String = name,
)

data class Seller(
    val id: String,
    val name: String,
    val phone: String,
    val isVerified: Boolean = false,
)

/** Prices are stored in minor currency units (e.g. cents) to avoid floating-point errors. */
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Long,
    val categoryId: String,
    val condition: Condition,
    val location: String,
    val seller: Seller,
    val listingType: ListingType,
    val oldPrice: Long? = null,
    val rating: Double? = null,
    val imageUrls: List<String> = emptyList(),
) {
    val discountPercent: Int?
        get() = oldPrice?.takeIf { it > price }?.let { ((it - price) * 100 / it).toInt() }
}

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val total: Long get() = product.price * quantity
}

/** User input for a new classified ad, before it becomes a [Product]. */
data class ListingDraft(
    val title: String,
    val description: String,
    val price: Long,
    val categoryId: String,
    val condition: Condition,
    val location: String,
    val phone: String,
)
