package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.domain.ListingDraft
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.domain.Seller
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Source of products and categories. Swap [InMemoryProductRepository] for a network-backed one later. */
interface ProductRepository {
    val categories: List<Category>
    fun observeProducts(): Flow<List<Product>>
    suspend fun getProduct(id: String): Product?
    suspend fun createListing(draft: ListingDraft, seller: Seller): Product

    /**
     * Replaces the title, description, price, photos and other fields of ad [id] with [draft].
     * Returns the updated product, or null if it doesn't exist or [seller] may not edit it.
     */
    suspend fun updateListing(id: String, draft: ListingDraft, seller: Seller): Product?
}

class InMemoryProductRepository(
    initialProducts: List<Product> = SampleData.products,
    override val categories: List<Category> = SampleData.categories,
) : ProductRepository {

    private val products: MutableStateFlow<List<Product>> = MutableStateFlow(initialProducts)
    private var nextId = 1

    override fun observeProducts(): StateFlow<List<Product>> = products.asStateFlow()

    override suspend fun getProduct(id: String): Product? = products.value.firstOrNull { it.id == id }

    override suspend fun createListing(draft: ListingDraft, seller: Seller): Product {
        val product = Product(
            id = "user-${nextId++}",
            title = draft.title,
            description = draft.description,
            price = draft.price,
            categoryId = draft.categoryId,
            condition = draft.condition,
            location = draft.location,
            seller = seller.copy(phone = draft.phone),
            listingType = ListingType.CLASSIFIED,
            photos = draft.photos,
        )
        products.update { listOf(product) + it }
        return product
    }

    override suspend fun updateListing(id: String, draft: ListingDraft, seller: Seller): Product? {
        val existing = getProduct(id)?.takeIf { it.isEditableBy(seller) } ?: return null
        val updated = existing.copy(
            title = draft.title,
            description = draft.description,
            price = draft.price,
            categoryId = draft.categoryId,
            condition = draft.condition,
            location = draft.location,
            seller = existing.seller.copy(phone = draft.phone),
            photos = draft.photos,
        )
        products.update { list -> list.map { if (it.id == id) updated else it } }
        return updated
    }
}

/** Keeps only products matching [query] (title/description) and [categoryId], if set. */
fun filterProducts(products: List<Product>, query: String, categoryId: String?): List<Product> {
    val q = query.trim()
    return products.filter { product ->
        (categoryId == null || product.categoryId == categoryId) &&
            (q.isEmpty() || product.title.contains(q, ignoreCase = true) ||
                product.description.contains(q, ignoreCase = true))
    }
}
