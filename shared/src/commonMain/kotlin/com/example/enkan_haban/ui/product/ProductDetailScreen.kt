package com.example.enkan_haban.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.data.MAX_MESSAGE_LENGTH
import com.example.enkan_haban.data.isValidPhone
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
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onGoToCart: () -> Unit,
    onOpenChat: (conversationId: String) -> Unit,
) {
    val state by produceState<DetailState>(DetailState.Loading, productId) {
        value = AppContainer.productRepository.getProduct(productId)?.let { DetailState.Loaded(it) } ?: DetailState.NotFound
    }

    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "ዝርዝር", onBack = onBack)
        when (val s = state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            DetailState.NotFound -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("እዚ ኣቕሓ ድሕሪ ሕጂ የለን።") }
            is DetailState.Loaded -> ProductDetailContent(s.product, onGoToCart, onOpenChat)
        }
    }
}

@Composable
private fun ProductDetailContent(product: Product, onGoToCart: () -> Unit, onOpenChat: (String) -> Unit) {
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
                            whatsappUri(product.seller.phone)?.let { link ->
                                val uriHandler = LocalUriHandler.current
                                Button(
                                    onClick = { runCatching { uriHandler.openUri(link) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) { Text("💬 WhatsApp", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
                if (product.listingType == ListingType.CLASSIFIED) ChatWithSellerCard(product, onOpenChat)
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

/** Jiji-style quick messages: tapping one fills the box, the buyer can edit before sending. */
private val quickReplies = listOf(
    "ዋጋ ኣቕርብ" to "ሰላም፡ ነዚ ኣቕሓ ብ… ክገዝኦ ደልየ።",
    "ኣሎ ድዩ?" to "ሰላም፡ እዚ ኣቕሓ ገና ኣሎ ድዩ?",
    "መወዳእታ ዋጋ" to "ሰላም፡ መወዳእታ ዋጋኹም ክንደይ እዩ?",
)

private val ChatOrange = Color(0xFFF7931E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatWithSellerCard(product: Product, onOpenChat: (String) -> Unit) {
    var message by remember(product.id) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ምስ ሸያጢ ተዘራረብ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickReplies.forEach { (label, text) ->
                    SuggestionChip(onClick = { message = text }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) message = it },
                placeholder = { Text("መልእኽትኻ ኣብዚ ጽሓፍ") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val id = AppContainer.chatRepository.startChat(product, message)
                    message = ""
                    onOpenChat(id)
                },
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ChatOrange, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ዕላል ጀምር", fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Bottom bar of a classified ad, like Jiji: "request call back" next to a green button that
 * shows the seller's number and opens the dialer in one tap.
 */
@Composable
private fun ClassifiedActions(product: Product) {
    val uriHandler = LocalUriHandler.current
    val phone = product.seller.phone
    val requests by AppContainer.chatRepository.callbackRequests.collectAsStateWithLifecycle()
    val requested = requests.any { it.productId == product.id }
    var showCallbackDialog by remember { mutableStateOf(false) }
    val compact = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { showCallbackDialog = true },
            enabled = !requested,
            contentPadding = compact,
            modifier = Modifier.weight(1f),
        ) { Text(if (requested) "✔ ተሓቲቱ" else "ደውለለይ", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (phone.isNotBlank()) {
            Button(
                onClick = { runCatching { uriHandler.openUri(telUri(phone)) } },
                colors = ButtonDefaults.buttonColors(containerColor = CallGreen, contentColor = Color.White),
                contentPadding = compact,
                modifier = Modifier.weight(1.5f),
            ) {
                Text(
                    "📞 $phone",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showCallbackDialog) {
        CallbackDialog(
            onDismiss = { showCallbackDialog = false },
            onSubmit = { name, number ->
                AppContainer.chatRepository.requestCallback(product, name, number)
                showCallbackDialog = false
            },
        )
    }
}

/** Buyer leaves a name and number; the seller sees the request and calls back. */
@Composable
private fun CallbackDialog(onDismiss: () -> Unit, onSubmit: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val nameError = if (name.isBlank()) "ስምካ ኣእቱ" else null
    val phoneError = if (!isValidPhone(phone)) "ቅኑዕ ቁጽሪ ተሌፎን ኣእቱ" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ሸያጢ ይደውለልካ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ስምካን ቁጽሪ ተሌፎንካን ግደፍ፡ ሸያጢ ክድውለልካ እዩ።", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("ስም") },
                    isError = showErrors && nameError != null,
                    supportingText = (if (showErrors) nameError else null)?.let { { Text(it) } },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 20) phone = it },
                    label = { Text("ቁጽሪ ተሌፎን") },
                    isError = showErrors && phoneError != null,
                    supportingText = (if (showErrors) phoneError else null)?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameError == null && phoneError == null) onSubmit(name, phone) else showErrors = true
            }) { Text("ስደድ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ሰርዝ") } },
    )
}
