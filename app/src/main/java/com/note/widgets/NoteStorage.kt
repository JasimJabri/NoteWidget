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

    private const val KEY_FONT_COLOR = "font_color"

    fun getFontColor(context: Context): Int {
        return prefs(context).getInt(KEY_FONT_COLOR, 0xFF1A1A1A.toInt())
    }

    fun setFontColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FONT_COLOR, color).apply()
    }

    private const val KEY_WIDGET_BG = "widget_bg_color"
    private const val KEY_RECENT_BG = "recent_bg_colors"

    fun getWidgetBgColor(context: Context): Int {
        return prefs(context).getInt(KEY_WIDGET_BG, 0xFFFFFFFF.toInt())
    }

    fun setWidgetBgColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_WIDGET_BG, color).apply()
        addRecentBgColor(context, color)
    }

    fun getRecentBgColors(context: Context): List<Int> {
        val json = prefs(context).getString(KEY_RECENT_BG, "[]") ?: "[]"
        val array = org.json.JSONArray(json)
        val colors = mutableListOf<Int>()
        for (i in 0 until array.length()) colors.add(array.getInt(i))
        return colors
    }

    private fun addRecentBgColor(context: Context, color: Int) {
        val recents = getRecentBgColors(context).toMutableList()
        recents.remove(color)
        recents.add(0, color)
        val trimmed = recents.take(5)
        val array = org.json.JSONArray()
        for (c in trimmed) array.put(c)
        prefs(context).edit().putString(KEY_RECENT_BG, array.toString()).apply()
    }
}
