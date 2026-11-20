package com.example.enkan_haban.domain

/**
 * How a product is sold.
 * - [STORE]: fulfilled through the app — added to cart and checked out (Jumia-style).
 * - [CLASSIFIED]: a seller's ad — buyers contact the seller directly (Jiji-style).
 */
enum class ListingType { STORE, CLASSIFIED }

enum class Condition(val label: String) {
    NEW("ሓድሽ"),
    USED("ዝተጠቐመሉ"),
    REFURBISHED("ዝተሓደሰ"),
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

/** Prices are stored in US cents and converted for display (see util/Money.kt). */
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
    val photos: List<Photo> = emptyList(),
    /** When the ad was posted; sample data keeps 0 and relies on list order (newest first). */
    val createdAtEpochMs: Long = 0,
) {
    val discountPercent: Int?
        get() = oldPrice?.takeIf { it > price }?.let { ((it - price) * 100 / it).toInt() }

    /** Only the seller who posted a classified ad may edit it; store items are managed elsewhere. */
    fun isEditableBy(user: Seller): Boolean = listingType == ListingType.CLASSIFIED && seller.id == user.id
}

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val total: Long get() = product.price * quantity
}

/** User input for a classified ad — a new one, or new values for an existing one. */
data class ListingDraft(
    val title: String,
    val description: String,
    val price: Long,
    val categoryId: String,
    val condition: Condition,
    val location: String,
    val phone: String,
    val photos: List<Photo> = emptyList(),
)

/**
 * A photo picked on the device, kept as JPEG bytes until ads are uploaded to a backend.
 * Compared by identity, so the same picture picked twice is two separate photos.
 */
class Photo(val bytes: ByteArray)

/** One message in a buyer ↔ seller chat about a classified ad. */
data class ChatMessage(
    val id: Long,
    val body: String,
    val fromMe: Boolean,
)

/** The buyer's chat with a seller about one ad (backend: conversations + messages). */
data class Conversation(
    val id: String,
    val product: Product,
    val messages: List<ChatMessage> = emptyList(),
)

/** "Request call back": the buyer leaves a name and number for the seller to call (backend: callback_requests). */
data class CallbackRequest(
    val productId: String,
    val name: String,
    val phone: String,
)

/** Reserved category key for the home feed with no category selected. */
const val ALL_CATEGORIES = "all"

/**
 * An admin's hand-made position of [productId] inside one category ([categoryKey], or
 * [ALL_CATEGORIES]). Position 1 is shown first. After [pinnedUntilEpochMs] it is ignored.
 */
data class Placement(
    val categoryKey: String,
    val productId: String,
    val position: Int,
    val pinnedUntilEpochMs: Long? = null,
) {
    fun isExpired(nowMs: Long): Boolean = pinnedUntilEpochMs != null && pinnedUntilEpochMs <= nowMs
}

/**
 * An advert banner in the home screen carousel. Only admins create, change or delete them
 * (backend: public.promo_banners). Shown while [isActive] and inside [startsAtMs]..[endsAtMs].
 */
data class PromoBanner(
    val id: String = "",
    /** Uploaded picture (Supabase Storage); shown full-size when set. */
    val imageUrl: String? = null,
    /** Picture picked on this phone and not uploaded yet (demo mode keeps it here). */
    val localImage: Photo? = null,
    val tag: String = "",
    val title: String = "",
    val subtitle: String = "",
    val cta: String = "",
    val emoji: String = "",
    /** "#RRGGBB" gradient used when there is no picture. */
    val colorStart: String = "#3FA9E8",
    val colorEnd: String = "#1B5E8C",
    /** A tap opens this category, or this product, or nothing. */
    val targetCategoryId: String? = null,
    val targetProductId: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val startsAtMs: Long? = null,
    val endsAtMs: Long? = null,
) {
    val hasPicture: Boolean get() = imageUrl != null || localImage != null

    fun isLive(nowMs: Long): Boolean =
        isActive && (startsAtMs == null || startsAtMs <= nowMs) && (endsAtMs == null || endsAtMs > nowMs)
}
