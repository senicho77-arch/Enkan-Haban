package com.example.enkan_haban

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enkan_haban.data.itemCount
import com.example.enkan_haban.ui.account.AccountScreen
import com.example.enkan_haban.ui.cart.CartScreen
import com.example.enkan_haban.ui.home.HomeScreen
import com.example.enkan_haban.ui.navigation.Navigator
import com.example.enkan_haban.ui.navigation.Route
import com.example.enkan_haban.ui.product.ProductDetailScreen
import com.example.enkan_haban.ui.sell.SellScreen
import com.example.enkan_haban.ui.theme.EnkanHabanTheme
import com.example.enkan_haban.ui.theme.Navy

private data class TabItem(val tab: Route.Tab, val label: String, val emoji: String)

private val tabs = listOf(
    TabItem(Route.Home, "ገዛ", "🏠"),
    TabItem(Route.Sell, "ሸይጥ", "➕"),
    TabItem(Route.Cart, "ዘንቢል", "🛒"),
    TabItem(Route.Account, "ኣካውንት", "👤"),
)

@Composable
@Preview
fun App(navigator: Navigator = AppContainer.navigator) {
    EnkanHabanTheme {
        val cartItems by AppContainer.cartRepository.items.collectAsStateWithLifecycle()
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.shadow(16.dp, ambientColor = Navy.copy(alpha = 0.08f), spotColor = Navy.copy(alpha = 0.12f)),
                ) {
                    tabs.forEach { item ->
                        NavigationBarItem(
                            selected = navigator.currentTab == item.tab,
                            onClick = { navigator.selectTab(item.tab) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                            icon = {
                                if (item.tab == Route.Cart && cartItems.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${cartItems.itemCount}") } }) { Text(item.emoji) }
                                } else {
                                    Text(item.emoji)
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                when (val route = navigator.current) {
                    Route.Home -> HomeScreen(
                        onProductClick = { navigator.navigate(Route.ProductDetail(it)) },
                        onSellClick = { navigator.selectTab(Route.Sell) },
                    )
                    Route.Sell -> SellScreen(onPosted = { navigator.navigate(Route.ProductDetail(it)) })
                    Route.Cart -> CartScreen(
                        onProductClick = { navigator.navigate(Route.ProductDetail(it)) },
                        onContinueShopping = { navigator.selectTab(Route.Home) },
                    )
                    Route.Account -> AccountScreen()
                    is Route.ProductDetail -> ProductDetailScreen(
                        productId = route.productId,
                        onBack = { navigator.goBack() },
                        onGoToCart = { navigator.selectTab(Route.Cart) },
                    )
                }
            }
        }
    }
}
