package com.note.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.note.widgets.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NoteAdapter(
            notes = mutableListOf(),
            onNoteClick = { note -> openEditor(note.id) },
            onDeleteClick = { note ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.delete_confirm_title))
                    .setMessage(getString(R.string.delete_confirm_message))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        NoteStorage.deleteNote(this, note.id)
                        updateWidget()
                        refreshList()
                    }
                    .show()
            }
        )

        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter

        binding.fabAdd.setOnClickListener { openTypePicker() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun openEditor(noteId: Long) {
        val intent = Intent(this, NoteEditActivity::class.java)
        intent.putExtra(NoteEditActivity.EXTRA_NOTE_ID, noteId)
        startActivity(intent)
    }

    private fun openTypePicker() {
        startActivity(Intent(this, NoteTypePickerActivity::class.java))
    }

    private fun refreshList() {
        val notes = NoteStorage.loadNotes(this)
        adapter.updateNotes(notes)
        binding.emptyText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerNotes.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateWidget() {
        val intent = Intent(this, NoteWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, NoteWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }
}
