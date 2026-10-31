package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Placement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Admin-only control of the listing order per category (see docs/ADMIN_LISTING_ORDER.md).
 * A Supabase implementation maps these to the `admin_*` RPCs and `rpc("is_admin")`.
 */
interface AdminRepository {
    /** True only for admins; hides every admin entry point in the app otherwise. */
    val isAdmin: StateFlow<Boolean>

    fun observePlacements(): Flow<List<Placement>>

    /**
     * Replaces the whole order of [categoryKey] with [productIds] (position = index + 1).
     * [pinnedUntil] gives each product's end date (missing key = no end date); when null the
     * end dates of products that were already placed are kept (like `admin_set_category_order`).
     * An empty list clears the order.
     */
    suspend fun setOrder(categoryKey: String, productIds: List<String>, pinnedUntil: Map<String, Long>? = null)

    /** Inserts or moves one product to [position]; the others shift down. */
    suspend fun pin(categoryKey: String, productId: String, position: Int = 1, pinnedUntil: Long? = null)

    /** Removes one product from the order and closes the gap. */
    suspend fun unpin(categoryKey: String, productId: String)
}

class InMemoryAdminRepository(
    isAdmin: Boolean,
    initialPlacements: List<Placement> = emptyList(),
) : AdminRepository {
    override val isAdmin: StateFlow<Boolean> = MutableStateFlow(isAdmin).asStateFlow()

    private val placements = MutableStateFlow(initialPlacements)

    override fun observePlacements(): StateFlow<List<Placement>> = placements.asStateFlow()

    override suspend fun setOrder(categoryKey: String, productIds: List<String>, pinnedUntil: Map<String, Long>?) {
        requireAdmin()
        require(productIds.toSet().size == productIds.size) { "duplicate listing in order" }
        placements.update { all ->
            val old = all.filter { it.categoryKey == categoryKey }.associate { it.productId to it.pinnedUntilEpochMs }
            all.filter { it.categoryKey != categoryKey } + productIds.mapIndexed { index, id ->
                Placement(categoryKey, id, index + 1, if (pinnedUntil == null) old[id] else pinnedUntil[id])
            }
        }
    }

    override suspend fun pin(categoryKey: String, productId: String, position: Int, pinnedUntil: Long?) {
        requireAdmin()
        placements.update { all ->
            val ids = all.filter { it.categoryKey == categoryKey && it.productId != productId }
                .sortedBy { it.position }.map { it.productId }.toMutableList()
            val dates = all.filter { it.categoryKey == categoryKey }.associate { it.productId to it.pinnedUntilEpochMs }
            ids.add((position - 1).coerceIn(0, ids.size), productId)
            all.filter { it.categoryKey != categoryKey } + ids.mapIndexed { index, id ->
                Placement(categoryKey, id, index + 1, if (id == productId) pinnedUntil else dates[id])
            }
        }
    }

    override suspend fun unpin(categoryKey: String, productId: String) {
        requireAdmin()
        placements.update { all ->
            val rest = all.filter { it.categoryKey == categoryKey && it.productId != productId }.sortedBy { it.position }
            all.filter { it.categoryKey != categoryKey } + rest.mapIndexed { index, p -> p.copy(position = index + 1) }
        }
    }

    private fun requireAdmin() {
        check(isAdmin.value) { "only admins can change the listing order" }
    }
}
