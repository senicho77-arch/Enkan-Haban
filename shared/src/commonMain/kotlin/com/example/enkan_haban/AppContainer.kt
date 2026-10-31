package com.example.enkan_haban

import com.example.enkan_haban.data.CartRepository
import com.example.enkan_haban.data.ChatRepository
import com.example.enkan_haban.data.InMemoryChatRepository
import com.example.enkan_haban.data.InMemoryProductRepository
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.domain.Seller
import com.example.enkan_haban.ui.navigation.Navigator

/** Simple manual dependency container. Replace with Koin or similar once the app grows. */
object AppContainer {
    val productRepository: ProductRepository by lazy { InMemoryProductRepository() }
    val cartRepository: CartRepository by lazy { CartRepository() }
    val chatRepository: ChatRepository by lazy { InMemoryChatRepository() }
    val navigator: Navigator by lazy { Navigator() }

    /** Placeholder until authentication is added. */
    val currentUser = Seller(id = "me", name = "You", phone = "")
}
