package com.example.legopartschecklist.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.legopartschecklist.model.PartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegoPartsApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importPdf(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LEGO Teile-Checkliste") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::addEmptyItem) {
                Icon(Icons.Default.Add, contentDescription = "Teil hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("PDF laden")
                }
                AssistChip(
                    onClick = viewModel::toggleMissingOnly,
                    label = { Text(if (state.filterMissingOnly) "Nur fehlende: an" else "Nur fehlende: aus") }
                )
            }

            Spacer(Modifier.height(12.dp))
            SortSelector(current = state.sortMode, onSelected = viewModel::setSortMode)
            Spacer(Modifier.height(12.dp))
            SummaryCard(
                parts = state.parts,
                status = state.status,
                onCopySummary = viewModel::copyMissingSummary
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.parts, key = { it.id }) { item ->
                    PartRow(
                        item = item,
                        onChange = viewModel::updateItem,
                        onDelete = { viewModel.removeItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(parts: List<PartItem>, status: String, onCopySummary: () -> Unit) {
    val totalRequired = parts.sumOf { it.required }
    val totalHave = parts.sumOf { it.have }
    val totalMissing = parts.sumOf { it.missing }
    val completed = parts.count { it.isComplete }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Zusammenfassung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(status)
            Divider()
            Text("Positionen: ${parts.size}")
            Text("Benötigt insgesamt: $totalRequired")
            Text("Vorhanden insgesamt: $totalHave")
            Text("Fehlend insgesamt: $totalMissing")
            Text("Vollständig erledigt: $completed / ${parts.size}")
            OutlinedButton(onClick = onCopySummary) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Fehlende Teile kopieren")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSelector(current: SortMode, onSelected: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = when (current) {
                SortMode.ORIGINAL_ORDER -> "Sortierung: PDF-Reihenfolge"
                SortMode.PART_NUMBER -> "Sortierung: Teilenummer"
                SortMode.MOST_MISSING -> "Sortierung: Meist fehlend"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("PDF-Reihenfolge") }, onClick = { onSelected(SortMode.ORIGINAL_ORDER); expanded = false })
            DropdownMenuItem(text = { Text("Teilenummer") }, onClick = { onSelected(SortMode.PART_NUMBER); expanded = false })
            DropdownMenuItem(text = { Text("Meist fehlend") }, onClick = { onSelected(SortMode.MOST_MISSING); expanded = false })
        }
    }
}

@Composable
private fun PartRow(item: PartItem, onChange: (PartItem) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PartThumbnail(item)
                Checkbox(
                    checked = item.isComplete,
                    onCheckedChange = { checked ->
                        onChange(item.copy(have = if (checked) item.required else 0))
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.displayName, fontWeight = FontWeight.Bold)
                    Text("Teilenummer: ${item.partNumber.ifBlank { "-" }}")
                    Text("Inventarseite: ${item.sourcePage ?: "-"}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = item.required,
                    label = "Benötigt",
                    modifier = Modifier.weight(1f),
                    onValueChange = { onChange(item.copy(required = it)) }
                )
                NumberField(
                    value = item.have,
                    label = "Habe",
                    modifier = Modifier.weight(1f),
                    onValueChange = { onChange(item.copy(have = it)) }
                )
            }
            OutlinedTextField(
                value = item.name,
                onValueChange = { onChange(item.copy(name = it)) },
                label = { Text("Name/Beschreibung") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = item.partNumber,
                onValueChange = { onChange(item.copy(partNumber = it)) },
                label = { Text("Teilenummer") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Fehlt noch: ${item.missing}")
        }
    }
}

@Composable
private fun PartThumbnail(item: PartItem) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.partNumber.ifBlank { "Bild" },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
private fun NumberField(value: Int, label: String, modifier: Modifier = Modifier, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onValueChange(it.filter(Char::isDigit).toIntOrNull() ?: 0) },
        label = { Text(label) },
        modifier = modifier
    )
}
