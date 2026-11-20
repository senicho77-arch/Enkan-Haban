package com.example.enkan_haban.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enkan_haban.data.PromoRepository
import com.example.enkan_haban.domain.Photo
import com.example.enkan_haban.domain.PromoBanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The banner being written or changed in the form. [newImage] is a picture picked but not saved yet. */
data class PromoForm(
    val banner: PromoBanner = PromoBanner(),
    val newImage: ByteArray? = null,
) {
    /** What the banner will look like after saving (for the live preview). */
    val preview: PromoBanner
        get() = if (newImage != null) banner.copy(localImage = Photo(newImage), imageUrl = null) else banner

    override fun equals(other: Any?): Boolean =
        other is PromoForm && other.banner == banner && other.newImage === newImage

    override fun hashCode(): Int = banner.hashCode() * 31 + (newImage?.size ?: 0)
}

data class PromoAdminUiState(
    val banners: List<PromoBanner> = emptyList(),
    /** Null = the list is shown; otherwise the form. */
    val form: PromoForm? = null,
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/** Field → error text (Tigrinya); empty when the banner can be saved. */
fun validatePromo(form: PromoForm): Map<String, String> = buildMap {
    val b = form.banner
    if (b.title.isBlank() && !b.hasPicture && form.newImage == null) put("title", "ርእሲ ወይ ስእሊ ኣእቱ")
    if (b.title.length > 60) put("title", "ርእሲ ክሳብ 60 ፊደላት")
    if (b.subtitle.length > 120) put("subtitle", "መግለጺ ክሳብ 120 ፊደላት")
    if (b.tag.length > 30) put("tag", "ምልክት ክሳብ 30 ፊደላት")
    if (b.cta.length > 24) put("cta", "መልጎም ክሳብ 24 ፊደላት")
    if (b.startsAtMs != null && b.endsAtMs != null && b.endsAtMs <= b.startsAtMs) put("dates", "መወዳእታ ድሕሪ መጀመርታ ክኸውን ኣለዎ")
}

/** Admin screen for the home screen adverts: list, add, edit, delete, reorder, on/off. */
class PromoAdminViewModel(
    private val repository: PromoRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = scope ?: viewModelScope

    private val _uiState = MutableStateFlow(PromoAdminUiState())
    val uiState: StateFlow<PromoAdminUiState> = _uiState.asStateFlow()

    init {
        this.scope.launch {
            repository.banners.collect { list -> _uiState.update { it.copy(banners = list.sortedBy { b -> b.sortOrder }) } }
        }
        // Admins also get hidden and expired banners from the database.
        launchAction { repository.refresh() }
    }

    fun startNew() = _uiState.update { it.copy(form = PromoForm(), errors = emptyMap(), error = null) }

    fun edit(id: String) {
        val banner = _uiState.value.banners.firstOrNull { it.id == id } ?: return
        _uiState.update { it.copy(form = PromoForm(banner), errors = emptyMap(), error = null) }
    }

    fun update(transform: (PromoBanner) -> PromoBanner) =
        _uiState.update { s -> s.copy(form = s.form?.let { it.copy(banner = transform(it.banner)) }, errors = emptyMap()) }

    fun setImage(bytes: ByteArray?) = _uiState.update { s ->
        s.copy(form = s.form?.let { f ->
            if (bytes == null) f.copy(newImage = null, banner = f.banner.copy(imageUrl = null, localImage = null)) else f.copy(newImage = bytes)
        })
    }

    fun cancel() = _uiState.update { it.copy(form = null, errors = emptyMap(), error = null) }

    fun save() {
        val form = _uiState.value.form ?: return
        val errors = validatePromo(form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        launchAction(onSuccess = { it.copy(form = null, message = "ማስታወቂያ ተዓቂቡ") }) {
            repository.save(form.banner, form.newImage)
        }
    }

    fun delete(id: String) = launchAction(onSuccess = { it.copy(message = "ማስታወቂያ ተደምሲሱ") }) { repository.delete(id) }

    fun move(id: String, up: Boolean) = launchAction { repository.move(id, up) }

    fun toggleActive(id: String) {
        val banner = _uiState.value.banners.firstOrNull { it.id == id } ?: return
        launchAction { repository.save(banner.copy(isActive = !banner.isActive), null) }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun launchAction(
        onSuccess: (PromoAdminUiState) -> PromoAdminUiState = { it },
        block: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                block()
                _uiState.update { onSuccess(it.copy(isSaving = false)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "ኣይተዓወተን: ${e.message ?: "ጌጋ"}") }
            }
        }
    }
}
