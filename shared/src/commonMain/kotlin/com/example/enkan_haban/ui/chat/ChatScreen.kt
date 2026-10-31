package com.example.enkan_haban.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enkan_haban.AppContainer
import com.example.enkan_haban.data.MAX_MESSAGE_LENGTH
import com.example.enkan_haban.domain.ChatMessage
import com.example.enkan_haban.ui.components.SimpleTopBar
import com.example.enkan_haban.util.formatPrice

/** One chat with a seller about an ad. */
@Composable
fun ChatScreen(conversationId: String, onBack: () -> Unit, onOpenProduct: (String) -> Unit) {
    val conversations by AppContainer.chatRepository.conversations.collectAsStateWithLifecycle()
    val conversation = conversations.firstOrNull { it.id == conversationId }

    Column(Modifier.fillMaxSize().imePadding()) {
        SimpleTopBar(title = conversation?.product?.seller?.name ?: "ዕላል", onBack = onBack)
        if (conversation == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text("እዚ ዕላል ኣይተረኽበን።") }
            return@Column
        }
        val product = conversation.product

        Surface(
            onClick = { onOpenProduct(product.id) },
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatPrice(product.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(conversation.messages.size) {
            if (conversation.messages.isNotEmpty()) listState.animateScrollToItem(conversation.messages.lastIndex)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(conversation.messages, key = { it.id }) { MessageBubble(it) }
            item {
                Text(
                    "ሸያጢ ምስ መለሰ፡ መልሱ ኣብዚ ክርአ እዩ።",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }

        var draft by remember { mutableStateOf("") }
        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) draft = it },
                    placeholder = { Text("መልእኽቲ ጽሓፍ…") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        AppContainer.chatRepository.send(conversation.id, draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) { Text("ስደድ") }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (message.fromMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            message.body,
            color = if (message.fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (message.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** All of the buyer's chats, newest first. */
@Composable
fun ChatListScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit) {
    val conversations by AppContainer.chatRepository.conversations.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar(title = "መልእኽታተይ", onBack = onBack)
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text("ገና ዕላል የብልካን። ኣብ መወዓውዒ \"ዕላል ጀምር\" ጠውቕ።")
            }
            return@Column
        }
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            LazyColumn {
                items(conversations, key = { it.id }) { conversation ->
                    if (conversation != conversations.first()) HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(conversation.product.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(conversation.messages.lastOrNull()?.body.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = { Text(conversation.product.seller.name, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.clickable { onOpenChat(conversation.id) },
                    )
                }
            }
        }
    }
}
