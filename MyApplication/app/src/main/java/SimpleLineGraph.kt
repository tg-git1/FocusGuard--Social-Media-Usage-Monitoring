package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SimpleLineGraph @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Paint for the curve line
    private val linePaint = Paint().apply {
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    // Paint for the fill area
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for the Y-axis labels (0m, 60m)
    private val textPaint = Paint().apply {
        color = Color.parseColor("#78909C")
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    var dataPoints: List<Float> = listOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()

        // REDUCED PADDING: Since we removed the text below, we don't need 60f space anymore.
        val paddingBottom = 20f
        val paddingLeft = 80f

        // 1. Setup Gradients
        linePaint.shader = LinearGradient(0f, 0f, width, 0f,
            intArrayOf(Color.parseColor("#93B88A"), Color.parseColor("#4DB6AC")),
            null, Shader.TileMode.CLAMP)

        fillPaint.shader = LinearGradient(0f, 0f, 0f, height,
            intArrayOf(Color.parseColor("#4493B88A"), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP)

        // 2. Calculate Scaling
        val maxVal = dataPoints.maxOrNull() ?: 100f
        val scaleY = (height - paddingBottom - 20) / (if (maxVal == 0f) 1f else maxVal)
        val stepX = (width - paddingLeft - 20) / (if (dataPoints.size > 1) dataPoints.size - 1 else 1)

        val path = Path()

        // Move to start
        val firstX = paddingLeft
        val firstY = height - paddingBottom - (dataPoints[0] * scaleY)
        path.moveTo(firstX, firstY)

        // 3. Draw the Curve (Bezier)
        for (i in 0 until dataPoints.size - 1) {
            val startX = paddingLeft + (i * stepX)
            val startY = height - paddingBottom - (dataPoints[i] * scaleY)
            val endX = paddingLeft + ((i + 1) * stepX)
            val endY = height - paddingBottom - (dataPoints[i+1] * scaleY)

            val controlX1 = (startX + endX) / 2
            val controlY1 = startY
            val controlX2 = (startX + endX) / 2
            val controlY2 = endY

            path.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)

            // --- X-AXIS TEXT REMOVED COMPLETELY FROM HERE ---
        }

        // 4. Draw the Fill
        val fillPath = Path(path)
        fillPath.lineTo(paddingLeft + ((dataPoints.size - 1) * stepX), height - paddingBottom)
        fillPath.lineTo(paddingLeft, height - paddingBottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // 5. Draw the Line
        canvas.drawPath(path, linePaint)

        // 6. Draw Y-Axis Labels Only (Minutes)
        // Top label (e.g., "45m")
        canvas.drawText("${maxVal.toInt()}m", 40f, 40f, textPaint)
        // Bottom label ("0m")
        canvas.drawText("0m", 40f, height - paddingBottom, textPaint)
    }
}