package com.example.enkan_haban.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.enkan_haban.data.SampleData
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Photo
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.ui.theme.DealRed
import com.example.enkan_haban.util.formatPrice
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** The product's first photo, or a category emoji placeholder when it has none. */
@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier, emojiSize: Int = 48) {
    product.photos.firstOrNull()?.let { photo ->
        PhotoImage(photo, modifier)
        return
    }
    val emoji = SampleData.categories.firstOrNull { it.id == product.categoryId }?.emoji ?: "📦"
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = emojiSize.sp)
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            ProductImage(product, Modifier.fillMaxWidth().aspectRatio(1f))
            product.discountPercent?.let { DiscountBadge(it, Modifier.padding(8.dp)) }
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                product.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )
            Text(formatPrice(product.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            product.oldPrice?.let {
                Text(
                    formatPrice(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListingTypeTag(product.listingType)
                Text(
                    "  ${product.location}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = DealRed, shape = RoundedCornerShape(6.dp)) {
        Text(
            "-$percent%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun ListingTypeTag(type: ListingType) {
    val (label, color) = when (type) {
        ListingType.STORE -> "Store" to MaterialTheme.colorScheme.primary
        ListingType.CLASSIFIED -> "Ad" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** App bar without icon dependencies; [onBack] shows a back button when set. */
@Composable
fun SimpleTopBar(title: String, onBack: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            } else {
                Box(Modifier.padding(start = 12.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Shows a photo picked on the device, cropped to fill [modifier]'s bounds. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun PhotoImage(photo: Photo, modifier: Modifier = Modifier) {
    val bitmap = remember(photo) { runCatching { photo.bytes.decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
