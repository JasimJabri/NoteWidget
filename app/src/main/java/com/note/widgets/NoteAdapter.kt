package com.note.widgets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.note.widgets.databinding.ItemNoteBinding

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val ctx = holder.binding.root.context

        holder.binding.noteTitle.text = note.title.ifEmpty { "Untitled" }
        holder.binding.noteTypeBadge.text = when (note.type) {
            NoteType.PLAIN -> ctx.getString(R.string.type_plain)
            NoteType.CHECKLIST -> ctx.getString(R.string.type_checklist)
            NoteType.BULLET -> ctx.getString(R.string.type_bullet)
        }
        holder.binding.noteText.text = formatPreview(note)

        // Apply type-specific colors
        val (cardColor, badgeColor, badgeTextColor) = when (note.type) {
            NoteType.PLAIN -> Triple(R.color.plain_card, R.color.plain_badge, R.color.plain_badge_text)
            NoteType.CHECKLIST -> Triple(R.color.checklist_card, R.color.checklist_badge, R.color.checklist_badge_text)
            NoteType.BULLET -> Triple(R.color.bullet_card, R.color.bullet_badge, R.color.bullet_badge_text)
        }
        holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, cardColor))
        holder.binding.noteTypeBadge.background.setTint(ContextCompat.getColor(ctx, badgeColor))
        holder.binding.noteTypeBadge.setTextColor(ContextCompat.getColor(ctx, badgeTextColor))

        holder.binding.root.setOnClickListener { onNoteClick(note) }
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(note) }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    private fun formatPreview(note: Note): String {
        if (note.text.isEmpty()) return "Empty note"
        return when (note.type) {
            NoteType.PLAIN -> note.text
            NoteType.CHECKLIST -> note.text.split("\n").joinToString("\n") { line ->
                when {
                    line.startsWith("[x] ") -> "☑ " + line.removePrefix("[x] ")
                    line.startsWith("[ ] ") -> "☐ " + line.removePrefix("[ ] ")
                    else -> line
                }
            }
            NoteType.BULLET -> note.text.split("\n").joinToString("\n") { "• $it" }
        }
    }
}
