package com.mdfchi.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max

/**
 * نمایش گرافیکی نتیجه CutOptimizer.
 * این View فقط Visualization است و هیچ منطق محاسباتی داخل آن قرار ندارد.
 */
class NestingView(
    context: Context,
    private val result: CutOptimizationResult
) : View(context) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = Color.rgb(90, 70, 55)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 48, 43)
        textSize = dp(11f)
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(72, 50, 38)
        textSize = dp(14f)
        isFakeBoldText = true
    }

    /** Palette ثابت و ساده برای تفکیک قطعات؛ رنگ معنای فنی ندارد. */
    private val palette = intArrayOf(
        Color.rgb(255, 224, 190),
        Color.rgb(220, 238, 211),
        Color.rgb(216, 233, 247),
        Color.rgb(244, 216, 224),
        Color.rgb(236, 224, 203),
        Color.rgb(224, 218, 244)
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(dp(280f).toInt())
        // برای هر ورق فضای مستقل رزرو می‌شود؛ ScrollView صفحه، ارتفاع زیاد را مدیریت می‌کند.
        val sheetHeight = dp(310f)
        val desiredHeight = max(dp(80f), result.sheets.size * sheetHeight + dp(20f))
        setMeasuredDimension(width, desiredHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (result.sheets.isEmpty()) {
            canvas.drawText("قطعه قابل جانمایی وجود ندارد.", dp(12f), dp(28f), titlePaint)
            return
        }

        val left = dp(14f)
        val availableWidth = width - dp(28f)
        var top = dp(24f)

        result.sheets.forEach { sheet ->
            // عنوان هر ورق بالای نمودار آن نمایش داده می‌شود.
            canvas.drawText(
                "ورق ${sheet.index} — راندمان ${format(sheet.efficiencyPercent())}%",
                left,
                top,
                titlePaint
            )
            top += dp(16f)

            val ratio = sheet.heightCm / sheet.widthCm
            val maxDiagramHeight = dp(250f)
            var diagramWidth = availableWidth
            var diagramHeight = diagramWidth * ratio.toFloat()
            // اگر نسبت طول ورق باعث ارتفاع زیاد شد، Scale بر اساس ارتفاع انجام می‌شود.
            if (diagramHeight > maxDiagramHeight) {
                diagramHeight = maxDiagramHeight
                diagramWidth = diagramHeight / ratio.toFloat()
            }

            val sheetRect = RectF(left, top, left + diagramWidth, top + diagramHeight)
            canvas.drawRect(sheetRect, borderPaint)

            val scaleX = diagramWidth / sheet.widthCm.toFloat()
            val scaleY = diagramHeight / sheet.heightCm.toFloat()

            sheet.placements.forEachIndexed { index, placement ->
                val pieceRect = RectF(
                    left + placement.xCm.toFloat() * scaleX,
                    top + placement.yCm.toFloat() * scaleY,
                    left + (placement.xCm + placement.widthCm).toFloat() * scaleX,
                    top + (placement.yCm + placement.heightCm).toFloat() * scaleY
                )

                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = palette[index % palette.size]
                }
                canvas.drawRect(pieceRect, fill)
                canvas.drawRect(pieceRect, borderPaint)

                // نام قطعه فقط وقتی فضا کافی باشد نوشته می‌شود تا نمودار شلوغ نشود.
                if (pieceRect.width() > dp(42f) && pieceRect.height() > dp(20f)) {
                    val label = placement.source.name.take(12)
                    canvas.save()
                    canvas.clipRect(pieceRect)
                    canvas.drawText(label, pieceRect.left + dp(3f), pieceRect.top + dp(13f), textPaint)
                    canvas.restore()
                }
            }

            top = sheetRect.bottom + dp(42f)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun format(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
}
