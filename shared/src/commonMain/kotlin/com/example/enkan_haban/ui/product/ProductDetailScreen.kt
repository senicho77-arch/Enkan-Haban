package com.example.enkan_haban.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.ui.components.DiscountBadge
import com.example.enkan_haban.ui.components.ListingTypeTag
import com.example.enkan_haban.ui.components.ProductImage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.formatPrice
import com.example.enkan_haban.util.telUri
import com.example.enkan_haban.util.whatsappUri

private val CallGreen = Color(0xFF00A63E)
private val WhatsAppGreen = Color(0xFF25D366)

private sealed interface DetailState {
    data object Loading : DetailState
    data object NotFound : DetailState
    data class Loaded(val product: Product) : DetailState
}

@Composable
fun ProductDetailScreen(productId: String, onBack: () -> Unit, onGoToCart: () -> Unit) {
    val state by produceState<DetailState>(DetailState.Loading, productId) {
        value = AppContainer.productRepository.getProduct(productId)?.let { DetailState.Loaded(it) } ?: DetailState.NotFound
    }

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "ዝርዝር", onBack = onBack)
        when (val s = state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            DetailState.NotFound -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("እዚ ኣቕሓ ድሕሪ ሕጂ የለን።") }
            is DetailState.Loaded -> ProductDetailContent(s.product, onGoToCart)
        }
    }
}

@Composable
private fun ProductDetailContent(product: Product, onGoToCart: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Box {
                ProductImage(product, Modifier.fillMaxWidth().aspectRatio(1.3f), emojiSize = 96)
                product.discountPercent?.let { DiscountBadge(it, Modifier.padding(12.dp)) }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListingTypeTag(product.listingType)
                    Text(product.condition.label, style = MaterialTheme.typography.labelMedium)
                    product.rating?.let { Text("★ $it", style = MaterialTheme.typography.labelMedium) }
                }
                Text(product.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    formatPrice(product.price),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                product.oldPrice?.let {
                    Text(formatPrice(it), textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("📍 ${product.location}", style = MaterialTheme.typography.bodyMedium)
                if (product.listingType == ListingType.CLASSIFIED) SafetyWarning()
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("መግለጺ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(product.description, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ሸያጢ", style = MaterialTheme.typography.labelMedium)
                        Text(
                            product.seller.name + if (product.seller.isVerified) "  ✔ ዝተረጋገጸ" else "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (product.listingType == ListingType.CLASSIFIED && product.seller.phone.isNotBlank()) {
                            Text(
                                "📞 ${product.seller.phone}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                when (product.listingType) {
                    ListingType.STORE -> StoreActions(product, onGoToCart)
                    ListingType.CLASSIFIED -> ClassifiedActions(product)
                }
            }
        }
    }
}

@Composable
private fun StoreActions(product: Product, onGoToCart: () -> Unit) {
    var added by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = {
                AppContainer.cartRepository.add(product)
                added = true
            },
            modifier = Modifier.weight(1f),
        ) { Text(if (added) "ካልእ ወስኽ" else "ናብ ዘንቢል ወስኽ") }
        if (added) {
            OutlinedButton(onClick = onGoToCart, modifier = Modifier.weight(1f)) { Text("ናብ ዘንቢል ኪድ") }
        }
    }
}

/** Shown on every ad, like Jiji: the most common scam is "pay first, I'll deliver". */
@Composable
private fun SafetyWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Text(
            "⚠️ ኣቕሓ ብዓይንኻ ከይረኣኻ ገንዘብ ኣቐዲምካ ኣይትኽፈል — ንመብጽሒ (delivery) እውን።",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/**
 * Buyers reach the seller directly: the number is shown on the button itself and one tap
 * opens the phone's dialer. WhatsApp appears when the number has a country code.
 */
@Composable
private fun ClassifiedActions(product: Product) {
    val uriHandler = LocalUriHandler.current
    val phone = product.seller.phone
    val whatsapp = whatsappUri(phone)
    if (phone.isBlank()) {
        Text("ሸያጢ ቁጽሪ ተሌፎን ኣየእተወን።", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { runCatching { uriHandler.openUri(telUri(phone)) } },
            colors = ButtonDefaults.buttonColors(containerColor = CallGreen, contentColor = Color.White),
            modifier = Modifier.weight(if (whatsapp != null) 1.6f else 1f),
        ) { Text("📞 $phone", fontWeight = FontWeight.Bold, maxLines = 1) }
        if (whatsapp != null) {
            Button(
                onClick = { runCatching { uriHandler.openUri(whatsapp) } },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
                modifier = Modifier.weight(1f),
            ) { Text("WhatsApp", fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}
