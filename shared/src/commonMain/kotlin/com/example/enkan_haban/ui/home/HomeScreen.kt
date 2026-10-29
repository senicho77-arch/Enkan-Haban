package com.example.enkan_haban.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.resources.Res
import com.example.enkan_haban.resources.enkan_haban_logo
import com.example.enkan_haban.ui.components.CurrencyToggle
import com.example.enkan_haban.ui.components.ProductCard
import com.example.enkan_haban.ui.components.SectionHeader
import com.example.enkan_haban.ui.components.softShadow
import com.example.enkan_haban.ui.theme.Gold
import com.example.enkan_haban.ui.theme.HeaderGradient
import com.example.enkan_haban.ui.theme.Navy
import com.example.enkan_haban.ui.theme.NavyDeep
import com.example.enkan_haban.ui.theme.SkyBlue
import com.example.enkan_haban.ui.theme.categoryColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onSellClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.productRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val promos = listOf(
        Promo(
            tag = "🔥 ዓቢ ቅናሽ",
            title = "ክሳብ 40% ቅናሽ",
            subtitle = "ኣብ ተሌፎናትን ኤሌክትሮኒክስን",
            cta = "ሕጂ ግዛእ",
            emoji = "📱",
            colors = listOf(Color(0xFF3FA9E8), Color(0xFF1B5E8C)),
            onClick = { viewModel.selectCategory("phones") },
        ),
        Promo(
            tag = "✨ ሓድሽ",
            title = "ብናጻ ሸይጥ",
            subtitle = "ኣቕሑኻ ኣብ ሓንቲ ደቒቕ ኣእትዎ — ንሓበሻ ኣብ ዓለም",
            cta = "ሕጂ ጀምር",
            emoji = "🛍️",
            colors = listOf(Color(0xFF7A5AF8), Color(0xFF3B2A99)),
            onClick = onSellClick,
        ),
        Promo(
            tag = "📢 ስፖንሰር · መዋዓውዒ",
            title = "ባህላዊ ክዳን ሓበሻ",
            subtitle = "ብኢድ ዝተሰርሐ ጥበብ፡ ንበዓልን ንመርዓን",
            cta = "ርአ",
            emoji = "👗",
            colors = listOf(Color(0xFFF59E3D), Color(0xFFC2410C)),
            onClick = { viewModel.selectCategory("fashion") },
        ),
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HomeHeader(query = state.query, onQueryChange = viewModel::onQueryChange)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.isBrowsing) {
                fullWidth {
                    Column {
                        Text("ሰላም 👋", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("እንታይ ክትገዝእ ትደሊ ሎሚ?", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                fullWidth { PromoCarousel(promos) }
            }

            fullWidth {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("ዓይነታት")
                    CategoryRow(
                        categories = state.categories,
                        selectedId = state.selectedCategoryId,
                        onClick = viewModel::onCategoryClick,
                        onAllClick = { viewModel.selectCategory(null) },
                    )
                }
            }

            if (state.isBrowsing && state.deals.isNotEmpty()) {
                fullWidth {
                    Column {
                        SectionHeader("⚡ ናይ ሎሚ ቅናሽ")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            items(state.deals, key = { "deal-${it.id}" }) { product ->
                                ProductCard(product, onClick = { onProductClick(product.id) }, modifier = Modifier.width(164.dp))
                            }
                        }
                    }
                }
            }

            fullWidth {
                val category = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                val title = when {
                    state.query.isNotBlank() -> "🔍 ውጽኢት ምድላይ"
                    category != null -> "${category.emoji} ${category.localName}"
                    else -> "✨ ንዓኻ ዝተሓረየ"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${state.products.size} ኣቕሑ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.products.isEmpty()) {
                fullWidth { EmptyState() }
            }

            items(state.products, key = { it.id }) { product ->
                ProductCard(product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(query: String, onQueryChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(HeaderGradient)
            .drawBehind {
                // Soft decorative bubbles for depth.
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.38f, center = Offset(size.width * 0.95f, 0f))
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.22f, center = Offset(size.width * 0.1f, size.height))
            },
    ) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.enkan_haban_logo),
                    contentDescription = "Enkan Haban logo",
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("እንካን ሃባን", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        "ENKAN HABAN · ዕዳጋ ሓበሻ",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                CurrencyToggle(onDark = true)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("ኣቕሑ፡ ዱኳን ወይ መዋዓውዒ ድለ…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Text("🔍", fontSize = 18.sp) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = NavyDeep,
                    unfocusedTextColor = NavyDeep,
                ),
                modifier = Modifier.fillMaxWidth().softShadow(12.dp, RoundedCornerShape(50)),
            )
        }
    }
}


// ── Promo banners ────────────────────────────────────────────────────────────

private data class Promo(
    val tag: String,
    val title: String,
    val subtitle: String,
    val cta: String,
    val emoji: String,
    val colors: List<Color>,
    val onClick: () -> Unit,
)

@Composable
private fun PromoCarousel(promos: List<Promo>) {
    val pagerState = rememberPagerState(pageCount = { promos.size })

    // Auto-advance every 4 seconds; restarts the timer after each page change.
    LaunchedEffect(pagerState.currentPage) {
        delay(4_000)
        pagerState.animateScrollToPage((pagerState.currentPage + 1) % promos.size)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth()) { page ->
            PromoBanner(promos[page])
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(promos.size) { index ->
                val selected = index == pagerState.currentPage
                val width by animateDpAsState(if (selected) 22.dp else 7.dp)
                Box(
                    Modifier
                        .height(7.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (selected) SkyBlue else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun PromoBanner(promo: Promo) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .softShadow(10.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(promo.colors))
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.75f, center = Offset(size.width * 0.86f, size.height * 0.55f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.35f, center = Offset(size.width * 0.55f, -size.height * 0.05f))
            }
            .clickable(onClick = promo.onClick),
    ) {
        Text(
            promo.emoji,
            fontSize = 74.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp),
        )
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(0.66f).padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
                    Text(
                        promo.tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(promo.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1)
                Text(
                    promo.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(onClick = promo.onClick, color = Color.White, shape = RoundedCornerShape(50)) {
                Text(
                    "${promo.cta}  →",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Navy,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

// ── Categories ───────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    categories: List<Category>,
    selectedId: String?,
    onClick: (String) -> Unit,
    onAllClick: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "all") {
            CategoryTile(emoji = "✨", label = "ኩሉ", colors = categoryColors(null), selected = selectedId == null, onClick = onAllClick)
        }
        items(categories, key = { it.id }) { category ->
            CategoryTile(
                emoji = category.emoji,
                label = category.localName,
                colors = categoryColors(category.id),
                selected = category.id == selectedId,
                onClick = { onClick(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryTile(emoji: String, label: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(shape)
                .background(Brush.linearGradient(colors))
                .then(if (selected) Modifier.border(2.5.dp, SkyBlue, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) SkyBlue else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🔎", fontSize = 48.sp)
        Text("ዝተረኽበ ኣቕሓ የለን", style = MaterialTheme.typography.titleMedium)
        Text(
            "ካልእ ቃል ፈትን ወይ ካልእ ዓይነት ምረጽ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
