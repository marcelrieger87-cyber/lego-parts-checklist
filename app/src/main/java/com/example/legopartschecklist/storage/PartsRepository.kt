package com.example.legopartschecklist.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.legopartschecklist.model.PartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "lego_parts_store")

class PartsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val partsKey = stringPreferencesKey("parts_json")

    val partsFlow: Flow<List<PartItem>> = context.dataStore.data.map { preferences ->
        val raw = preferences[partsKey].orEmpty()
        if (raw.isBlank()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(PartItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun save(parts: List<PartItem>) {
        context.dataStore.edit { preferences ->
            preferences[partsKey] = json.encodeToString(ListSerializer(PartItem.serializer()), parts)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.remove(partsKey)
        }
    }
}
