package com.example.ghostlink

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private var isEraserMode = false

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.color = Color.BLACK
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 8f
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    // Метод переключения между кистью и ластиком
    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
        if (isEraserMode) {
            // Режим CLEAR "вырезает" пиксели до прозрачности
            drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            drawPaint.strokeWidth = 40f
        } else {
            drawPaint.xfermode = null
            drawPaint.strokeWidth = 8f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvasBitmap = bitmap
            drawCanvas = Canvas(bitmap)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, canvasPaint)
        }

        // Если включен ластик, рисуем серый превью-путь, чтобы видеть, где стираем
        if (isEraserMode) {
            val previewPaint = Paint(drawPaint).apply {
                xfermode = null
                color = Color.LTGRAY
            }
            canvas.drawPath(drawPath, previewPaint)
        } else {
            canvas.drawPath(drawPath, drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                // При отпускании пальца переносим путь на основной холст
                drawCanvas?.drawPath(drawPath, drawPaint)
                drawPath.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun clearCanvas() {
        drawCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    fun getCompressedByteArray(): ByteArray? {
        val bitmap = canvasBitmap ?: return null
        val outputStream = ByteArrayOutputStream()
        return if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
            outputStream.toByteArray()
        } else {
            null
        }
    }

    fun getBitmap(): Bitmap? = canvasBitmap
}