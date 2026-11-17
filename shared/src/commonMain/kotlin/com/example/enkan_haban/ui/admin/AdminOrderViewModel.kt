package com.example.enkan_haban.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enkan_haban.data.AdminRepository
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.domain.ALL_CATEGORIES
import com.example.enkan_haban.domain.Category
import com.example.enkan_haban.domain.Placement
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.util.nowEpochMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the "placed" list on the admin screen. [position] is always index + 1. */
data class PlacedRow(
    val product: Product,
    val position: Int,
    val pinnedUntilEpochMs: Long?,
    val isExpired: Boolean,
)

data class AdminOrderUiState(
    val categoryKey: String = ALL_CATEGORIES,
    val categories: List<Category> = emptyList(),
    val placed: List<PlacedRow> = emptyList(),
    /** Products of the category that are not placed, newest first. */
    val others: List<Product> = emptyList(),
    /** True when the order on screen differs from the saved one. */
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    /** One-off confirmation for the snackbar. */
    val message: String? = null,
    val error: String? = null,
)

/**
 * Edits the admin order of one category locally; [save] sends the whole order in one call.
 * [scope] is only for tests; the app uses viewModelScope.
 */
class AdminOrderViewModel(
    private val productRepository: ProductRepository,
    private val adminRepository: AdminRepository,
    scope: CoroutineScope? = null,
    private val now: () -> Long = ::nowEpochMs,
) : ViewModel() {
    private val scope: CoroutineScope = scope ?: viewModelScope

    private var products: List<Product> = emptyList()
    private var placements: List<Placement> = emptyList()

    private val _uiState = MutableStateFlow(AdminOrderUiState(categories = productRepository.categories))
    val uiState: StateFlow<AdminOrderUiState> = _uiState.asStateFlow()

    init {
        this.scope.launch {
            combine(productRepository.observeProducts(), adminRepository.observePlacements()) { p, pl -> p to pl }
                .collect { (p, pl) ->
                    products = p
                    placements = pl
                    // Keep the admin's unsaved edits; only reload when nothing was changed.
                    if (!_uiState.value.hasChanges) reload()
                }
        }
    }

    fun selectCategory(categoryKey: String) {
        _uiState.update { it.copy(categoryKey = categoryKey, hasChanges = false, error = null) }
        reload()
    }

    fun moveUp(index: Int) = move(index, index - 1)

    fun moveDown(index: Int) = move(index, index + 1)

    /** Moves the row at [from] to [to] (also used by drag to reorder). */
    fun move(from: Int, to: Int) {
        val rows = _uiState.value.placed
        if (from !in rows.indices || to !in rows.indices || from == to) return
        val list = rows.toMutableList()
        list.add(to, list.removeAt(from))
        edit(list, _uiState.value.others)
    }

    /** "✖": takes the row out of the order; it goes back to the other posts. */
    fun remove(index: Int) {
        val state = _uiState.value
        val row = state.placed.getOrNull(index) ?: return
        edit(state.placed - row, newestFirst(state.others + row.product))
    }

    /** "➕ ናብ መስርዕ": adds a product to the end of the placed list. */
    fun addToOrder(productId: String) {
        val state = _uiState.value
        val product = state.others.firstOrNull { it.id == productId } ?: return
        edit(state.placed + PlacedRow(product, 0, null, false), state.others - product)
    }

    /** "📌": sets or clears (null) the end date of one row. */
    fun setPinnedUntil(index: Int, epochMs: Long?) {
        val state = _uiState.value
        if (index !in state.placed.indices) return
        val list = state.placed.toMutableList()
        list[index] = list[index].copy(pinnedUntilEpochMs = epochMs, isExpired = epochMs != null && epochMs <= now())
        edit(list, state.others)
    }

    /** "ሰርዝ": throws away unsaved changes. */
    fun discard() {
        _uiState.update { it.copy(hasChanges = false, error = null) }
        reload()
    }

    /** "ዕቀብ": sends the full order of this category in one call. */
    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        scope.launch {
            try {
                adminRepository.setOrder(
                    state.categoryKey,
                    state.placed.map { it.product.id },
                    state.placed.mapNotNull { row -> row.pinnedUntilEpochMs?.let { row.product.id to it } }.toMap(),
                )
                _uiState.update { it.copy(isSaving = false, hasChanges = false, message = "መስርዕ ተዓቂቡ") }
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "መስርዕ ኣይተዓቀበን: ${e.message ?: "ጌጋ ኣጋጢሙ"}") }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun edit(placed: List<PlacedRow>, others: List<Product>) {
        _uiState.update {
            it.copy(
                placed = placed.mapIndexed { i, row -> row.copy(position = i + 1) },
                others = others,
                hasChanges = true,
                error = null,
            )
        }
    }

    /** Rebuilds both lists from the saved placements of the selected category. */
    private fun reload() {
        val key = _uiState.value.categoryKey
        val inCategory = products.filter { key == ALL_CATEGORIES || it.categoryId == key }
        val byId = inCategory.associateBy { it.id }
        val nowMs = now()
        val placed = placements
            .filter { it.categoryKey == key && it.productId in byId }
            .sortedBy { it.position }
            .mapIndexed { i, p -> PlacedRow(byId.getValue(p.productId), i + 1, p.pinnedUntilEpochMs, p.isExpired(nowMs)) }
        val placedIds = placed.map { it.product.id }.toSet()
        _uiState.update {
            it.copy(placed = placed, others = newestFirst(inCategory.filter { p -> p.id !in placedIds }))
        }
    }

    /** Newest first; products without a date keep the repository's order (already newest first). */
    private fun newestFirst(list: List<Product>): List<Product> {
        val order = products.withIndex().associate { (i, p) -> p.id to i }
        return list.sortedWith(compareByDescending<Product> { it.createdAtEpochMs }.thenBy { order[it.id] ?: Int.MAX_VALUE })
    }
}
