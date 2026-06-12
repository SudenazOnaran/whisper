package com.example.data

import android.content.Context
import com.example.network.SegmentData
import com.example.network.TranscriptResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedNote(
    val id: String,
    val title: String,
    val fullText: String,
    val segments: List<SegmentData>,
    val language: String,
    val languageProbability: Float = 0f,
    val sourceType: String,
    val createdAt: Long
) {
    fun toTranscriptResult() = TranscriptResult(
        fullText = fullText,
        segments = segments,
        language = language,
        languageProbability = languageProbability
    )
}

class NoteStore(context: Context) {
    private val notesFile = File(context.filesDir, "notes.json")

    fun loadNotes(): List<SavedNote> {
        if (!notesFile.exists()) return emptyList()

        return runCatching {
            val array = JSONArray(notesFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i).toSavedNote())
                }
            }
        }.getOrDefault(emptyList())
            .sortedByDescending { it.createdAt }
    }

    fun saveNote(note: SavedNote): List<SavedNote> {
        val notes = loadNotes().filterNot { it.id == note.id } + note
        writeNotes(notes)
        return notes.sortedByDescending { it.createdAt }
    }

    fun deleteNote(id: String): List<SavedNote> {
        val notes = loadNotes().filterNot { it.id == id }
        writeNotes(notes)
        return notes
    }

    private fun writeNotes(notes: List<SavedNote>) {
        val array = JSONArray()
        notes.sortedByDescending { it.createdAt }.forEach { array.put(it.toJson()) }
        notesFile.writeText(array.toString(2))
    }
}

private fun SavedNote.toJson() = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("fullText", fullText)
    put("language", language)
    put("languageProbability", languageProbability)
    put("sourceType", sourceType)
    put("createdAt", createdAt)
    put("segments", JSONArray().apply {
        segments.forEach { segment ->
            put(JSONObject().apply {
                put("time", segment.time)
                put("text", segment.text)
                put("start", segment.start)
                put("end", segment.end)
            })
        }
    })
}

private fun JSONObject.toSavedNote(): SavedNote {
    val segmentArray = optJSONArray("segments") ?: JSONArray()
    val segments = buildList {
        for (i in 0 until segmentArray.length()) {
            val segment = segmentArray.getJSONObject(i)
            add(
                SegmentData(
                    time = segment.optString("time", "00:00"),
                    text = segment.optString("text", ""),
                    start = segment.optDouble("start", 0.0).toFloat(),
                    end = segment.optDouble("end", 0.0).toFloat()
                )
            )
        }
    }

    return SavedNote(
        id = optString("id"),
        title = optString("title", "İsimsiz Not"),
        fullText = optString("fullText"),
        segments = segments,
        language = optString("language", "bilinmiyor"),
        languageProbability = optDouble("languageProbability", 0.0).toFloat(),
        sourceType = optString("sourceType", "unknown"),
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )
}
