package com.example.enkan_haban

import com.example.enkan_haban.data.AdminRepository
import com.example.enkan_haban.data.CartRepository
import com.example.enkan_haban.data.InMemoryAdminRepository
import com.example.enkan_haban.data.InMemoryProductRepository
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.domain.Seller
import com.example.enkan_haban.ui.navigation.Navigator

/** Simple manual dependency container. Replace with Koin or similar once the app grows. */
object AppContainer {
    /**
     * Demo/debug only: makes the current user an admin so the listing-order screen can be tried.
     * Must be false in release builds.
     * TODO: remove and use rpc("is_admin") once Google sign-in exists.
     */
    const val DEMO_USER_IS_ADMIN = true

    /** Show a small "📌" tag on products the admin placed. Off by default. */
    val showPinnedBadge = false

    val productRepository: ProductRepository by lazy { InMemoryProductRepository() }
    val adminRepository: AdminRepository by lazy { InMemoryAdminRepository(isAdmin = DEMO_USER_IS_ADMIN) }
    val cartRepository: CartRepository by lazy { CartRepository() }
    val navigator: Navigator by lazy { Navigator() }

    /** Placeholder until authentication is added. */
    val currentUser = Seller(id = "me", name = "You", phone = "")
}
