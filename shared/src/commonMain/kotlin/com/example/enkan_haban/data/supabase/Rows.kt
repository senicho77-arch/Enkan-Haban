package com.example.enkan_haban.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* Rows as they come from the database (see supabase/migrations). Only the columns the app reads. */

@Serializable
internal data class CategoryRow(
    val id: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("name_ti") val nameTi: String,
    @SerialName("name_en") val nameEn: String,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
internal data class ListingRow(
    val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("listing_type") val listingType: String,
    val title: String,
    val description: String = "",
    val condition: String = "new",
    @SerialName("price_minor") val priceMinor: Long,
    @SerialName("old_price_minor") val oldPriceMinor: Long? = null,
    val currency: String,
    val city: String,
    val area: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
internal data class ProfileCardRow(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
)

@Serializable
internal data class MyProfileRow(
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
)

@Serializable
internal data class ListingImageRow(
    @SerialName("listing_id") val listingId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
internal data class PlacementRow(
    @SerialName("category_id") val categoryId: String,
    @SerialName("listing_id") val listingId: String,
    val position: Int,
    @SerialName("pinned_until") val pinnedUntil: String? = null,
)
