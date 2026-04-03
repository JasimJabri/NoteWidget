package com.note.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.note.widgets.databinding.ActivityNoteEditBinding

class NoteEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditBinding
    private var noteId: Long = -1L
    private var noteType: NoteType = NoteType.PLAIN

    private lateinit var addItemBtn: com.google.android.material.button.MaterialButton

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        private const val SEPARATOR = "\n"
        private const val CHECKED_PREFIX = "[x] "
        private const val UNCHECKED_PREFIX = "[ ] "
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        val note = NoteStorage.loadNotes(this).find { it.id == noteId } ?: return finish()

        noteType = note.type
        binding.editTitle.setText(note.title)

        binding.typeBadge.text = when (noteType) {
            NoteType.PLAIN -> getString(R.string.type_plain)
            NoteType.CHECKLIST -> getString(R.string.type_checklist)
            NoteType.BULLET -> getString(R.string.type_bullet)
        }

        val (bgColor, badgeColor, badgeTextColor) = when (noteType) {
            NoteType.PLAIN -> Triple(R.color.plain_card, R.color.plain_badge, R.color.plain_badge_text)
            NoteType.CHECKLIST -> Triple(R.color.checklist_card, R.color.checklist_badge, R.color.checklist_badge_text)
            NoteType.BULLET -> Triple(R.color.bullet_card, R.color.bullet_badge, R.color.bullet_badge_text)
        }
        binding.root.setBackgroundColor(ContextCompat.getColor(this, bgColor))
        binding.typeBadge.setTextColor(ContextCompat.getColor(this, badgeTextColor))
        binding.typeBadge.background.setTint(ContextCompat.getColor(this, badgeColor))

        when (noteType) {
            NoteType.PLAIN -> setupPlainMode(note.text)
            NoteType.CHECKLIST -> setupListMode(note.text, isChecklist = true)
            NoteType.BULLET -> setupListMode(note.text, isChecklist = false)
        }

        binding.editTitle.addTextChangedListener(autoSaveWatcher)

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        applyFontSize()
    }

    private fun applyFontSize() {
        val sizeIndex = NoteStorage.getFontSize(this)
        val bodySize = SettingsActivity.FONT_SIZES[sizeIndex]
        binding.editNote.textSize = bodySize

        val colorIndex = NoteStorage.getFontColor(this)
        val fontColor = SettingsActivity.FONT_COLORS[colorIndex]
        binding.editNote.setTextColor(fontColor)
    }

    private fun setupPlainMode(text: String) {
        binding.editNote.visibility = View.VISIBLE
        binding.editNote.setText(text)
        binding.editNote.addTextChangedListener(autoSaveWatcher)
    }

    private fun setupListMode(text: String, isChecklist: Boolean) {
        binding.itemsScroll.visibility = View.VISIBLE

        val items = if (text.isEmpty()) mutableListOf("") else text.split(SEPARATOR).toMutableList()
        for (item in items) {
            addItemRow(item, isChecklist)
        }

        addItemBtn = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            this.text = getString(R.string.add_item)
            this.textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            setOnClickListener {
                addItemRow("", isChecklist)
                val last = binding.itemsContainer.getChildAt(binding.itemsContainer.childCount - 2)
                last.findViewById<EditText>(R.id.itemEdit)?.requestFocus()
            }
        }
        binding.itemsContainer.addView(addItemBtn)
    }

    private fun addItemRow(text: String, isChecklist: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        if (isChecklist) {
            val isChecked = text.startsWith(CHECKED_PREFIX)
            val cleanText = text.removePrefix(CHECKED_PREFIX).removePrefix(UNCHECKED_PREFIX)

            val checkBox = CheckBox(this).apply {
                this.isChecked = isChecked
                setOnCheckedChangeListener { _, checked ->
                    val edit = row.findViewById<EditText>(R.id.itemEdit)
                    if (checked) {
                        edit.paintFlags = edit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        edit.paintFlags = edit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    saveFromItems()
                }
            }
            row.addView(checkBox)

            val editText = createItemEditText(cleanText)
            if (isChecked) editText.paintFlags = editText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            row.addView(editText)
        } else {
            val bullet = TextView(this).apply {
                this.text = "  •  "
                textSize = 18f
                setTextColor(SettingsActivity.FONT_COLORS[NoteStorage.getFontColor(context)])
            }
            row.addView(bullet)

            val editText = createItemEditText(text)
            row.addView(editText)
        }

        val deleteBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_delete)
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { marginStart = 8 }
            setOnClickListener {
                MaterialAlertDialogBuilder(this@NoteEditActivity)
                    .setTitle(getString(R.string.delete_item_title))
                    .setMessage(getString(R.string.delete_item_message))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        binding.itemsContainer.removeView(row)
                        saveFromItems()
                    }
                    .show()
            }
        }
        row.addView(deleteBtn)

        val insertIndex = binding.itemsContainer.childCount - (if (::addItemBtn.isInitialized) 1 else 0)
        binding.itemsContainer.addView(row, insertIndex)
    }

    private fun createItemEditText(text: String): EditText {
        val sizeIndex = NoteStorage.getFontSize(this)
        val itemSize = SettingsActivity.ITEM_FONT_SIZES[sizeIndex]
        return EditText(this).apply {
            id = R.id.itemEdit
            setText(text)
            textSize = itemSize
            setTextColor(SettingsActivity.FONT_COLORS[NoteStorage.getFontColor(context)])
            setHintTextColor(0xFF888888.toInt())
            hint = getString(R.string.hint_item)
            background = null
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_NEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(autoSaveWatcher)
        }
    }

    private fun saveFromItems() {
        val lines = mutableListOf<String>()
        for (i in 0 until binding.itemsContainer.childCount) {
            val child = binding.itemsContainer.getChildAt(i)
            if (child is LinearLayout) {
                val edit = child.findViewById<EditText>(R.id.itemEdit) ?: continue
                val text = edit.text.toString()
                if (noteType == NoteType.CHECKLIST) {
                    val checkBox = child.getChildAt(0) as CheckBox
                    val prefix = if (checkBox.isChecked) CHECKED_PREFIX else UNCHECKED_PREFIX
                    lines.add(prefix + text)
                } else {
                    lines.add(text)
                }
            }
        }
        NoteStorage.updateNote(this, noteId, binding.editTitle.text.toString(), lines.joinToString(SEPARATOR))
        updateWidget()
    }

    private val autoSaveWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (noteType == NoteType.PLAIN) {
                NoteStorage.updateNote(this@NoteEditActivity, noteId, binding.editTitle.text.toString(), binding.editNote.text.toString())
                updateWidget()
            } else {
                saveFromItems()
            }
        }
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
