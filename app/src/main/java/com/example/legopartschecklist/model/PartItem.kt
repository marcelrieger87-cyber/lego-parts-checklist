package com.example.legopartschecklist.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PartItem(
    val id: String = UUID.randomUUID().toString(),
    val partNumber: String = "",
    val name: String = "",
    val color: String = "",
    val required: Int = 0,
    val have: Int = 0,
    val notes: String = "",
    val sourcePage: Int? = null,
    val sourceOrder: Int = 0,
    val thumbnailNote: String = ""
) {
    val missing: Int get() = (required - have).coerceAtLeast(0)
    val isComplete: Boolean get() = have >= required && required > 0
    val displayName: String get() = name.ifBlank { "Teil $partNumber".trim() }
}
