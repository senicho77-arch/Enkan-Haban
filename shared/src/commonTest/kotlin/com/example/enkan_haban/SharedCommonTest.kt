package com.example.enkan_haban

import com.example.enkan_haban.data.CartRepository
import com.example.enkan_haban.data.SampleData
import com.example.enkan_haban.data.filterProducts
import com.example.enkan_haban.data.itemCount
import com.example.enkan_haban.data.subtotal
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.ui.navigation.Navigator
import com.example.enkan_haban.ui.navigation.Route
import com.example.enkan_haban.ui.sell.SellForm
import com.example.enkan_haban.ui.sell.validate
import com.example.enkan_haban.util.Currency
import com.example.enkan_haban.util.formatPrice
import com.example.enkan_haban.util.formatTotal
import com.example.enkan_haban.util.parsePrice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun formatsDollars() {
        assertEquals("$0", formatPrice(0, Currency.USD))
        assertEquals("$1,250", formatPrice(125_000, Currency.USD))
        assertEquals("$12.05", formatPrice(1_205, Currency.USD))
        assertEquals("-$3", formatPrice(-300, Currency.USD))
    }

    @Test
    fun formatsShillingsRoundedToHundreds() {
        assertEquals("UGX 0", formatPrice(0, Currency.UGX))
        assertEquals("UGX 3,700", formatPrice(100, Currency.UGX))
        assertEquals("UGX 1,394,600", formatPrice(38_000, Currency.UGX))
    }

    @Test
    fun parsesUserInputIntoUsdCents() {
        assertEquals(125_000L, parsePrice("1,250", Currency.USD))
        assertEquals(125_050L, parsePrice(" 1250.5 ", Currency.USD))
        assertEquals(100L, parsePrice("3670", Currency.UGX))
        assertNull(parsePrice("abc", Currency.USD))
        assertNull(parsePrice("-5", Currency.UGX))
    }

    @Test
    fun cartTotalMatchesSumOfShownPrices() {
        // UGX 1,394,600 + 2 × UGX 55,100 = UGX 1,504,800 (rounding the USD total would give 1,504,700).
        assertEquals("UGX 1,504,800", formatTotal(listOf(38_000L to 1, 1_500L to 2), Currency.UGX))
        assertEquals("$410", formatTotal(listOf(38_000L to 1, 1_500L to 2), Currency.USD))
    }

    @Test
    fun shillingPriceSurvivesRoundTrip() {
        val stored = parsePrice("50,000", Currency.UGX)!!
        assertEquals("UGX 50,000", formatPrice(stored, Currency.UGX))
    }
}

class CartRepositoryTest {
    private val storeProduct = SampleData.products.first { it.listingType == ListingType.STORE }
    private val adProduct = SampleData.products.first { it.listingType == ListingType.CLASSIFIED }

    @Test
    fun addingSameProductIncreasesQuantity() {
        val cart = CartRepository()
        cart.add(storeProduct)
        cart.add(storeProduct)
        assertEquals(1, cart.items.value.size)
        assertEquals(2, cart.items.value.itemCount)
        assertEquals(storeProduct.price * 2, cart.items.value.subtotal)
    }

    @Test
    fun settingQuantityToZeroRemovesLine() {
        val cart = CartRepository()
        cart.add(storeProduct)
        cart.setQuantity(storeProduct.id, 0)
        assertTrue(cart.items.value.isEmpty())
    }

    @Test
    fun classifiedAdsCannotBeAddedToCart() {
        assertFailsWith<IllegalArgumentException> { CartRepository().add(adProduct) }
    }
}

class FilterProductsTest {
    @Test
    fun filtersByCategoryAndQuery() {
        val phones = filterProducts(SampleData.products, "", "phones")
        assertTrue(phones.isNotEmpty() && phones.all { it.categoryId == "phones" })

        val iphones = filterProducts(SampleData.products, "iphone", "phones")
        assertEquals(listOf("p2"), iphones.map { it.id })

        assertEquals(SampleData.products, filterProducts(SampleData.products, "  ", null))
    }
}

class SellValidationTest {
    @Test
    fun emptyFormReportsAllRequiredFields() {
        assertEquals(setOf("title", "description", "price", "category", "location", "phone"), validate(SellForm()).keys)
    }

    @Test
    fun completeFormIsValid() {
        val form = SellForm(
            title = "Used bicycle",
            description = "Good condition",
            price = "4500",
            categoryId = "vehicles",
            location = "Addis Ababa",
            phone = "+251 911 000 000",
        )
        assertTrue(validate(form).isEmpty())
    }
}

class NavigatorTest {
    @Test
    fun backStackKeepsHomeAsRoot() {
        val navigator = Navigator()
        navigator.selectTab(Route.Cart)
        navigator.navigate(Route.ProductDetail("p1"))
        assertEquals(Route.Cart, navigator.currentTab)
        assertTrue(navigator.goBack())
        assertEquals(Route.Cart, navigator.current)
        assertTrue(navigator.goBack())
        assertEquals(Route.Home, navigator.current)
        assertFalse(navigator.goBack())
    }
}
