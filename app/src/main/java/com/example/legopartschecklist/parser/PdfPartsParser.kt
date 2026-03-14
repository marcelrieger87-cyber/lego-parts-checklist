package com.example.legopartschecklist.parser

import android.content.Context
import android.net.Uri
import com.example.legopartschecklist.model.PartItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfPartsParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun parse(uri: Uri): List<PartItem> {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "PDF konnte nicht gelesen werden." }
            return parseStream(input)
        }
    }

    private fun parseStream(input: InputStream): List<PartItem> {
        PDDocument.load(input).use { document ->
            val inventoryStart = maxOf(1, document.numberOfPages - 3)
            val pages = (inventoryStart..document.numberOfPages).map { pageNumber ->
                val text = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = pageNumber
                    endPage = pageNumber
                }.getText(document)
                pageNumber to text
            }
            return extractParts(pages)
        }
    }

    private fun extractParts(pageTexts: List<Pair<Int, String>>): List<PartItem> {
        val items = mutableListOf<PartItem>()
        var order = 0

        pageTexts.forEach { (pageNumber, rawText) ->
            val lines = rawText
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filterNot { it.contains("LEGO.com/service", ignoreCase = true) }
                .filterNot { it.contains("trademarks of the LEGO Group", ignoreCase = true) }
                .filterNot { it.matches(Regex("^\\d{6,8}$")) && it == linesLastModelNumber(rawText) }

            var pendingQty: Int? = null

            for (line in lines) {
                val qtyMatch = Regex("^(\\d{1,3})[xX×]$").matchEntire(line)
                if (qtyMatch != null) {
                    pendingQty = qtyMatch.groupValues[1].toIntOrNull()
                    continue
                }

                val inlineMatch = Regex("^(\\d{1,3})[xX×]\\s+(\\d{4,8})$").matchEntire(line)
                if (inlineMatch != null) {
                    items += PartItem(
                        partNumber = inlineMatch.groupValues[2],
                        name = "",
                        required = inlineMatch.groupValues[1].toInt(),
                        sourcePage = pageNumber,
                        sourceOrder = order++,
                        thumbnailNote = "Bildausschnitt aus Inventarseite vorbereiten"
                    )
                    pendingQty = null
                    continue
                }

                if (pendingQty != null && line.matches(Regex("^\\d{4,8}$"))) {
                    items += PartItem(
                        partNumber = line,
                        name = "",
                        required = pendingQty,
                        sourcePage = pageNumber,
                        sourceOrder = order++,
                        thumbnailNote = "Bildausschnitt aus Inventarseite vorbereiten"
                    )
                    pendingQty = null
                    continue
                }
            }
        }

        return items
            .filter { it.required > 0 && it.partNumber.isNotBlank() }
            .sortedWith(compareBy<PartItem> { it.sourcePage ?: Int.MAX_VALUE }.thenBy { it.sourceOrder })
    }

    private fun linesLastModelNumber(rawText: String): String? {
        return rawText
            .lines()
            .map { it.trim() }
            .lastOrNull { it.matches(Regex("^\\d{7}$")) }
    }
}
