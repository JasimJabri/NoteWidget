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
- **MainActivity** — modern main screen with:
  - Time-based greeting header ("Good Morning ☀️" / "Good Afternoon 🌤️" / "Good Evening 🌙")
  - Subtitle: "Note Widget · 3 notes"
  - Search icon (toggles pill-shaped search bar with animation, opens keyboard)
  - Sort icon (opens bottom sheet with 5 sort options, persisted in SharedPreferences)
  - Settings icon (opens SettingsActivity)
  - 2-column GridLayoutManager for note cards
  - Extended FAB "New Note" — shrinks to icon on scroll down, extends on scroll up
  - FAB opens bottom sheet type picker (not a separate activity)
  - Empty state with note icon + title + subtitle
  - "No results found" when search returns empty
  - Long-press on card to delete (with confirmation dialog showing note title)
  - Search clears on resume
- **NoteEditActivity** — edit a note. Title (top, textCapWords) + type badge (inline) + back button (top right). Back always navigates to MainActivity (works from widget too). Auto-saves via TextWatcher. Supports 3 modes:
  - Plain: free-form EditText
  - Checklist: dynamic rows with CheckBox + EditText + delete. Checked items get strikethrough. Stored as `[x] item` / `[ ] item`. Delete requires confirmation.
  - Bullet: dynamic rows with `•` prefix + EditText + delete. Delete requires confirmation.
  - "+ Add item" button inside the scrollable list at the end
- **SettingsActivity** — settings screen with:
  - Font Size: Material slider (Small/Medium/Large/Extra Large)
  - Font Color: tappable row with swatch → opens ColorPickerHelper dialog
  - Widget Background: tappable row with swatch → opens ColorPickerHelper dialog with opacity

### Color Picker
- **ColorPickerHelper** — reusable bottom sheet color picker dialog with:
  - 2D SatValView (custom View) — tap/drag for saturation (X) and brightness (Y)
  - HueBarView (custom View) — horizontal rainbow bar for hue selection
  - Opacity SeekBar (optional, for widget background)
  - Hex input field
  - Preset color grid (5 columns)
  - Recent colors row
  - Select button
- **SatValView** — custom View rendering white→hue horizontal gradient + transparent→black vertical gradient with circle indicator
- **HueBarView** — custom View rendering rainbow gradient with circle indicator

### Data
- **NoteStorage** (object) — CRUD operations on notes stored as JSON array in SharedPreferences (`note_prefs`). Also stores per-widget index, sort preference, font size, font color, widget background color, and recent background colors.
- **Note** data class — `id: Long, title: String, text: String, type: NoteType`
- **NoteType** enum — `PLAIN, CHECKLIST, BULLET`

### Widget
- **NoteWidgetProvider** — AppWidgetProvider showing current note with left/right triangle arrows (◂ ▸ as TextViews) to navigate between notes. Title is dynamic (note title). Formats checklist/bullet with unicode symbols. Tap note text opens NoteEditActivity for the current note (falls back to MainActivity if no notes).
- Widget has responsive font sizes — scales based on average of widget height and width using `setTextViewTextSize()` programmatically. `onAppWidgetOptionsChanged` triggers re-render on resize.
- Font multipliers: title 0.05, body 0.06, indicator 0.035, arrows = title × 1.3
- User font size preference applied as multiplier (0.8x/1.0x/1.2x/1.4x) on title and body
- User font color applied to widget body text
- User background color applied via `setInt(widgetRoot, "setBackgroundColor", color)`
- Widget body has 1.3x line spacing and 8dp horizontal / 6dp top padding.
- Widget metadata: `res/xml/note_widget_info.xml` — 250x110dp min, resizable, 30min update.

### Adapter
- **NoteAdapter** — RecyclerView adapter for 2-column grid. Each card shows type icon, colored badge, title (2 lines), preview text (4 lines, 1.3x line spacing). Ripple touch feedback. Tap to open, long-press to delete. Type-specific card background colors. Cards have 20dp corners, 0 elevation, 140dp min height.

### Bottom Sheets
- **New Note picker** (`bottom_sheet_new_note.xml`) — sleek cards with type icons, colored accents, descriptions, chevrons. Creates note and opens editor.
- **Sort options** (`bottom_sheet_sort.xml`) — 5 sort options with checkmark on active. Persisted.

## Color Scheme (per note type)
- Plain Text: warm cream (#FFF8F0 card, #F0E4D0 badge, #8A7350 text)
- Checklist: soft blue (#F0F4FF card, #D6E4FF badge, #4A6FA5 text)
- Bullet List: soft green (#F0FAF0 card, #D4EDDA badge, #3D7A4A text)

Applied to: note list cards, edit screen background, bottom sheet type picker cards, badges.

## Font Colors (presets)
Black, Light Gray, Yellow, Green, Blue, Red, Purple, Orange, Brown, Blue Gray

## Widget Background Presets
Transparent, White, Light Gray, Warm Yellow, Mint Green, Light Blue, Lavender, Blush Pink, Semi Black, Semi Dark Gray, Semi Navy, Semi Maroon, Black, Dark Gray, Dark Blue, Dark Slate

## UI Details
- Background: #FAFAFA everywhere (main, edit, status bar)
- Light status bar icons
- Pill-shaped search bar (#F2F2F7 bg, 28dp radius) — hidden by default, animated fade in/out
- Extended FAB: #6750A4 purple, 28dp corner radius, white text/icon
- Badge: 12dp rounded corners
- Delete confirmation shows note title: Delete "Packing"?
- Search has clear (X) icon when text is present
- Header icons (settings, sort, search) aligned to top of row
- Edit screen title aligned with main screen title (matching paddingTop, includeFontPadding=false)

## Sort Options
1. Newest first (default)
2. Oldest first
3. Title A → Z
4. Title Z → A
5. By type

## Settings
- Font Size: 4 levels via slider (14sp/18sp/22sp/26sp), persisted
- Font Color: 10 presets + custom via HSV picker, persisted
- Widget Background: 16 presets (incl. transparent + semi-transparent) + custom via HSV picker with opacity, persisted
- Recent background colors tracked (up to 5)

## Key Decisions
- Auto-save (no save button) — TextWatcher on all editable fields
- Widget updates immediately on every save via ACTION_APPWIDGET_UPDATE broadcast
- Checklist items stored with `[x] ` / `[ ] ` prefix in the text field
- Delete requires confirmation via MaterialAlertDialog (notes, checklist items, bullet items)
- Theme is Light-only with #FAFAFA windowBackground
- App icon uses vector drawable (`@drawable/ic_launcher_foreground`) — no mipmap PNGs
- NoteTypePickerActivity removed — replaced by bottom sheet in MainActivity
- Widget arrows are TextViews (not ImageButtons) for dynamic sizing
- Widget font sizing uses average of (height + width) / 2 as sizeFactor
- Multi-layout widget approach was abandoned — programmatic `setTextViewTextSize` is more reliable
- Color picker uses custom Views (SatValView, HueBarView) instead of Compose (app is View-based)

## Known Issues / Past Bugs Fixed
- `setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless)` crashes — must resolve theme attribute via TypedValue first
- mipmap/ic_launcher not found — switched to drawable vector reference in manifest
- EditText has internal padding even with padding="0dp" — use includeFontPadding="false" + minHeight="0dp"
- `text` parameter shadowing in Kotlin `apply` block — use `this.text` explicitly
- checkerboard.xml with percentage values crashes AAPT — removed, using ⊘ symbol instead

## Git
- Remote: https://github.com/JasimJabri/NoteWidget.git
- Branch: main
