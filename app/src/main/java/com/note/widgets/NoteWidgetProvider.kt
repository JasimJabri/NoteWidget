package com.note.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

class NoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.note.widgets.ACTION_PREV"
        const val ACTION_NEXT = "com.note.widgets.ACTION_NEXT"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val notes = NoteStorage.loadNotes(context)
        if (notes.isEmpty()) return

        var index = NoteStorage.getWidgetIndex(context, widgetId)

        when (intent.action) {
            ACTION_PREV -> index = if (index > 0) index - 1 else notes.size - 1
            ACTION_NEXT -> index = if (index < notes.size - 1) index + 1 else 0
        }

        NoteStorage.setWidgetIndex(context, widgetId, index)
        updateSingleWidget(context, AppWidgetManager.getInstance(context), widgetId)
    }

    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_note)
        val notes = NoteStorage.loadNotes(context)

        // Responsive font sizes based on widget height
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val heightDp = maxOf(minH, maxH, 110)
        val widthDp = maxOf(minW, maxW, 250)
        val sizeFactor = (heightDp + widthDp) / 2f
        val titleSp = (sizeFactor * 0.04f).coerceIn(13f, 22f)
        val bodySp = (sizeFactor * 0.05f).coerceIn(14f, 26f)
        val indicatorSp = (sizeFactor * 0.03f).coerceIn(11f, 16f)

        views.setTextViewTextSize(R.id.widgetTitle, android.util.TypedValue.COMPLEX_UNIT_SP, titleSp)
        views.setTextViewTextSize(R.id.widgetNoteText, android.util.TypedValue.COMPLEX_UNIT_SP, bodySp)
        views.setTextViewTextSize(R.id.widgetPageIndicator, android.util.TypedValue.COMPLEX_UNIT_SP, indicatorSp)

        if (notes.isEmpty()) {
            views.setTextViewText(R.id.widgetTitle, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widgetNoteText, context.getString(R.string.widget_placeholder))
            views.setTextViewText(R.id.widgetPageIndicator, "")
        } else {
            var index = NoteStorage.getWidgetIndex(context, widgetId)
            if (index >= notes.size) index = 0
            NoteStorage.setWidgetIndex(context, widgetId, index)

            val note = notes[index]
            views.setTextViewText(R.id.widgetTitle, note.title.ifEmpty { "Untitled" })
            views.setTextViewText(R.id.widgetNoteText, formatForWidget(note))
            views.setTextViewText(R.id.widgetPageIndicator, "${index + 1} / ${notes.size}")
        }

        views.setOnClickPendingIntent(R.id.btnPrev, makePendingIntent(context, widgetId, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.btnNext, makePendingIntent(context, widgetId, ACTION_NEXT))

        val openIntent = if (notes.isNotEmpty()) {
            var index = NoteStorage.getWidgetIndex(context, widgetId)
            if (index >= notes.size) index = 0
            Intent(context, NoteEditActivity::class.java).apply {
                putExtra(NoteEditActivity.EXTRA_NOTE_ID, notes[index].id)
            }
        } else {
            Intent(context, MainActivity::class.java)
        }
        val openPending = PendingIntent.getActivity(
            context, widgetId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetNoteText, openPending)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun makePendingIntent(context: Context, widgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, NoteWidgetProvider::class.java).apply {
            this.action = action
            putExtra(EXTRA_WIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context, widgetId * 10 + if (action == ACTION_PREV) 1 else 2,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatForWidget(note: Note): String {
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
