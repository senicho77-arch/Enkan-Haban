package com.example.enkan_haban.data.supabase

import com.example.enkan_haban.data.AdminRepository
import com.example.enkan_haban.data.AuthRepository
import com.example.enkan_haban.domain.Placement
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Admin listing order through the admin_* functions in the database (they refuse non-admins).
 * [isAdmin] comes from rpc("is_admin") each time the signed-in user changes.
 */
class SupabaseAdminRepository(
    private val client: SupabaseClient,
    auth: AuthRepository,
    private val scope: CoroutineScope,
    /** Called after a change so the feed shows the new order. */
    private val onOrderChanged: suspend () -> Unit = {},
) : AdminRepository {
    private val _isAdmin = MutableStateFlow(false)
    override val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val placements = MutableStateFlow<List<Placement>>(emptyList())
    override fun observePlacements(): StateFlow<List<Placement>> = placements.asStateFlow()

    init {
        scope.launch {
            auth.currentUser.collect { user ->
                _isAdmin.value = user != null &&
                    runCatching { client.postgrest.rpc("is_admin").decodeAs<Boolean>() }.getOrDefault(false)
            }
        }
        scope.launch { reload() }
    }

    /** Placements are public (the feed uses them); the database ignores expired or stale ones. */
    suspend fun reload() {
        runCatching {
            placements.value = client.from("category_placements").select().decodeList<PlacementRow>().map {
                Placement(it.categoryId, it.listingId, it.position, it.pinnedUntil?.let(::parseEpochMs))
            }
        }
    }

    override suspend fun setOrder(categoryKey: String, productIds: List<String>, pinnedUntil: Map<String, Long>?) {
        client.postgrest.rpc("admin_set_category_order", buildJsonObject {
            put("p_category_id", categoryKey)
            putJsonArray("p_listing_ids") { productIds.forEach { add(JsonPrimitive(it)) } }
        })
        // End dates are set per listing (admin_set_category_order keeps the old ones).
        if (pinnedUntil != null) {
            val old = placements.value.filter { it.categoryKey == categoryKey }.associate { it.productId to it.pinnedUntilEpochMs }
            productIds.forEachIndexed { index, id ->
                if (old[id] != pinnedUntil[id]) pinAt(categoryKey, id, index + 1, pinnedUntil[id])
            }
        }
        afterChange()
    }

    override suspend fun pin(categoryKey: String, productId: String, position: Int, pinnedUntil: Long?) {
        pinAt(categoryKey, productId, position, pinnedUntil)
        afterChange()
    }

    override suspend fun unpin(categoryKey: String, productId: String) {
        client.postgrest.rpc("admin_unpin_listing", buildJsonObject {
            put("p_category_id", categoryKey)
            put("p_listing_id", productId)
        })
        afterChange()
    }

    private suspend fun pinAt(categoryKey: String, productId: String, position: Int, pinnedUntil: Long?) {
        client.postgrest.rpc("admin_pin_listing", buildJsonObject {
            put("p_category_id", categoryKey)
            put("p_listing_id", productId)
            put("p_position", position)
            if (pinnedUntil == null) put("p_pinned_until", JsonNull) else put("p_pinned_until", isoTimestamp(pinnedUntil))
        })
    }

    private suspend fun afterChange() {
        reload()
        onOrderChanged()
    }
}

@OptIn(ExperimentalTime::class)
private fun isoTimestamp(epochMs: Long): String = Instant.fromEpochMilliseconds(epochMs).toString()
