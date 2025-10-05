@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package org.lingxi9374.game2048

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Serializable
data class HistoryEntry(
    val score: Int,
    val timeElapsed: Long,
    val maxTile: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class HistoryManager(private val context: Context) {

    private val HISTORY_KEY = stringPreferencesKey("game_history")

    val history: Flow<List<HistoryEntry>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[HISTORY_KEY]
            if (jsonString != null) {
                Json.decodeFromString<List<HistoryEntry>>(jsonString)
            } else {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)

    suspend fun addHistoryEntry(entry: HistoryEntry) {
        context.dataStore.edit { preferences ->
            val currentHistoryJson = preferences[HISTORY_KEY]
            val currentHistory = if (currentHistoryJson != null) {
                Json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
            } else {
                mutableListOf()
            }
            currentHistory.add(0, entry) // Add new entry to the top
            preferences[HISTORY_KEY] = Json.encodeToString(currentHistory)
        }
    }

    suspend fun exportHistory(): String {
        val historyList = history.first()
        val sortedHistory = historyList.sortedBy { it.timestamp }
        val stringBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        sortedHistory.forEach { entry ->
            val date = dateFormat.format(Date(entry.timestamp))
            stringBuilder.append("$date,${entry.score},${entry.timeElapsed},${entry.maxTile}\n")
        }
        return stringBuilder.toString()
    }

    suspend fun importHistory(text: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newEntries = text.lines().mapNotNull { line ->
            try {
                val parts = line.split(',')
                if (parts.size == 4) {
                    val date = dateFormat.parse(parts[0])
                    val score = parts[1].toInt()
                    val timeElapsed = parts[2].toLong()
                    val maxTile = parts[3].toInt()
                    HistoryEntry(score, timeElapsed, maxTile, date.time)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        if (newEntries.isNotEmpty()) {
            context.dataStore.edit { preferences ->
                val currentHistoryJson = preferences[HISTORY_KEY]
                val currentHistory = if (currentHistoryJson != null) {
                    Json.decodeFromString<MutableList<HistoryEntry>>(currentHistoryJson)
                } else {
                    mutableListOf()
                }
                currentHistory.addAll(newEntries)
                // Sort by timestamp and remove duplicates, keeping the one from the import
                val distinctHistory = currentHistory.distinctBy { it.timestamp }
                val sortedHistory = distinctHistory.sortedByDescending { it.timestamp }
                preferences[HISTORY_KEY] = Json.encodeToString(sortedHistory)
            }
        }
    }
}
