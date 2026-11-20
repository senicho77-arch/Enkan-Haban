package com.example.enkan_haban.data.supabase

import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.data.SampleData
import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.domain.ListingDraft
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.domain.Seller
import com.example.enkan_haban.util.minorToUsdCents
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Thrown for features that are not connected to the database yet. */
class NotConnectedYetException(message: String) : IllegalStateException(message)

/**
 * Real posts from Supabase. The feed comes from rpc("search_listings"), which already puts
 * the admin's order first and the rest newest first. Posting and editing come in 1.0.3.
 */
class SupabaseProductRepository(
    private val client: SupabaseClient,
    private val scope: CoroutineScope,
) : ProductRepository {
    // Until the first load, the built-in list (same ids as the top categories in the database).
    override var categories: List<Category> = SampleData.categories
        private set

    private val products = MutableStateFlow<List<Product>>(emptyList())

    /** Last load error in Tigrinya, or null. */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    init {
        scope.launch { refresh() }
    }

    override fun observeProducts(): StateFlow<List<Product>> = products.asStateFlow()

    override suspend fun getProduct(id: String): Product? = products.value.firstOrNull { it.id == id }

    /** Reloads categories and the feed. */
    suspend fun refresh() {
        try {
            val categoryRows = client.from("categories").select().decodeList<CategoryRow>().filter { it.isActive }
            val parentOf = categoryRows.associate { it.id to it.parentId }
            categoryRows.filter { it.parentId == null }.sortedBy { it.sortOrder }
                .map { Category(id = it.id, name = it.nameEn, emoji = it.icon ?: "📦", localName = it.nameTi) }
                .takeIf { it.isNotEmpty() }
                ?.let { categories = it }

            val listings = client.postgrest
                .rpc("search_listings", buildJsonObject { put("p_limit", 100) })
                .decodeList<ListingRow>()
            products.value = toProducts(listings, parentOf)
            _loadError.value = null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _loadError.value = "ምርኻብ ኣይተኻእለን — ኢንተርነት ፈትሽ"
            println("Enkan Haban: loading the feed failed: $e")
        }
    }

    private suspend fun toProducts(rows: List<ListingRow>, parentOf: Map<String, String?>): List<Product> {
        if (rows.isEmpty()) return emptyList()
        val sellerIds = rows.map { it.sellerId }.distinct()
        val sellers = client.from("profiles")
            .select(Columns.list("id", "full_name", "is_verified")) { filter { isIn("id", sellerIds) } }
            .decodeList<ProfileCardRow>()
            .associateBy { it.id }
        val bucket = client.storage.from(SupabaseConfig.IMAGES_BUCKET)
        val images = client.from("listing_images")
            .select(Columns.list("listing_id", "storage_path", "sort_order")) { filter { isIn("listing_id", rows.map { it.id }) } }
            .decodeList<ListingImageRow>()
            .groupBy { it.listingId }
            .mapValues { (_, list) -> list.sortedBy { it.sortOrder }.map { bucket.publicUrl(it.storagePath) } }

        return rows.mapNotNull { row ->
            // Prices are kept in US cents in the app; skip currencies the app can't show yet.
            val price = minorToUsdCents(row.priceMinor, row.currency) ?: return@mapNotNull null
            val seller = sellers[row.sellerId]
            Product(
                id = row.id,
                title = row.title,
                description = row.description,
                price = price,
                oldPrice = row.oldPriceMinor?.let { minorToUsdCents(it, row.currency) },
                // Sub-categories (phones-smartphones) show under their top category (phones).
                categoryId = parentOf[row.categoryId] ?: row.categoryId,
                condition = when (row.condition) {
                    "used" -> Condition.USED
                    "refurbished" -> Condition.REFURBISHED
                    else -> Condition.NEW
                },
                location = listOfNotNull(row.area?.takeIf { it.isNotBlank() }, row.city).joinToString(", "),
                seller = Seller(
                    id = row.sellerId,
                    name = seller?.fullName?.takeIf { it.isNotBlank() } ?: "ሸያጢ",
                    phone = row.contactPhone.orEmpty(),
                    isVerified = seller?.isVerified ?: false,
                ),
                listingType = if (row.listingType == "store") ListingType.STORE else ListingType.CLASSIFIED,
                imageUrls = images[row.id].orEmpty(),
                createdAtEpochMs = parseEpochMs(row.createdAt),
            )
        }
    }

    override suspend fun createListing(draft: ListingDraft, seller: Seller): Product =
        throw NotConnectedYetException("ኣቕሓ ምልጣፍ ኣብ ዝቕጽል ስሪት (1.0.3) ይመጽእ።")

    override suspend fun updateListing(id: String, draft: ListingDraft, seller: Seller): Product? =
        throw NotConnectedYetException("ኣቕሓ ምምሕያሽ ኣብ ዝቕጽል ስሪት (1.0.3) ይመጽእ።")
}

@OptIn(ExperimentalTime::class)
internal fun parseEpochMs(timestamp: String?): Long =
    timestamp?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() } ?: 0L
