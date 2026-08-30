package com.mdfchi.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * تولید گزارش PDF پروژه بدون کتابخانه خارجی.
 * PdfDocument بخشی از Android Framework است و از minSdk پروژه پشتیبانی می‌کند.
 */
object ReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    /**
     * نوشتن گزارش کامل روی Uri انتخاب‌شده توسط Storage Access Framework.
     * خروجی شامل خلاصه پروژه، مواد مصرفی، نتیجه بهینه‌سازی و Cut List است.
     */
    fun writeProjectPdf(
        context: Context,
        uri: Uri,
        project: ProjectRecord,
        optimization: CutOptimizationResult
    ) {
        val document = PdfDocument()
        val writer = PageWriter(document)

        try {
            writer.title("دستیار هوشمند کابینتسازی")
            writer.subtitle("گزارش پروژه: ${project.name}")
            writer.line("نوع پروژه: ${project.type.label}")
            writer.line("آخرین ویرایش: ${date(project.updatedAt)}")
            writer.space(8)

            writer.heading("خلاصه پروژه")
            project.metrics["mdfAreaM2"]?.let { writer.line("متراژ MDF: ${fmt(it)} مترمربع") }
            project.metrics["sheetCount"]?.let { writer.line("تعداد تقریبی ورق محاسبات اولیه: ${fmt(it)}") }
            project.metrics["pvcMeters"]?.let { writer.line("نوار PVC: ${fmt(it)} متر") }
            writer.line("ورق موردنیاز بعد از چیدمان: ${optimization.sheets.size}")
            writer.line("راندمان چیدمان: ${fmt(optimization.efficiencyPercent)}٪")
            writer.line("پرت سطحی تقریبی: ${fmt(optimization.wasteAreaM2)} مترمربع")
            writer.space(8)

            writer.heading("لیست برش")
            project.cutList.forEachIndexed { index, piece ->
                writer.line(
                    "${index + 1}. ${piece.name} — ${fmt(piece.lengthCm)} × ${fmt(piece.widthCm)} cm — ${piece.quantity} عدد — ${piece.material} — PVC ${fmt(piece.pvcMeters())} m"
                )
                if (piece.note.isNotBlank()) writer.small("توضیح: ${piece.note}")
            }

            if (project.notes.isNotEmpty()) {
                writer.space(8)
                writer.heading("پیشنهادها و نکات")
                project.notes.forEach { note -> writer.line("• $note") }
            }

            if (optimization.unplaced.isNotEmpty()) {
                writer.space(8)
                writer.heading("قطعات خارج از ابعاد ورق")
                optimization.unplaced.forEach { piece ->
                    writer.line("• ${piece.name}: ${fmt(piece.lengthCm)} × ${fmt(piece.widthCm)} cm — ${piece.quantity} عدد")
                }
            }

            writer.space(12)
            writer.small("این گزارش یک برآورد کارگاهی است. قبل از برش نهایی، اندازه‌ها، رگه ورق، نوع یراق و استاندارد ساخت کارگاه کنترل شوند.")
            writer.finish()

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                document.writeTo(stream)
            } ?: error("امکان ساخت فایل خروجی وجود ندارد.")
        } finally {
            document.close()
        }
    }

    /** Writer صفحه‌بندی‌شده برای متن RTL. */
    private class PageWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = MARGIN

        private val normalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(40, 40, 40)
            textSize = 13f
        }
        private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(85, 85, 85)
            textSize = 10f
        }
        private val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(86, 55, 37)
            textSize = 16f
            isFakeBoldText = true
        }
        private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 48, 32)
            textSize = 22f
            isFakeBoldText = true
        }
        private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 55, 55)
            textSize = 15f
            isFakeBoldText = true
        }

        init {
            newPage()
        }

        fun title(text: String) = paragraph(text, titlePaint, 14)
        fun subtitle(text: String) = paragraph(text, subtitlePaint, 10)
        fun heading(text: String) = paragraph(text, headingPaint, 8)
        fun line(text: String) = paragraph(text, normalPaint, 5)
        fun small(text: String) = paragraph(text, smallPaint, 4)
        fun space(px: Int) {
            y += px
            ensureSpace(20)
        }

        /** بستن آخرین صفحه در پایان گزارش. */
        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }

        /** ساخت یک پاراگراف RTL و رفتن خودکار به صفحه بعد در صورت کمبود فضا. */
        private fun paragraph(text: String, paint: TextPaint, bottomSpace: Int) {
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, CONTENT_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(2f, 1f)
                .build()

            ensureSpace(layout.height + bottomSpace)
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN.toFloat(), y.toFloat())
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + bottomSpace
        }

        /** اگر پاراگراف در صفحه جا نشود، صفحه جدید ساخته می‌شود. */
        private fun ensureSpace(required: Int) {
            if (y + required <= PAGE_HEIGHT - MARGIN) return
            page?.let { document.finishPage(it) }
            newPage()
        }

        /** ایجاد صفحه A4 با Footer نسخه صفحه. */
        private fun newPage() {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(info)
            y = MARGIN

            val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                textSize = 9f
            }
            page!!.canvas.drawText(
                "Smart Cabinet Assistant • Page $pageNumber",
                MARGIN.toFloat(),
                (PAGE_HEIGHT - 18).toFloat(),
                footer
            )
        }
    }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun date(time: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(time))
}
