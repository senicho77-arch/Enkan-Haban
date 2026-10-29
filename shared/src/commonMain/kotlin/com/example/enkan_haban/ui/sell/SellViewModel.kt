package com.example.enkan_haban.ui.sell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enkan_haban.data.ProductRepository
import com.example.enkan_haban.domain.Condition
import com.example.enkan_haban.domain.ListingDraft
import com.example.enkan_haban.domain.Seller
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
)

data class SellUiState(
    val form: SellForm = SellForm(),
    val errors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
)

/** Returns field name -> error message; empty when the form is valid. */
fun validate(form: SellForm): Map<String, String> = buildMap {
    if (form.title.trim().length < 5) put("title", "ኣርእስቲ እንተወሓደ 5 ፊደላት ክኸውን ኣለዎ")
    if (form.description.isBlank()) put("description", "ሓጺር መግለጺ ወስኽ")
    val price = parsePrice(form.price)
    if (price == null || price <= 0) put("price", "ቅኑዕ ዋጋ ኣእቱ")
    if (form.categoryId == null) put("category", "ዓይነት ምረጽ")
    if (form.location.isBlank()) put("location", "ቦታኻ ኣእቱ")
    if (form.phone.count { it.isDigit() } < 9) put("phone", "ቅኑዕ ቁጽሪ ተሌፎን ኣእቱ")
}

class SellViewModel(
    private val repository: ProductRepository,
    private val seller: Seller,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SellUiState())
    val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

    val categories get() = repository.categories

    fun update(transform: (SellForm) -> SellForm) {
        _uiState.update { it.copy(form = transform(it.form), errors = emptyMap()) }
    }

    /** Validates and posts the ad, then calls [onPosted] with the new product id. */
    fun submit(onPosted: (String) -> Unit) {
        val form = _uiState.value.form
        val errors = validate(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val product = repository.createListing(
                ListingDraft(
                    title = form.title.trim(),
                    description = form.description.trim(),
                    price = parsePrice(form.price)!!,
                    categoryId = form.categoryId!!,
                    condition = form.condition,
                    location = form.location.trim(),
                    phone = form.phone.trim(),
                ),
                seller,
            )
            _uiState.value = SellUiState()
            onPosted(product.id)
        }
    }
}
