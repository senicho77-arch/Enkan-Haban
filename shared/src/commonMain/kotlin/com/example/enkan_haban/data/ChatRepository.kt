package com.example.enkan_haban.data

import com.example.enkan_haban.domain.CallbackRequest
import com.example.enkan_haban.domain.ChatMessage
import com.example.enkan_haban.domain.Conversation
import com.example.enkan_haban.domain.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Same limit as messages.body in the backend. */
const val MAX_MESSAGE_LENGTH = 4000

private val phonePattern = Regex("^\\+?[0-9 ]{7,20}$")

/** Same rule as the backend (digits/spaces, optional +) plus at least 9 digits, like the sell form. */
fun isValidPhone(phone: String): Boolean {
    val trimmed = phone.trim()
    return phonePattern.matches(trimmed) && trimmed.count { it.isDigit() } >= 9
}

/**
 * Buyer ↔ seller contact on classified ads: chat and "request call back".
 * In memory for now; the Supabase version uses rpc("start_conversation"), the messages
 * table and rpc("request_callback").
 */
interface ChatRepository {
    /** Newest activity first. */
    val conversations: StateFlow<List<Conversation>>
    val callbackRequests: StateFlow<List<CallbackRequest>>

    /** Opens (or reopens) the chat about [product], sends [body] and returns the conversation id. */
    fun startChat(product: Product, body: String): String
    fun send(conversationId: String, body: String)
    fun requestCallback(product: Product, name: String, phone: String)
}

class InMemoryChatRepository : ChatRepository {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _callbackRequests = MutableStateFlow<List<CallbackRequest>>(emptyList())
    override val callbackRequests: StateFlow<List<CallbackRequest>> = _callbackRequests.asStateFlow()

    private var nextMessageId = 1L

    override fun startChat(product: Product, body: String): String {
        val id = "chat-${product.id}"
        if (_conversations.value.none { it.id == id }) {
            _conversations.update { listOf(Conversation(id, product)) + it }
        }
        send(id, body)
        return id
    }

    override fun send(conversationId: String, body: String) {
        val text = body.trim()
        require(text.isNotEmpty() && text.length <= MAX_MESSAGE_LENGTH) { "Message must be 1–$MAX_MESSAGE_LENGTH characters" }
        _conversations.update { list ->
            val conversation = list.firstOrNull { it.id == conversationId } ?: error("Unknown conversation $conversationId")
            val updated = conversation.copy(messages = conversation.messages + ChatMessage(nextMessageId++, text, fromMe = true))
            listOf(updated) + list.filterNot { it.id == conversationId }
        }
    }

    override fun requestCallback(product: Product, name: String, phone: String) {
        require(name.isNotBlank()) { "Name is required" }
        require(isValidPhone(phone)) { "Phone is not valid" }
        // Asking again on the same ad replaces the earlier request, like the backend.
        _callbackRequests.update { list ->
            list.filterNot { it.productId == product.id } + CallbackRequest(product.id, name.trim(), phone.trim())
        }
    }
}
