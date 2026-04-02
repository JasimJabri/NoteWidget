package com.note.widgets

import android.content.Context
import org.json.JSONArray

enum class NoteType { PLAIN, CHECKLIST, BULLET }

data class Note(val id: Long, val title: String, val text: String, val type: NoteType = NoteType.PLAIN)

object NoteStorage {
    private const val PREFS_NAME = "note_prefs"
    private const val KEY_NOTES = "notes_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadNotes(context: Context): MutableList<Note> {
        val json = prefs(context).getString(KEY_NOTES, "[]") ?: "[]"
        val array = JSONArray(json)
        val notes = mutableListOf<Note>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val type = try {
                NoteType.valueOf(obj.optString("type", "PLAIN"))
            } catch (_: Exception) { NoteType.PLAIN }
            notes.add(Note(
                obj.getLong("id"),
                obj.optString("title", ""),
                obj.getString("text"),
                type
            ))
        }
        return notes
    }

    fun saveNotes(context: Context, notes: List<Note>) {
        val array = JSONArray()
        for (note in notes) {
            val obj = org.json.JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("text", note.text)
            obj.put("type", note.type.name)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun addNote(context: Context, title: String, text: String, type: NoteType = NoteType.PLAIN): Note {
        val notes = loadNotes(context)
        val note = Note(id = System.currentTimeMillis(), title = title, text = text, type = type)
        notes.add(0, note)
        saveNotes(context, notes)
        return note
    }

    fun updateNote(context: Context, id: Long, title: String, text: String) {
        val notes = loadNotes(context)
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(title = title, text = text)
            saveNotes(context, notes)
        }
    }

    fun deleteNote(context: Context, id: Long) {
        val notes = loadNotes(context)
        notes.removeAll { it.id == id }
        saveNotes(context, notes)
    }

    fun getLatestNote(context: Context): String {
        return loadNotes(context).firstOrNull()?.text ?: ""
    }

    private const val KEY_WIDGET_INDEX = "widget_index_"

    fun getWidgetIndex(context: Context, widgetId: Int): Int {
        return prefs(context).getInt(KEY_WIDGET_INDEX + widgetId, 0)
    }

    fun setWidgetIndex(context: Context, widgetId: Int, index: Int) {
        prefs(context).edit().putInt(KEY_WIDGET_INDEX + widgetId, index).apply()
    }

    private const val KEY_SORT = "sort_option"

    fun getSortOption(context: Context): String {
        return prefs(context).getString(KEY_SORT, "NEWEST") ?: "NEWEST"
    }

    fun setSortOption(context: Context, option: String) {
        prefs(context).edit().putString(KEY_SORT, option).apply()
    }

    private const val KEY_FONT_SIZE = "font_size"

    fun getFontSize(context: Context): Int {
        return prefs(context).getInt(KEY_FONT_SIZE, 1)
    }

    fun setFontSize(context: Context, size: Int) {
        prefs(context).edit().putInt(KEY_FONT_SIZE, size).apply()
    }
}
