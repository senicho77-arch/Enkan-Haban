package com.example.enkan_haban.ui.sell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.domain.ListingDraft
import com.example.enkan_haban.domain.Photo
import com.example.enkan_haban.domain.Product
import com.example.enkan_haban.domain.Seller
import com.example.enkan_haban.util.Currency
import com.example.enkan_haban.util.CurrencySettings
import com.example.enkan_haban.util.formatPriceInput
import com.example.enkan_haban.util.parsePrice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SellForm(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val categoryId: String? = null,
    val condition: Condition = Condition.USED,
    val location: String = "",
    val phone: String = "",
    val photos: List<Photo> = emptyList(),
)

/** Fills the form with an existing ad's values, the price written in [currency]. */
fun Product.toSellForm(currency: Currency = CurrencySettings.current) = SellForm(
    title = title,
    description = description,
    price = formatPriceInput(price, currency),
    categoryId = categoryId,
    condition = condition,
    location = location,
    phone = seller.phone,
    photos = photos,
)

/**
 * The ad being edited. The original price is kept so an untouched price field saves the
 * exact stored amount, instead of drifting by the rounding of the shilling conversion.
 */
data class EditTarget(val productId: String, val price: Long, val priceText: String, val currency: Currency)

/** The price to save: the original one if the seller didn't change the field, else what they typed. */
fun resolvePrice(form: SellForm, editing: EditTarget?, currency: Currency = CurrencySettings.current): Long? =
    if (editing != null && form.price == editing.priceText && currency == editing.currency) {
        editing.price
    } else {
        parsePrice(form.price, currency)
    }

data class SellUiState(
    val form: SellForm = SellForm(),
    /** Set when editing an existing ad instead of posting a new one. */
    val editing: EditTarget? = null,
    /** True while the ad to edit is loading. */
    val isLoading: Boolean = false,
    /** Set when the ad to edit is gone or belongs to someone else. */
    val loadError: String? = null,
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    /** Shown under the photos, e.g. when some picked photos didn't fit. */
    val photoMessage: String? = null,
) {
    val photoSlotsLeft: Int get() = MAX_PHOTOS - form.photos.size
}

data class PhotoAddResult(val photos: List<Photo>, val dropped: Int)

/** Appends [picked] to [current], keeping at most [MAX_PHOTOS]; extras are dropped and counted. */
fun mergePhotos(current: List<Photo>, picked: List<Photo>): PhotoAddResult {
    val room = (MAX_PHOTOS - current.size).coerceAtLeast(0)
    val kept = picked.take(room)
    return PhotoAddResult(current + kept, dropped = picked.size - kept.size)
}

/** Returns field name -> error message; empty when the form is valid. */
fun validate(form: SellForm): Map<String, String> = buildMap {
    if (form.title.trim().length < 5) put("title", "ኣርእስቲ እንተወሓደ 5 ፊደላት ክኸውን ኣለዎ")
    if (form.description.isBlank()) put("description", "ሓጺር መግለጺ ወስኽ")
    val price = parsePrice(form.price)
    if (price == null || price <= 0) put("price", "ቅኑዕ ዋጋ ኣእቱ")
    if (form.categoryId == null) put("category", "ዓይነት ምረጽ")
    if (form.location.isBlank()) put("location", "ቦታኻ ኣእቱ")
    if (form.phone.count { it.isDigit() } < 9) put("phone", "ቅኑዕ ቁጽሪ ተሌፎን ኣእቱ")
    if (form.photos.size > MAX_PHOTOS) put("photos", "ክሳብ $MAX_PHOTOS ፎቶታት ጥራይ ክትውስኽ ትኽእል")
}

/** Posts a new ad, or — with [editProductId] — edits the seller's existing one. */
class SellViewModel(
    private val repository: ProductRepository,
    private val seller: Seller,
    private val editProductId: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SellUiState(isLoading = editProductId != null))
    val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

    init {
        if (editProductId != null) {
            viewModelScope.launch {
                val product = repository.getProduct(editProductId)?.takeIf { it.isEditableBy(seller) }
                _uiState.value = if (product == null) {
                    SellUiState(loadError = "እዚ መዋዓውዒ ክመሓየሽ ኣይክእልን")
                } else {
                    val currency = CurrencySettings.current
                    val form = product.toSellForm(currency)
                    SellUiState(form = form, editing = EditTarget(product.id, product.price, form.price, currency))
                }
            }
        }
    }

    val categories get() = repository.categories

    fun update(transform: (SellForm) -> SellForm) {
        _uiState.update { it.copy(form = transform(it.form), errors = emptyMap()) }
    }

    fun addPhotos(picked: List<ByteArray>) {
        if (picked.isEmpty()) return
        _uiState.update { state ->
            val result = mergePhotos(state.form.photos, picked.map(::Photo))
            state.copy(
                form = state.form.copy(photos = result.photos),
                errors = state.errors - "photos",
                photoMessage = if (result.dropped > 0) {
                    "ክሳብ $MAX_PHOTOS ፎቶታት ጥራይ ይፍቀድ — ${result.dropped} ኣይተወሰኹን"
                } else {
                    null
                },
            )
        }
    }

    fun removePhoto(photo: Photo) {
        _uiState.update { state ->
            state.copy(form = state.form.copy(photos = state.form.photos - photo), photoMessage = null)
        }
    }

    /**
     * Validates and posts the ad — or saves the changes when editing — then calls [onDone]
     * with the product id.
     */
    fun submit(onDone: (String) -> Unit) {
        val state = _uiState.value
        val form = state.form
        val errors = validate(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val draft = ListingDraft(
                title = form.title.trim(),
                description = form.description.trim(),
                price = resolvePrice(form, state.editing)!!,
                categoryId = form.categoryId!!,
                condition = form.condition,
                location = form.location.trim(),
                phone = form.phone.trim(),
                photos = form.photos,
            )
            val editing = state.editing
            if (editing == null) {
                val product = repository.createListing(draft, seller)
                _uiState.value = SellUiState()
                onDone(product.id)
            } else {
                val product = repository.updateListing(editing.productId, draft, seller)
                if (product == null) {
                    _uiState.update { it.copy(isSubmitting = false, loadError = "ለውጢ ኣይተዓቀበን — እዚ መዋዓውዒ የለን") }
                } else {
                    _uiState.update { it.copy(isSubmitting = false) }
                    onDone(product.id)
                }
            }
        }
    }
}
