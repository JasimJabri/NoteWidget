package com.note.widgets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.note.widgets.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    companion object {
        val FONT_SIZES = floatArrayOf(14f, 18f, 22f, 26f)
        val ITEM_FONT_SIZES = floatArrayOf(13f, 16f, 20f, 24f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val current = NoteStorage.getFontSize(this)
        binding.fontSlider.value = current.toFloat()
        updatePreview(current)

        binding.fontSlider.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            val size = value.toInt()
            NoteStorage.setFontSize(this, size)
            updatePreview(size)
        })

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun updatePreview(sizeIndex: Int) {
        binding.fontPreview.textSize = FONT_SIZES[sizeIndex]
    }
}
