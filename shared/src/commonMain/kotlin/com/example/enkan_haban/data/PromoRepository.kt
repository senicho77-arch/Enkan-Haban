package com.example.enkan_haban.data

import com.example.enkan_haban.domain.Photo
import com.example.enkan_haban.domain.PromoBanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Advert banners on the home screen. Everyone reads them; only admins post, edit and delete
 * (the database refuses anyone else, see migrations/20261120000000_promo_banners.sql).
 */
interface PromoRepository {
    /** All banners this user may see, in [PromoBanner.sortOrder]; admins also get hidden/expired ones. */
    val banners: StateFlow<List<PromoBanner>>

    suspend fun refresh()

    /** Creates ([PromoBanner.id] empty) or updates a banner; [newImage] (JPEG bytes) replaces the picture. */
    suspend fun save(banner: PromoBanner, newImage: ByteArray?): PromoBanner

    suspend fun delete(id: String)

    /** Swaps the banner with its neighbour above ([up]) or below. */
    suspend fun move(id: String, up: Boolean)
}

/** Banners the buyer sees right now, in order. */
fun liveBanners(all: List<PromoBanner>, nowMs: Long): List<PromoBanner> =
    all.filter { it.isLive(nowMs) }.sortedBy { it.sortOrder }

/** Swaps [id] with its neighbour and renumbers 1..n. Returns the list unchanged at the ends. */
fun moveBanner(all: List<PromoBanner>, id: String, up: Boolean): List<PromoBanner> {
    val list = all.sortedBy { it.sortOrder }.toMutableList()
    val from = list.indexOfFirst { it.id == id }
    val to = if (up) from - 1 else from + 1
    if (from < 0 || to !in list.indices) return list
    list.add(to, list.removeAt(from))
    return list.mapIndexed { i, b -> b.copy(sortOrder = i + 1) }
}

/** Demo mode: starts with the three original banners. */
class InMemoryPromoRepository(
    private val isAdmin: () -> Boolean,
    initial: List<PromoBanner> = defaultBanners,
) : PromoRepository {
    private val _banners = MutableStateFlow(initial.sortedBy { it.sortOrder })
    override val banners: StateFlow<List<PromoBanner>> = _banners.asStateFlow()
    private var nextId = 1

    override suspend fun refresh() = Unit

    override suspend fun save(banner: PromoBanner, newImage: ByteArray?): PromoBanner {
        requireAdmin()
        require(banner.title.isNotBlank() || banner.hasPicture || newImage != null) { "ርእሲ ወይ ስእሊ የድሊ" }
        val saved = banner.copy(
            id = banner.id.ifBlank { "promo-${nextId++}" },
            localImage = newImage?.let(::Photo) ?: banner.localImage,
            sortOrder = if (banner.id.isBlank()) (_banners.value.maxOfOrNull { it.sortOrder } ?: 0) + 1 else banner.sortOrder,
        )
        _banners.update { list -> (list.filterNot { it.id == saved.id } + saved).sortedBy { it.sortOrder } }
        return saved
    }

    override suspend fun delete(id: String) {
        requireAdmin()
        _banners.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun move(id: String, up: Boolean) {
        requireAdmin()
        _banners.update { moveBanner(it, id, up) }
    }

    private fun requireAdmin() = check(isAdmin()) { "ኣድሚን ጥራይ ማስታወቂያ ክለጥፍ ይኽእል" }

    companion object {
        val defaultBanners = listOf(
            PromoBanner(
                id = "promo-phones", tag = "🔥 ዓቢ ቅናሽ", title = "ክሳብ 40% ቅናሽ", subtitle = "ኣብ ተሌፎናትን ኤሌክትሮኒክስን",
                cta = "ሕጂ ግዛእ", emoji = "📱", colorStart = "#3FA9E8", colorEnd = "#1B5E8C",
                targetCategoryId = "phones", sortOrder = 1,
            ),
            PromoBanner(
                id = "promo-sell", tag = "✨ ሓድሽ", title = "ብናጻ ሸይጥ", subtitle = "ኣቕሑኻ ኣብ ሓንቲ ደቒቕ ኣእትዎ — ንሓበሻ ኣብ ዓለም",
                cta = "ሕጂ ጀምር", emoji = "🛍️", colorStart = "#7A5AF8", colorEnd = "#3B2A99", sortOrder = 2,
            ),
            PromoBanner(
                id = "promo-fashion", tag = "📢 ስፖንሰር", title = "ባህላዊ ክዳን ሓበሻ", subtitle = "ብኢድ ዝተሰርሐ ጥበብ፡ ንበዓልን ንመርዓን",
                cta = "ርአ", emoji = "👗", colorStart = "#F59E3D", colorEnd = "#C2410C",
                targetCategoryId = "fashion", sortOrder = 3,
            ),
        )
    }
}
