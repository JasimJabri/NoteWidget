package com.note.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.note.widgets.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        val FONT_SIZES = floatArrayOf(14f, 18f, 22f, 26f)
        val ITEM_FONT_SIZES = floatArrayOf(13f, 16f, 20f, 24f)
        val FONT_COLORS = intArrayOf(
            0xFF1A1A1A.toInt(),
            0xFFE0E0E0.toInt(),
            0xFFFFEB3B.toInt(),
            0xFF4CAF50.toInt(),
            0xFF2196F3.toInt(),
            0xFFF44336.toInt(),
            0xFF9C27B0.toInt(),
            0xFFFF9800.toInt(),
            0xFF795548.toInt(),
            0xFF607D8B.toInt()
        )
        val BG_PRESETS = intArrayOf(
            0x00000000,
            0xFFFFFFFF.toInt(),
            0xFFF5F5F5.toInt(),
            0xFFFFF8E1.toInt(),
            0xFFE8F5E9.toInt(),
            0xFFE3F2FD.toInt(),
            0xFFF3E5F5.toInt(),
            0xFFFFEBEE.toInt(),
            0x80000000.toInt(),
            0x80333333.toInt(),
            0x80000033.toInt(),
            0x80330000.toInt(),
            0xFF000000.toInt(),
            0xFF333333.toInt(),
            0xFF1A1A2E.toInt(),
            0xFF263238.toInt()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup header
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        val headerSubtitle = findViewById<TextView>(R.id.headerSubtitle)
        val headerIcons = findViewById<LinearLayout>(R.id.headerIcons)

        headerTitle.text = getString(R.string.settings)
        headerSubtitle.text = getString(R.string.settings_subtitle)

        val btnBack = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (48 * resources.displayMetrics.density).toInt(),
                (48 * resources.displayMetrics.density).toInt()
            )
            setImageResource(R.drawable.ic_back)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setPadding((4 * resources.displayMetrics.density).toInt(), 0, 0, 0)
        }
        headerIcons.addView(btnBack)
        btnBack.setOnClickListener { finish() }

        // Font size slider
        binding.fontSlider.value = NoteStorage.getFontSize(this).toFloat()
        binding.fontSlider.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            NoteStorage.setFontSize(this, value.toInt())
            updateWidget()
        })

        // Font color — show current, tap to open picker
        updateFontColorPreview()
        binding.fontColorRow.setOnClickListener {
            val currentColor = FONT_COLORS[NoteStorage.getFontColor(this)]
            ColorPickerHelper(
                context = this,
                title = getString(R.string.font_color),
                presets = FONT_COLORS,
                showAlpha = false,
                initialColor = currentColor,
                recentColors = emptyList()
            ) { color ->
                val index = FONT_COLORS.indexOf(color)
                if (index >= 0) {
                    NoteStorage.setFontColor(this, index)
                } else {
                    NoteStorage.setFontColor(this, 0)
                }
                updateFontColorPreview()
                updateWidget()
            }.show()
        }

        // Widget background — show current, tap to open picker
        updateBgColorPreview()
        binding.bgColorRow.setOnClickListener {
            val currentBg = NoteStorage.getWidgetBgColor(this)
            ColorPickerHelper(
                context = this,
                title = getString(R.string.widget_bg),
                presets = BG_PRESETS,
                showAlpha = true,
                initialColor = currentBg,
                recentColors = NoteStorage.getRecentBgColors(this)
            ) { color ->
                NoteStorage.setWidgetBgColor(this, color)
                updateBgColorPreview()
                updateWidget()
            }.show()
        }

    }

    private fun updateFontColorPreview() {
        val color = FONT_COLORS[NoteStorage.getFontColor(this)]
        binding.fontColorSwatch.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun updateBgColorPreview() {
        val color = NoteStorage.getWidgetBgColor(this)
        val dp = resources.displayMetrics.density
        binding.bgColorSwatch.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (Color.alpha(color) < 128 || color == 0xFFFFFFFF.toInt())
                setStroke((2 * dp).toInt(), 0xFFDDDDDD.toInt())
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
