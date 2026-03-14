package com.example.legopartschecklist.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.legopartschecklist.model.PartItem
import com.example.legopartschecklist.parser.PdfPartsParser
import com.example.legopartschecklist.storage.PartsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val parts: List<PartItem> = emptyList(),
    val status: String = "PDF laden oder Teile manuell ergänzen.",
    val isLoading: Boolean = false,
    val filterMissingOnly: Boolean = false,
    val sortMode: SortMode = SortMode.ORIGINAL_ORDER,
    val copiedSummary: Boolean = false
)

enum class SortMode { ORIGINAL_ORDER, PART_NUMBER, MOST_MISSING }

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PartsRepository(application)
    private val parser = PdfPartsParser(application)
    private val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var rawParts: List<PartItem> = emptyList()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.partsFlow.collect { storedParts ->
                rawParts = storedParts
                _uiState.value = _uiState.value.copy(
                    parts = applySortAndFilter(rawParts, _uiState.value),
                    copiedSummary = false
                )
            }
        }
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, status = "Letzte PDF-Seiten werden analysiert …")
            runCatching { parser.parse(uri) }
                .onSuccess { parts ->
                    val message = if (parts.isEmpty()) {
                        "Keine Inventarpositionen erkannt. Für gescannte PDFs braucht die App noch OCR/Bilderkennung."
                    } else {
                        "${parts.size} Inventarpositionen erkannt. Reihenfolge der letzten PDF-Seiten wurde übernommen."
                    }
                    repository.save(parts)
                    _uiState.value = _uiState.value.copy(isLoading = false, status = message)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        status = "Fehler beim PDF-Import: ${error.message ?: "Unbekannt"}"
                    )
                }
        }
    }

    fun addEmptyItem() {
        mutateParts { it + PartItem(name = "Neues Teil", required = 1, sourceOrder = it.size) }
    }

    fun updateItem(updated: PartItem) {
        mutateParts { list -> list.map { if (it.id == updated.id) updated else it } }
    }

    fun removeItem(id: String) {
        mutateParts { list -> list.filterNot { it.id == id } }
    }

    fun toggleMissingOnly() {
        val newState = _uiState.value.copy(filterMissingOnly = !_uiState.value.filterMissingOnly)
        _uiState.value = newState.copy(parts = applySortAndFilter(rawParts, newState))
    }

    fun setSortMode(mode: SortMode) {
        val newState = _uiState.value.copy(sortMode = mode)
        _uiState.value = newState.copy(parts = applySortAndFilter(rawParts, newState))
    }

    fun copyMissingSummary() {
        val summary = buildMissingSummary(rawParts)
        clipboard.setPrimaryClip(ClipData.newPlainText("Fehlende LEGO Teile", summary))
        _uiState.value = _uiState.value.copy(copiedSummary = true, status = "Fehlende Teile in die Zwischenablage kopiert.")
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clear()
            _uiState.value = _uiState.value.copy(status = "Liste gelöscht.")
        }
    }

    private fun mutateParts(transform: (List<PartItem>) -> List<PartItem>) {
        viewModelScope.launch {
            repository.save(transform(rawParts))
        }
    }

    private fun applySortAndFilter(parts: List<PartItem>, state: AppUiState): List<PartItem> {
        val filtered = if (state.filterMissingOnly) parts.filter { it.missing > 0 } else parts
        return when (state.sortMode) {
            SortMode.ORIGINAL_ORDER -> filtered.sortedWith(compareBy<PartItem> { it.sourcePage ?: Int.MAX_VALUE }.thenBy { it.sourceOrder })
            SortMode.PART_NUMBER -> filtered.sortedBy { it.partNumber }
            SortMode.MOST_MISSING -> filtered.sortedByDescending { it.missing }
        }
    }

    private fun buildMissingSummary(parts: List<PartItem>): String {
        val missingParts = parts.filter { it.missing > 0 }
        if (missingParts.isEmpty()) return "Es fehlen aktuell keine Teile."
        return buildString {
            appendLine("Fehlende LEGO-Teile")
            appendLine()
            missingParts.forEach { item ->
                appendLine("${item.partNumber}; fehlt=${item.missing}; benötigt=${item.required}; vorhanden=${item.have}")
            }
        }.trim()
    }
}
