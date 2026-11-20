package com.example.enkan_haban.data.supabase

import com.example.enkan_haban.data.PromoRepository
import com.example.enkan_haban.data.moveBanner
import com.example.enkan_haban.domain.PromoBanner
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Banners in public.promo_banners; pictures in listing-images/promos/<id>.jpg.
 * The database lets only admins write (RLS), so a normal user's save fails there too.
 */
class SupabasePromoRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : PromoRepository {
    private val _banners = MutableStateFlow<List<PromoBanner>>(emptyList())
    override val banners: StateFlow<List<PromoBanner>> = _banners.asStateFlow()
    private val bucket get() = client.storage.from(SupabaseConfig.IMAGES_BUCKET)

    init {
        scope.launch { runCatching { refresh() } }
    }

    override suspend fun refresh() {
        _banners.value = client.from("promo_banners").select().decodeList<PromoRow>()
            .sortedBy { it.sortOrder }
            .map { it.toBanner() }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun save(banner: PromoBanner, newImage: ByteArray?): PromoBanner {
        val id = banner.id.ifBlank { Uuid.random().toString() }
        val current = _banners.value.firstOrNull { it.id == id }
        var imagePath = current?.let { imagePathOf(it) }
        if (newImage != null) {
            imagePath = "promos/$id.jpg"
            bucket.upload(imagePath, newImage) {
                upsert = true
                contentType = ContentType.Image.JPEG
            }
        }
        val sortOrder = if (current == null) (_banners.value.maxOfOrNull { it.sortOrder } ?: 0) + 1 else banner.sortOrder
        val row = buildJsonObject {
            put("id", id)
            if (imagePath == null) put("image_path", JsonNull) else put("image_path", imagePath)
            put("tag", banner.tag.trim())
            put("title", banner.title.trim())
            put("subtitle", banner.subtitle.trim())
            put("cta", banner.cta.trim())
            put("emoji", banner.emoji.trim())
            put("color_start", banner.colorStart)
            put("color_end", banner.colorEnd)
            putNullable("target_category_id", banner.targetCategoryId)
            putNullable("target_listing_id", banner.targetProductId)
            put("sort_order", sortOrder)
            put("is_active", banner.isActive)
            putNullable("starts_at", banner.startsAtMs?.let(::iso))
            putNullable("ends_at", banner.endsAtMs?.let(::iso))
        }
        client.from("promo_banners").upsert(JsonArray(listOf(row)))
        refresh()
        return _banners.value.firstOrNull { it.id == id } ?: banner.copy(id = id)
    }

    override suspend fun delete(id: String) {
        val path = _banners.value.firstOrNull { it.id == id }?.let { imagePathOf(it) }
        client.from("promo_banners").delete { filter { eq("id", id) } }
        if (path != null) runCatching { bucket.delete(path) }
        refresh()
    }

    override suspend fun move(id: String, up: Boolean) {
        val before = _banners.value.associate { it.id to it.sortOrder }
        moveBanner(_banners.value, id, up).filter { before[it.id] != it.sortOrder }.forEach { b ->
            client.from("promo_banners").update({ set("sort_order", b.sortOrder) }) { filter { eq("id", b.id) } }
        }
        refresh()
    }

    private fun PromoRow.toBanner() = PromoBanner(
        id = id,
        // ?v= makes phones load the new picture after the admin replaces it.
        imageUrl = imagePath?.let { bucket.publicUrl(it) + "?v=" + (updatedAt ?: "") },
        tag = tag, title = title, subtitle = subtitle, cta = cta, emoji = emoji,
        colorStart = colorStart, colorEnd = colorEnd,
        targetCategoryId = targetCategoryId, targetProductId = targetListingId,
        sortOrder = sortOrder, isActive = isActive,
        startsAtMs = startsAt?.let(::parseEpochMs), endsAtMs = endsAt?.let(::parseEpochMs),
    )

    private fun imagePathOf(banner: PromoBanner): String? =
        if (banner.imageUrl == null) null else "promos/${banner.id}.jpg"
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

@OptIn(ExperimentalTime::class)
private fun iso(epochMs: Long): String = Instant.fromEpochMilliseconds(epochMs).toString()
