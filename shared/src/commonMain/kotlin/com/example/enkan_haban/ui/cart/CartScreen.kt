package com.example.enkan_haban.ui.cart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.data.CartRepository
import com.example.enkan_haban.data.itemCount
import com.example.enkan_haban.data.subtotal
import com.example.enkan_haban.domain.CartItem
import com.example.enkan_haban.ui.components.ProductImage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.formatPrice
import com.example.enkan_haban.util.formatTotal

@Composable
fun CartScreen(
    onProductClick: (String) -> Unit,
    onContinueShopping: () -> Unit,
    cart: CartRepository = AppContainer.cartRepository,
) {
    val items by cart.items.collectAsStateWithLifecycle()
    var showCheckoutInfo by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "ዘንቢል (${items.itemCount})")
        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🛒", style = MaterialTheme.typography.displayMedium)
                Text("ዘንቢልካ ባዶ እዩ", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(16.dp))
                Button(onClick = onContinueShopping) { Text("ዕደጋ ጀምር") }
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.product.id }) { item ->
                CartLine(
                    item = item,
                    onClick = { onProductClick(item.product.id) },
                    onQuantityChange = { cart.setQuantity(item.product.id, it) },
                    onRemove = { cart.remove(item.product.id) },
                )
            }
        }
        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ድምር", style = MaterialTheme.typography.titleMedium)
                    Text(formatTotal(items.map { it.product.price to it.quantity }), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { showCheckoutInfo = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("ክፈል (${formatTotal(items.map { it.product.price to it.quantity })})")
                }
            }
        }
    }

    if (showCheckoutInfo) {
        AlertDialog(
            onDismissRequest = { showCheckoutInfo = false },
            confirmButton = { TextButton(onClick = { showCheckoutInfo = false }) { Text("ሕራይ") } },
            title = { Text("ክፍሊት ቀልጢፉ ይመጽእ") },
            text = { Text("ኣድራሻ ምብጻሕ፡ ክፍሊት (ንኣብነት Mobile Money ወይ ኣብ ምብጻሕ ጥረ ገንዘብ) ከምኡ’ውን ምክትታል ትእዛዝ ቀጺሉ ዝመጽእ እዩ።") },
        )
    }
}

@Composable
private fun CartLine(item: CartItem, onClick: () -> Unit, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(
                item.product,
                Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
                emojiSize = 32,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.product.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(formatPrice(item.product.price), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onQuantityChange(item.quantity - 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
                        Text("−")
                    }
                    Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { Text("${item.quantity}") }
                    OutlinedButton(onClick = { onQuantityChange(item.quantity + 1) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(36.dp)) {
                        Text("+")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRemove) { Text("ኣውጽእ") }
                }
            }
        }
    }
}
