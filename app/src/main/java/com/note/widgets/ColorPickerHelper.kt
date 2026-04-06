package com.note.widgets

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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

    @SuppressLint("InflateParams")
    fun show() {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)

        // Expand bottom sheet fully so layout_weight works
        dialog.behavior.peekHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

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
            preview.background = makeCheckerPreview(color)
            preview.clipToOutline = true
            preview.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
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
        val colCount = 5

        for (rowStart in colors.indices step colCount) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (6 * dp).toInt() }
            }

            for (i in rowStart until minOf(rowStart + colCount, colors.size)) {
                val color = colors[i]
                val isTransparent = Color.alpha(color) == 0

                // Wrapper with weight for even spacing
                val wrapper = LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    gravity = Gravity.CENTER
                }

                val frame = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size)
                }
                if (isTransparent) {
                    frame.addView(makeCheckerView(size))
                } else {
                    val circle = View(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                            val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
                            if (lum > 0.85 || Color.alpha(color) < 128)
                                setStroke((2 * dp).toInt(), 0xFFDDDDDD.toInt())
                        }
                    }
                    frame.addView(circle)
                }
                frame.setOnClickListener { onClick(color) }
                wrapper.addView(frame)
                row.addView(wrapper)
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

    private fun makeCheckerBitmap(size: Int, squareSize: Int = 8): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val light = Paint().apply { color = 0xFFFFFFFF.toInt() }
        val dark = Paint().apply { color = 0xFFCCCCCC.toInt() }
        val cols = size / squareSize + 1
        for (r in 0 until cols) {
            for (c in 0 until cols) {
                val p = if ((r + c) % 2 == 0) light else dark
                canvas.drawRect(
                    (c * squareSize).toFloat(), (r * squareSize).toFloat(),
                    ((c + 1) * squareSize).toFloat(), ((r + 1) * squareSize).toFloat(), p
                )
            }
        }
        return bmp
    }

    private fun makeCheckerView(size: Int): View {
        val dp = context.resources.displayMetrics.density
        val sq = (6 * dp).toInt()
        return View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val bmp = makeCheckerBitmap(size, sq)
            val checker = BitmapDrawable(context.resources, bmp)
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            background = checker
        }
    }

    private fun makeCheckerPreview(color: Int): android.graphics.drawable.Drawable {
        val dp = context.resources.displayMetrics.density
        val sq = (8 * dp).toInt()
        val bmp = makeCheckerBitmap(200, sq)
        val checker = BitmapDrawable(context.resources, bmp)
        val colorLayer = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        return LayerDrawable(arrayOf(checker, colorLayer))
    }
}
