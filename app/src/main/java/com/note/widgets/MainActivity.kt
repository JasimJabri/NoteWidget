package com.note.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.note.widgets.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private var allNotes = mutableListOf<Note>()
    private var currentSort = SortOption.NEWEST

    private enum class SortOption { NEWEST, OLDEST, TITLE_AZ, TITLE_ZA, BY_TYPE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentSort = try {
            SortOption.valueOf(NoteStorage.getSortOption(this))
        } catch (_: Exception) { SortOption.NEWEST }

        adapter = NoteAdapter(
            notes = mutableListOf(),
            onNoteClick = { note -> openEditor(note.id) },
            onDeleteClick = { note ->
                val title = note.title.ifEmpty { "Untitled" }
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.delete_confirm_title, title))
                    .setMessage(getString(R.string.delete_confirm_message, title))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        NoteStorage.deleteNote(this, note.id)
                        updateWidget()
                        refreshList()
                    }
                    .show()
            }
        )

        binding.recyclerNotes.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerNotes.adapter = adapter

        binding.fabAdd.setOnClickListener { openTypePicker() }

        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchBar.visibility == View.VISIBLE
            if (isVisible) {
                binding.searchBar.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        binding.searchBar.visibility = View.GONE
                        binding.searchBar.alpha = 1f
                    }
                    .start()
                binding.searchField.setText("")
                applySort(allNotes)
            } else {
                binding.searchBar.alpha = 0f
                binding.searchBar.visibility = View.VISIBLE
                binding.searchBar.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                binding.searchField.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchField, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                val clear = if (hasText) resources.getDrawable(R.drawable.ic_clear, theme) else null
                binding.searchField.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, clear, null)
                filterNotes(s.toString())
            }
        })

        binding.searchField.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = binding.searchField.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= binding.searchField.right - binding.searchField.paddingEnd - drawableEnd.intrinsicWidth) {
                    binding.searchField.setText("")
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.btnSort.setOnClickListener { showSortSheet() }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // FAB shrink/extend on scroll
        binding.recyclerNotes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) binding.fabAdd.shrink() else if (dy < 0) binding.fabAdd.extend()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.searchField.setText("")
        binding.searchBar.visibility = View.GONE
        binding.noResults.visibility = View.GONE
        updateGreeting()
        refreshList()
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greet = when {
            hour < 12 -> getString(R.string.greeting_morning)
            hour < 17 -> getString(R.string.greeting_afternoon)
            else -> getString(R.string.greeting_evening)
        }
        binding.greeting.text = greet
    }

    private fun showSortSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)
        dialog.setContentView(view)

        val checks = mapOf(
            SortOption.NEWEST to view.findViewById<ImageView>(R.id.checkNewest),
            SortOption.OLDEST to view.findViewById<ImageView>(R.id.checkOldest),
            SortOption.TITLE_AZ to view.findViewById<ImageView>(R.id.checkTitleAz),
            SortOption.TITLE_ZA to view.findViewById<ImageView>(R.id.checkTitleZa),
            SortOption.BY_TYPE to view.findViewById<ImageView>(R.id.checkByType)
        )
        checks[currentSort]?.visibility = View.VISIBLE

        val rows = mapOf(
            SortOption.NEWEST to view.findViewById<LinearLayout>(R.id.sortNewest),
            SortOption.OLDEST to view.findViewById<LinearLayout>(R.id.sortOldest),
            SortOption.TITLE_AZ to view.findViewById<LinearLayout>(R.id.sortTitleAz),
            SortOption.TITLE_ZA to view.findViewById<LinearLayout>(R.id.sortTitleZa),
            SortOption.BY_TYPE to view.findViewById<LinearLayout>(R.id.sortByType)
        )

        for ((option, row) in rows) {
            row?.setOnClickListener {
                currentSort = option
                NoteStorage.setSortOption(this, option.name)
                refreshList()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun openEditor(noteId: Long) {
        val intent = Intent(this, NoteEditActivity::class.java)
        intent.putExtra(NoteEditActivity.EXTRA_NOTE_ID, noteId)
        startActivity(intent)
    }

    private fun openTypePicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_new_note, null)
        dialog.setContentView(view)

        fun createNote(type: NoteType) {
            val note = NoteStorage.addNote(this, "", "", type)
            openEditor(note.id)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.cardPlain).setOnClickListener { createNote(NoteType.PLAIN) }
        view.findViewById<View>(R.id.cardChecklist).setOnClickListener { createNote(NoteType.CHECKLIST) }
        view.findViewById<View>(R.id.cardBullet).setOnClickListener { createNote(NoteType.BULLET) }

        dialog.show()
    }

    private fun refreshList() {
        allNotes = NoteStorage.loadNotes(this)
        val count = allNotes.size
        val countText = resources.getQuantityString(R.plurals.note_count, count, count)
        binding.noteCount.text = getString(R.string.main_subtitle, countText)

        val query = binding.searchField.text.toString()
        if (query.isNotEmpty()) {
            filterNotes(query)
        } else {
            applySort(allNotes)
        }
        binding.emptyState.visibility = if (allNotes.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerNotes.visibility = if (allNotes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun applySort(notes: List<Note>) {
        val sorted = when (currentSort) {
            SortOption.NEWEST -> notes.sortedByDescending { it.id }
            SortOption.OLDEST -> notes.sortedBy { it.id }
            SortOption.TITLE_AZ -> notes.sortedBy { it.title.lowercase() }
            SortOption.TITLE_ZA -> notes.sortedByDescending { it.title.lowercase() }
            SortOption.BY_TYPE -> notes.sortedBy { it.type.ordinal }
        }
        adapter.updateNotes(sorted)
    }

    private fun filterNotes(query: String) {
        if (query.isEmpty()) {
            binding.noResults.visibility = View.GONE
            binding.recyclerNotes.visibility = View.VISIBLE
            applySort(allNotes)
            return
        }
        val filtered = allNotes.filter {
            it.title.contains(query, ignoreCase = true) || it.text.contains(query, ignoreCase = true)
        }
        binding.noResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerNotes.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        applySort(filtered)
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
