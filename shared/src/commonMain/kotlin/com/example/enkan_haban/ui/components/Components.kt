package com.example.enkan_haban.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.enkan_haban.data.SampleData
import com.example.enkan_haban.domain.ListingType
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.ui.theme.DealRed
import com.example.enkan_haban.ui.theme.Gold
import com.example.enkan_haban.ui.theme.HeaderGradient
import com.example.enkan_haban.ui.theme.Navy
import com.example.enkan_haban.ui.theme.categoryColors
import com.example.enkan_haban.util.Currency
import com.example.enkan_haban.util.CurrencySettings
import com.example.enkan_haban.util.formatPrice

/** Soft drop shadow tinted with the brand navy, used on cards and the search bar. */
fun Modifier.softShadow(elevation: Dp = 10.dp, shape: RoundedCornerShape = RoundedCornerShape(20.dp)): Modifier =
    shadow(elevation, shape, clip = false, ambientColor = Navy.copy(alpha = 0.10f), spotColor = Navy.copy(alpha = 0.14f))

/**
 * Product picture. Until real photos come from the backend, shows the category emoji
 * on that category's soft gradient so every card still looks colorful and alive.
 */
@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier, emojiSize: Int = 52) {
    val emoji = SampleData.categories.firstOrNull { it.id == product.categoryId }?.emoji ?: "📦"
    val colors = categoryColors(product.categoryId)
    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        // Soft white halo behind the emoji gives a "product on a pedestal" look.
        Box(
            Modifier
                .size((emojiSize * 1.9f).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.45f)),
        )
        Text(emoji, fontSize = emojiSize.sp)
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    var liked by remember(product.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(8.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        Box {
            ProductImage(product, Modifier.fillMaxWidth().aspectRatio(1f))
            product.discountPercent?.let { DiscountBadge(it, Modifier.align(Alignment.TopStart).padding(10.dp)) }
            Surface(
                onClick = { liked = !liked },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (liked) "❤️" else "🤍", fontSize = 15.sp)
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                product.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp),
            )
            product.rating?.let { RatingRow(it) }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatPrice(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                product.oldPrice?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatPrice(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListingTypeTag(product.listingType)
                Text(
                    "  📍 ${product.location}",
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
fun RatingRow(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("★", color = Gold, fontSize = 13.sp)
        Text(
            " ${(rating * 10).toInt() / 10.0}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = DealRed, shape = RoundedCornerShape(50)) {
        Text(
            "-$percent%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun ListingTypeTag(type: ListingType) {
    val (label, color) = when (type) {
        ListingType.STORE -> "🏪 ዱኳን" to MaterialTheme.colorScheme.primary
        ListingType.CLASSIFIED -> "📢 መዋዓውዒ" to Color(0xFF7A5AF8)
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

/** Section title with an optional "see all" action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Gradient app bar matching the home header; [onBack] shows a round back button when set. */
@Composable
fun SimpleTopBar(title: String, onBack: (() -> Unit)? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(HeaderGradient),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(40.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("←", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Two-way currency switch: Uganda shilling or US dollar. Every price in the app
 * follows the choice immediately. [onDark] styles it for the gradient header.
 */
@Composable
fun CurrencyToggle(modifier: Modifier = Modifier, onDark: Boolean = false) {
    val track = if (onDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    val idleText = if (onDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier.clip(RoundedCornerShape(50)).background(track).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Currency.entries.forEach { currency ->
            val selected = CurrencySettings.current == currency
            Text(
                if (currency == Currency.USD) "$ USD" else "UGX",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (selected) Navy else idleText,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { CurrencySettings.current = currency }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
