package com.example.enkan_haban.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.enkan_haban.domain.PromoBanner
import com.example.enkan_haban.ui.components.PhotoImage
import com.example.enkan_haban.ui.components.softShadow
import com.example.enkan_haban.ui.theme.Navy

/** "#RRGGBB" → Color; falls back to the brand blue for anything else. */
fun hexColor(hex: String, fallback: Color = Color(0xFF3FA9E8)): Color =
    hex.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback

/**
 * One advert banner. With a picture, the picture fills the card (texts on a dark fade so they stay
 * readable); without one, the gradient + emoji look.
 */
@Composable
fun PromoBannerCard(banner: PromoBanner, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val colors = listOf(hexColor(banner.colorStart), hexColor(banner.colorEnd))
    Box(
        modifier
            .fillMaxWidth()
            .height(168.dp)
            .softShadow(10.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .drawBehind {
                if (!banner.hasPicture) {
                    drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.75f, center = Offset(size.width * 0.86f, size.height * 0.55f))
                    drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.35f, center = Offset(size.width * 0.55f, -size.height * 0.05f))
                }
            }
            .clickable(onClick = onClick),
    ) {
        when {
            banner.localImage != null -> PhotoImage(banner.localImage, Modifier.fillMaxSize())
            banner.imageUrl != null -> AsyncImage(
                model = banner.imageUrl,
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            banner.emoji.isNotBlank() -> Text(
                banner.emoji,
                fontSize = 74.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp),
            )
        }
        val hasText = banner.tag.isNotBlank() || banner.title.isNotBlank() || banner.subtitle.isNotBlank() || banner.cta.isNotBlank()
        if (banner.hasPicture && hasText) {
            // Dark fade on the left so white text reads on any picture.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)),
                ),
            )
        }
        if (hasText) {
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(0.66f).padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (banner.tag.isNotBlank()) {
                        Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
                            Text(
                                banner.tag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            )
                        }
                    }
                    if (banner.title.isNotBlank()) {
                        Text(banner.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1)
                    }
                    if (banner.subtitle.isNotBlank()) {
                        Text(
                            banner.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (banner.cta.isNotBlank()) {
                    Surface(onClick = onClick, color = Color.White, shape = RoundedCornerShape(50)) {
                        Text(
                            "${banner.cta}  →",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Navy,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}
