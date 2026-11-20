package com.example.enkan_haban

import com.example.enkan_haban.data.AdminRepository
import com.example.enkan_haban.data.AuthRepository
import com.example.enkan_haban.data.ChatRepository
import com.example.enkan_haban.data.DemoAuthRepository
import com.example.enkan_haban.data.InMemoryAdminRepository
import com.example.enkan_haban.data.InMemoryChatRepository
import com.example.enkan_haban.data.InMemoryProductRepository
import com.example.enkan_haban.data.InMemoryPromoRepository
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.data.PromoRepository
import com.example.enkan_haban.data.SavedRepository
import com.example.enkan_haban.data.supabase.Supabase
import com.example.enkan_haban.data.supabase.SupabaseAdminRepository
import com.example.enkan_haban.data.supabase.SupabaseAuthRepository
import com.example.enkan_haban.data.supabase.SupabaseConfig
import com.example.enkan_haban.data.supabase.SupabaseProductRepository
import com.example.enkan_haban.data.supabase.SupabasePromoRepository
import com.example.enkan_haban.domain.Seller
import com.example.enkan_haban.ui.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Simple manual dependency container. Replace with Koin or similar once the app grows.
 *
 * With [SupabaseConfig] filled in, posts, sign-in and the admin order come from Supabase.
 * Without it, the app runs on demo data. Saved items and chat are still on the phone (1.0.4).
 */
object AppContainer {
    /** Demo mode only: makes the demo user an admin so the listing-order screen can be tried. */
    const val DEMO_USER_IS_ADMIN = true

    /** Show a small "📌" tag on products the admin placed. Off by default. */
    val showPinnedBadge = false

    val useSupabase: Boolean get() = SupabaseConfig.isConfigured

    /** Lives as long as the app; for background loading in the repositories. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val authRepository: AuthRepository by lazy {
        if (useSupabase) SupabaseAuthRepository(Supabase.client, appScope) else DemoAuthRepository()
    }
    val productRepository: ProductRepository by lazy {
        if (useSupabase) SupabaseProductRepository(Supabase.client, appScope) else InMemoryProductRepository()
    }
    val adminRepository: AdminRepository by lazy {
        if (useSupabase) {
            SupabaseAdminRepository(Supabase.client, authRepository, appScope) {
                (productRepository as? SupabaseProductRepository)?.refresh()
            }
        } else {
            InMemoryAdminRepository(isAdmin = DEMO_USER_IS_ADMIN)
        }
    }
    /** Advert banners on the home screen; only admins post them. */
    val promoRepository: PromoRepository by lazy {
        if (useSupabase) SupabasePromoRepository(Supabase.client, appScope)
        else InMemoryPromoRepository(isAdmin = { adminRepository.isAdmin.value })
    }
    val chatRepository: ChatRepository by lazy { InMemoryChatRepository() }
    /** The ዘንቢል tab: products buyers picked to contact the seller later. */
    val savedRepository: SavedRepository by lazy { SavedRepository() }
    val navigator: Navigator by lazy { Navigator() }

    /** The signed-in user, or a guest (empty id) when signed out. */
    val currentUser: Seller get() = authRepository.currentUser.value ?: GUEST

    private val GUEST = Seller(id = "", name = "", phone = "")
}
