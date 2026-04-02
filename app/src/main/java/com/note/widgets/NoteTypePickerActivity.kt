package com.note.widgets

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.note.widgets.databinding.ActivityNoteTypePickerBinding

class NoteTypePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteTypePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteTypePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardPlain.setOnClickListener { openEditor(NoteType.PLAIN) }
        binding.cardChecklist.setOnClickListener { openEditor(NoteType.CHECKLIST) }
        binding.cardBullet.setOnClickListener { openEditor(NoteType.BULLET) }
    }

    private fun openEditor(type: NoteType) {
        val note = NoteStorage.addNote(this, "", "", type)
        val intent = Intent(this, NoteEditActivity::class.java)
        intent.putExtra(NoteEditActivity.EXTRA_NOTE_ID, note.id)
        startActivity(intent)
        finish()
    }
}
