package com.example.enkan_haban.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.data.SavedRepository
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.ui.components.ProductImage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.formatPrice
import com.example.enkan_haban.util.telUri

private val CallGreen = Color(0xFF00A63E)

/** The ዘንቢል tab: products the buyer picked, each with the seller's number to call. */
@Composable
fun SavedScreen(
    onProductClick: (String) -> Unit,
    onContinueShopping: () -> Unit,
    saved: SavedRepository = AppContainer.savedRepository,
) {
    val items by saved.items.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "ዘንቢል — ዝመረጽካዮም (${items.size})")
        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🛒", style = MaterialTheme.typography.displayMedium)
                Text("ገና ዝመረጽካዮ ንብረት የለን", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ኣብ ንብረት 🤍 ጠውቕ፡ ኣብዚ ይጸንሓልካ።",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(16.dp))
                Button(onClick = onContinueShopping) { Text("ናብ ገዛ ተመለስ") }
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { product ->
                SavedLine(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onRemove = { saved.remove(product.id) },
                )
            }
        }
    }
}

@Composable
private fun SavedLine(product: Product, onClick: () -> Unit, onRemove: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val phone = product.seller.phone
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(product, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), emojiSize = 28)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        product.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatPrice(product.price),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "👤 ${product.seller.name}" + if (product.seller.isVerified) "  ✔" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onRemove) { Text("✖") }
            }
            if (phone.isNotBlank()) {
                Button(
                    onClick = { runCatching { uriHandler.openUri(telUri(phone)) } },
                    colors = ButtonDefaults.buttonColors(containerColor = CallGreen, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("📞 $phone", fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }
    }
}
