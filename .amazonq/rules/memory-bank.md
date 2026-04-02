# Note Widget – Quick Notes | Project Memory Bank

## Overview
Android app (Kotlin) called "Note Widget – Quick Notes" that supports multiple notes with a home screen widget.

## Package
`com.note.widgets`

## Tech Stack
- Kotlin, minSdk 24, targetSdk 34, compileSdk 34
- ViewBinding (no findViewById)
- SharedPreferences with JSON for storage (no Room/SQLite)
- Material 3 Light theme (no dark mode)
- No heavy frameworks — clean minimal architecture

## Architecture
Single-module app with these key files:

### Activities
- **MainActivity** — note list (RecyclerView + FAB). FAB opens NoteTypePickerActivity. Delete shows confirmation dialog.
- **NoteTypePickerActivity** — pick note format: Plain Text, Checklist, or Bullet List. Creates the note then opens editor.
- **NoteEditActivity** — edit a note. Title (top, textCapWords) + back button (top right). Type badge below title. Auto-saves via TextWatcher. Supports 3 modes:
  - Plain: free-form EditText
  - Checklist: dynamic rows with CheckBox + EditText + delete. Checked items get strikethrough. Stored as `[x] item` / `[ ] item`.
  - Bullet: dynamic rows with `•` prefix + EditText + delete.

### Data
- **NoteStorage** (object) — CRUD operations on notes stored as JSON array in SharedPreferences (`note_prefs`). Also stores per-widget index.
- **Note** data class — `id: Long, title: String, text: String, type: NoteType`
- **NoteType** enum — `PLAIN, CHECKLIST, BULLET`

### Widget
- **NoteWidgetProvider** — AppWidgetProvider showing current note with left/right chevron arrows to navigate between notes. Title is dynamic (note title). Formats checklist/bullet with unicode symbols. Tap note text opens NoteEditActivity for the current note (falls back to MainActivity if no notes).
- Widget has responsive font sizes — scales based on average of widget height and width using `setTextViewTextSize()` programmatically. `onAppWidgetOptionsChanged` triggers re-render on resize.
- Widget metadata: `res/xml/note_widget_info.xml` — 250x110dp min, resizable, 30min update.

### Adapter
- **NoteAdapter** — RecyclerView adapter. Each card shows title, preview text (formatted per type), type badge (colored), delete button. Type-specific card background colors.

## Color Scheme (per note type)
- Plain Text: warm cream (#FFF8F0 card, #F0E4D0 badge, #8A7350 text)
- Checklist: soft blue (#F0F4FF card, #D6E4FF badge, #4A6FA5 text)
- Bullet List: soft green (#F0FAF0 card, #D4EDDA badge, #3D7A4A text)

Applied to: note list cards, edit screen background, type picker cards, badges.

## Key Decisions
- Auto-save (no save button) — TextWatcher on all editable fields
- Widget updates immediately on every save via ACTION_APPWIDGET_UPDATE broadcast
- Checklist items stored with `[x] ` / `[ ] ` prefix in the text field (simple, no extra data structure)
- Delete button requires confirmation via MaterialAlertDialog
- Theme is Light-only with explicit white windowBackground to avoid dark mode issues
- App icon uses vector drawable (`@drawable/ic_launcher_foreground`) — no mipmap PNGs
- Widget font sizing uses average of (height + width) / 2 as sizeFactor, with multipliers: title 0.04, body 0.05, indicator 0.03
- Multi-layout widget approach (small/medium/large XMLs) was abandoned — programmatic `setTextViewTextSize` is more reliable across launchers

## Known Issues / Past Bugs Fixed
- `setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)` crashes — must resolve theme attribute via TypedValue first
- mipmap/ic_launcher not found — switched to drawable vector reference in manifest
