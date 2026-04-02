package com.note.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog

class ColorPickerHelper(
    private val context: Context,
    private val title: String,
    private val presets: IntArray,
    private val showAlpha: Boolean,
    private val initialColor: Int,
    private val recentColors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) {
    private val hsv = FloatArray(3)
    private var alpha = 255
    private var updatingFromCode = false

    fun show() {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)

        val titleView = view.findViewById<TextView>(R.id.pickerTitle)
        val preview = view.findViewById<View>(R.id.colorPreviewBox)
        val hexInput = view.findViewById<EditText>(R.id.hexInput)
        val satValBox = view.findViewById<SatValView>(R.id.satValBox)
        val hueBar = view.findViewById<HueBarView>(R.id.hueBar)
        val alphaSlider = view.findViewById<SeekBar>(R.id.alphaSlider)
        val opacityLabel = view.findViewById<View>(R.id.opacityLabel)
        val presetGrid = view.findViewById<LinearLayout>(R.id.presetGrid)
        val recentLabel = view.findViewById<View>(R.id.recentLabelPicker)
        val recentRow = view.findViewById<LinearLayout>(R.id.recentRow)
        val btnSelect = view.findViewById<View>(R.id.btnSelect)

        titleView.text = title

        // Parse initial color
        alpha = Color.alpha(initialColor)
        Color.colorToHSV(initialColor or (0xFF shl 24), hsv)

        if (showAlpha) {
            alphaSlider.visibility = View.VISIBLE
            opacityLabel.visibility = View.VISIBLE
        }

        fun currentColor(): Int {
            val rgb = Color.HSVToColor(hsv)
            return Color.argb(if (showAlpha) alpha else 255, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        }

        fun updateUI() {
            updatingFromCode = true
            val color = currentColor()
            val dp = context.resources.displayMetrics.density
            preview.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * dp
                setColor(color)
                if (Color.alpha(color) < 128)
                    setStroke((2 * dp).toInt(), 0xFFDDDDDD.toInt())
            }
            satValBox.hue = hsv[0]
            satValBox.setColor(hsv[1], hsv[2])
            hueBar.setHue(hsv[0])
            alphaSlider.progress = alpha
            hexInput.setText(
                if (showAlpha) String.format("#%08X", color)
                else String.format("#%06X", color and 0xFFFFFF)
            )
            updatingFromCode = false
        }

        // SatVal box changes
        satValBox.onColorChanged = {
            if (!updatingFromCode) {
                hsv[1] = satValBox.sat
                hsv[2] = satValBox.value
                updateUI()
            }
        }

        // Hue bar changes
        hueBar.onHueChanged = {
            if (!updatingFromCode) {
                hsv[0] = hueBar.hue
                satValBox.hue = hsv[0]
                updateUI()
            }
        }

        // Alpha slider
        alphaSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !updatingFromCode) {
                    alpha = progress
                    updateUI()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Hex input
        hexInput.setOnEditorActionListener { _, _, _ ->
            val hex = hexInput.text.toString()
            try {
                val parsed = Color.parseColor(hex)
                alpha = Color.alpha(parsed)
                Color.colorToHSV(parsed or (0xFF shl 24), hsv)
                updateUI()
            } catch (_: Exception) {}
            false
        }

        // Presets
        fun applyColor(color: Int) {
            if (Color.alpha(color) == 0) {
                alpha = 0
                hsv[0] = 0f; hsv[1] = 0f; hsv[2] = 1f
            } else {
                alpha = Color.alpha(color)
                Color.colorToHSV(color or (0xFF shl 24), hsv)
            }
            updateUI()
        }

        buildPresetGrid(presetGrid, presets) { applyColor(it) }

        if (recentColors.isNotEmpty()) {
            recentLabel.visibility = View.VISIBLE
            buildRecentRow(recentRow, recentColors) { applyColor(it) }
        }

        btnSelect.setOnClickListener {
            onColorSelected(currentColor())
            dialog.dismiss()
        }

        updateUI()
        dialog.show()
    }

    private fun buildPresetGrid(container: LinearLayout, colors: IntArray, onClick: (Int) -> Unit) {
        container.removeAllViews()
        val dp = context.resources.displayMetrics.density
        val size = (36 * dp).toInt()
        val margin = (5 * dp).toInt()
        val colCount = 5

        for (rowStart in colors.indices step colCount) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (6 * dp).toInt() }
            }
            for (i in rowStart until minOf(rowStart + colCount, colors.size)) {
                val color = colors[i]
                val isTransparent = Color.alpha(color) == 0
                val frame = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginStart = margin; marginEnd = margin
                    }
                }
                val circle = View(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        if (isTransparent) {
                            setColor(0xFFEEEEEE.toInt())
                            setStroke((2 * dp).toInt(), 0xFFCCCCCC.toInt())
                        } else {
                            setColor(color)
                            val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
                            if (lum > 0.85 || Color.alpha(color) < 128)
                                setStroke((2 * dp).toInt(), 0xFFDDDDDD.toInt())
                        }
                    }
                }
                frame.addView(circle)
                if (isTransparent) {
                    val slash = TextView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER
                        )
                        text = "\u2298"
                        setTextColor(0xFF999999.toInt())
                        textSize = 14f
                        gravity = Gravity.CENTER
                    }
                    frame.addView(slash)
                }
                frame.setOnClickListener { onClick(color) }
                row.addView(frame)
            }
            container.addView(row)
        }
    }

    private fun buildRecentRow(container: LinearLayout, colors: List<Int>, onClick: (Int) -> Unit) {
        container.removeAllViews()
        val dp = context.resources.displayMetrics.density
        val size = (36 * dp).toInt()
        val margin = (5 * dp).toInt()

        for (color in colors) {
            val frame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = margin; marginEnd = margin
                }
            }
            val circle = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke((1 * dp).toInt(), 0xFFDDDDDD.toInt())
                }
            }
            frame.addView(circle)
            frame.setOnClickListener { onClick(color) }
            container.addView(frame)
        }
    }
}
