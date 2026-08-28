package com.mdfchi.app

import kotlin.math.ceil
import kotlin.math.max

/**
 * موتور محاسبات تخصصی دستیار هوشمند کابینت‌سازی.
 * این کلاس هیچ وابستگی به Android UI ندارد تا فرمول‌ها مستقل، قابل تست و قابل توسعه باشند.
 */
object CabinetEngine {

    /** اندازه استاندارد پیش‌فرض ورق MDF در ایران؛ کاربر در ابزار تعداد ورق می‌تواند آن را تغییر دهد. */
    const val DEFAULT_SHEET_WIDTH_CM = 183.0
    const val DEFAULT_SHEET_LENGTH_CM = 366.0

    /** ساخت لیست برش کابینت زمینی. */
    fun floorCabinet(
        width: Double,
        height: Double,
        depth: Double,
        thickness: Double,
        shelves: Int,
        doors: Int,
        backThickness: Double,
        wastePercent: Double
    ): CalculationResult {
        requireDimensions(width, height, depth, thickness)
        val inner = width - (2 * thickness)
        require(inner > 0) { "عرض یونیت نسبت به ضخامت MDF کافی نیست." }
        val doorCount = doors.coerceAtLeast(1)
        val pieces = mutableListOf(
            CutPiece("بدنه چپ و راست", height, depth, 2, longEdges = 1),
            CutPiece("کف", inner, depth, 1, longEdges = 1),
            CutPiece("پل بالا جلو و عقب", inner, 10.0, 2, longEdges = 1),
            CutPiece("درب", max(height - 0.4, 1.0), max((width - 0.4) / doorCount, 1.0), doorCount, longEdges = 2, shortEdges = 2)
        )
        if (shelves > 0) pieces += CutPiece("طبقه", inner, max(depth - 1.0, 1.0), shelves, longEdges = 1)
        if (backThickness > 0) pieces += CutPiece("پشت‌بند", height, width, 1, material = "HDF $backThickness cm")
        return summarize(pieces, wastePercent, listOf("فرمول برای یونیت زمینی استاندارد با دو بدنه و پل بالایی است."))
    }

    /** ساخت لیست برش کابینت دیواری. */
    fun wallCabinet(
        width: Double,
        height: Double,
        depth: Double,
        thickness: Double,
        shelves: Int,
        doors: Int,
        backThickness: Double,
        wastePercent: Double
    ): CalculationResult {
        requireDimensions(width, height, depth, thickness)
        val inner = width - (2 * thickness)
        require(inner > 0) { "عرض یونیت نسبت به ضخامت MDF کافی نیست." }
        val doorCount = doors.coerceAtLeast(1)
        val pieces = mutableListOf(
            CutPiece("بدنه چپ و راست", height, depth, 2, longEdges = 1),
            CutPiece("کف و سقف", inner, depth, 2, longEdges = 1),
            CutPiece("درب", max(height - 0.4, 1.0), max((width - 0.4) / doorCount, 1.0), doorCount, longEdges = 2, shortEdges = 2)
        )
        if (shelves > 0) pieces += CutPiece("طبقه", inner, max(depth - 1.0, 1.0), shelves, longEdges = 1)
        if (backThickness > 0) pieces += CutPiece("پشت‌بند", height, width, 1, material = "HDF $backThickness cm")
        return summarize(pieces, wastePercent, listOf("عمق طبقات یک سانتی‌متر کمتر از عمق بدنه در نظر گرفته شده است."))
    }

    /** ساخت لیست برش کمد دیواری. */
    fun wardrobe(
        width: Double,
        height: Double,
        depth: Double,
        thickness: Double,
        shelves: Int,
        verticalDividers: Int,
        doors: Int,
        backThickness: Double,
        wastePercent: Double
    ): CalculationResult {
        requireDimensions(width, height, depth, thickness)
        val inner = width - (2 * thickness)
        require(inner > 0) { "عرض کمد نسبت به ضخامت MDF کافی نیست." }
        val doorCount = doors.coerceAtLeast(1)
        val compartmentWidth = (inner - (verticalDividers.coerceAtLeast(0) * thickness)) / (verticalDividers.coerceAtLeast(0) + 1)
        require(compartmentWidth > 0) { "تعداد تقسیمات عمودی برای این عرض زیاد است." }
        val pieces = mutableListOf(
            CutPiece("بدنه چپ و راست", height, depth, 2, longEdges = 1),
            CutPiece("کف و سقف", inner, depth, 2, longEdges = 1),
            CutPiece("درب", max(height - 0.6, 1.0), max((width - 0.6) / doorCount, 1.0), doorCount, longEdges = 2, shortEdges = 2)
        )
        if (verticalDividers > 0) pieces += CutPiece("تقسیم عمودی", max(height - (2 * thickness), 1.0), depth, verticalDividers, longEdges = 1)
        if (shelves > 0) pieces += CutPiece("طبقه", compartmentWidth, max(depth - 1.0, 1.0), shelves, longEdges = 1)
        if (backThickness > 0) pieces += CutPiece("پشت‌بند", height, width, 1, material = "HDF $backThickness cm")
        return summarize(pieces, wastePercent, listOf("طبقات بر اساس عرض هر دهانه بعد از تقسیم عمودی محاسبه شده‌اند."))
    }

    /** ساخت لیست برش یونیت ایستاده/قدی. */
    fun tallUnit(
        width: Double,
        height: Double,
        depth: Double,
        thickness: Double,
        shelves: Int,
        doors: Int,
        backThickness: Double,
        wastePercent: Double
    ): CalculationResult {
        requireDimensions(width, height, depth, thickness)
        val inner = width - (2 * thickness)
        require(inner > 0) { "عرض یونیت نسبت به ضخامت MDF کافی نیست." }
        val doorCount = doors.coerceAtLeast(1)
        val pieces = mutableListOf(
            CutPiece("بدنه چپ و راست", height, depth, 2, longEdges = 1),
            CutPiece("کف و سقف", inner, depth, 2, longEdges = 1),
            CutPiece("درب قدی", max(height - 0.6, 1.0), max((width - 0.6) / doorCount, 1.0), doorCount, longEdges = 2, shortEdges = 2)
        )
        if (shelves > 0) pieces += CutPiece("طبقه", inner, max(depth - 1.0, 1.0), shelves, longEdges = 1)
        if (backThickness > 0) pieces += CutPiece("پشت‌بند", height, width, 1, material = "HDF $backThickness cm")
        return summarize(pieces, wastePercent, listOf("برای درب‌های بسیار بلند، تعداد لولا را با ابزار لولا بررسی کنید."))
    }

    /** ابعاد تقریبی درب با خلاصی قابل تنظیم بین و دور درب‌ها. */
    fun doorDimensions(openingWidth: Double, openingHeight: Double, doorCount: Int, gapMm: Double): Map<String, Double> {
        require(openingWidth > 0 && openingHeight > 0) { "عرض و ارتفاع باید بزرگ‌تر از صفر باشند." }
        val count = doorCount.coerceAtLeast(1)
        val gapCm = max(gapMm, 0.0) / 10.0
        val totalHorizontalGap = gapCm * (count + 1)
        val eachWidth = (openingWidth - totalHorizontalGap) / count
        val eachHeight = openingHeight - (2 * gapCm)
        require(eachWidth > 0 && eachHeight > 0) { "خلاصی یا تعداد درب برای این ابعاد معتبر نیست." }
        return mapOf("doorWidthCm" to eachWidth, "doorHeightCm" to eachHeight, "doorCount" to count.toDouble())
    }

    /** ابعاد جعبه کشو و کف کشو بر اساس دهانه و خلاصی ریل. */
    fun drawerDimensions(
        openingWidth: Double,
        openingHeight: Double,
        cabinetDepth: Double,
        sideThickness: Double,
        railClearanceMmEachSide: Double,
        bottomThickness: Double
    ): CalculationResult {
        require(openingWidth > 0 && openingHeight > 0 && cabinetDepth > 0 && sideThickness > 0) { "ابعاد کشو معتبر نیستند." }
        val clearanceCm = max(railClearanceMmEachSide, 0.0) / 10.0
        val outerDrawerWidth = openingWidth - (2 * clearanceCm)
        val drawerDepth = max(cabinetDepth - 5.0, 1.0)
        val sideHeight = max(openingHeight * 0.65, 8.0)
        val innerFrontBackWidth = outerDrawerWidth - (2 * sideThickness)
        require(innerFrontBackWidth > 0) { "عرض دهانه برای ضخامت انتخاب‌شده کافی نیست." }
        val pieces = listOf(
            CutPiece("بغل کشو", drawerDepth, sideHeight, 2, longEdges = 1),
            CutPiece("جلو و پشت جعبه کشو", innerFrontBackWidth, sideHeight, 2, longEdges = 1),
            CutPiece("کف کشو", max(drawerDepth - 1.0, 1.0), max(outerDrawerWidth - 1.0, 1.0), 1, material = if (bottomThickness <= 0) "HDF" else "کف $bottomThickness cm")
        )
        return summarize(pieces, 5.0, listOf("عمق کشو ۵ سانتی‌متر کمتر از عمق کابینت در نظر گرفته شده است.", "نوع ریل می‌تواند خلاصی واقعی را تغییر دهد."))
    }

    /** تعداد پیشنهادی لولا و محل مرکز هر لولا از لبه بالای درب. */
    fun hinges(doorHeightCm: Double, doorWeightKg: Double = 0.0): Pair<Int, List<Double>> {
        require(doorHeightCm > 0) { "ارتفاع درب باید بزرگ‌تر از صفر باشد." }
        var count = when {
            doorHeightCm <= 90 -> 2
            doorHeightCm <= 150 -> 3
            doorHeightCm <= 210 -> 4
            doorHeightCm <= 240 -> 5
            else -> 6
        }
        if (doorWeightKg > 20) count += 1
        val edge = 10.0
        val positions = if (count == 1) listOf(doorHeightCm / 2) else {
            val usable = max(doorHeightCm - (2 * edge), 0.0)
            val step = if (count > 1) usable / (count - 1) else 0.0
            List(count) { index -> edge + (step * index) }
        }
        return count to positions
    }

    /** ابعاد و مساحت طبقات. */
    fun shelves(internalWidth: Double, depth: Double, count: Int, frontClearanceCm: Double): CalculationResult {
        require(internalWidth > 0 && depth > 0) { "ابعاد طبقه معتبر نیستند." }
        val qty = count.coerceAtLeast(1)
        val shelfDepth = max(depth - max(frontClearanceCm, 0.0), 1.0)
        val pieces = listOf(CutPiece("طبقه", internalWidth, shelfDepth, qty, longEdges = 1))
        return summarize(pieces, 0.0, emptyList())
    }

    /** ابعاد پشت‌بند و متراژ آن. */
    fun backPanel(width: Double, height: Double, clearanceMm: Double): CalculationResult {
        require(width > 0 && height > 0) { "ابعاد پشت‌بند معتبر نیستند." }
        val gapCm = max(clearanceMm, 0.0) / 10.0
        val pieces = listOf(CutPiece("پشت‌بند", max(height - gapCm, 1.0), max(width - gapCm, 1.0), 1, material = "HDF"))
        return summarize(pieces, 0.0, emptyList())
    }

    /** محاسبه صفحه کابینت بر اساس طول، عمق و تعداد تکه‌ها. */
    fun countertop(lengthCm: Double, depthCm: Double, count: Int): Map<String, Double> {
        require(lengthCm > 0 && depthCm > 0) { "ابعاد صفحه کابینت معتبر نیستند." }
        val qty = count.coerceAtLeast(1)
        return mapOf(
            "linearMeters" to (lengthCm * qty / 100.0),
            "areaM2" to (lengthCm * depthCm * qty / 10000.0),
            "count" to qty.toDouble()
        )
    }

    /** مساحت MDF یک قطعه تکرارشونده. */
    fun mdfArea(lengthCm: Double, widthCm: Double, quantity: Int): Double {
        require(lengthCm > 0 && widthCm > 0) { "ابعاد MDF معتبر نیستند." }
        return lengthCm * widthCm * quantity.coerceAtLeast(1) / 10000.0
    }

    /** تعداد تقریبی ورق از روی مساحت و درصد پرت. */
    fun sheetEstimate(areaM2: Double, sheetWidthCm: Double, sheetLengthCm: Double, wastePercent: Double): Map<String, Double> {
        require(areaM2 > 0 && sheetWidthCm > 0 && sheetLengthCm > 0) { "مساحت یا ابعاد ورق معتبر نیستند." }
        val sheetArea = sheetWidthCm * sheetLengthCm / 10000.0
        val adjusted = areaM2 * (1 + max(wastePercent, 0.0) / 100.0)
        return mapOf("sheetAreaM2" to sheetArea, "adjustedAreaM2" to adjusted, "sheetCount" to ceil(adjusted / sheetArea))
    }

    /** متراژ PVC با درصد پرت. */
    fun pvc(lengthCm: Double, widthCm: Double, quantity: Int, longEdges: Int, shortEdges: Int, wastePercent: Double): Map<String, Double> {
        require(lengthCm > 0 && widthCm > 0) { "ابعاد قطعه معتبر نیستند." }
        val base = ((lengthCm * longEdges.coerceAtLeast(0)) + (widthCm * shortEdges.coerceAtLeast(0))) * quantity.coerceAtLeast(1) / 100.0
        val adjusted = base * (1 + max(wastePercent, 0.0) / 100.0)
        return mapOf("baseMeters" to base, "withWasteMeters" to adjusted)
    }

    /** درصد پرت واقعی از روی مساحت خریداری‌شده و مصرف‌شده. */
    fun waste(usedAreaM2: Double, purchasedAreaM2: Double): Map<String, Double> {
        require(usedAreaM2 >= 0 && purchasedAreaM2 > 0 && usedAreaM2 <= purchasedAreaM2) { "مساحت مصرف و خرید معتبر نیستند." }
        val wasteArea = purchasedAreaM2 - usedAreaM2
        return mapOf("wasteAreaM2" to wasteArea, "wastePercent" to (wasteArea / purchasedAreaM2 * 100.0), "efficiencyPercent" to (usedAreaM2 / purchasedAreaM2 * 100.0))
    }

    /** قیمت MDF. */
    fun mdfPrice(sheetCount: Double, pricePerSheet: Double, transport: Double): Double =
        max(sheetCount, 0.0) * max(pricePerSheet, 0.0) + max(transport, 0.0)

    /** جمع یراق‌آلات اصلی و هزینه متفرقه. */
    fun hardwarePrice(
        hinges: Double, hingePrice: Double,
        railPairs: Double, railPairPrice: Double,
        handles: Double, handlePrice: Double,
        legs: Double, legPrice: Double,
        misc: Double
    ): Double =
        max(hinges, 0.0) * max(hingePrice, 0.0) +
            max(railPairs, 0.0) * max(railPairPrice, 0.0) +
            max(handles, 0.0) * max(handlePrice, 0.0) +
            max(legs, 0.0) * max(legPrice, 0.0) +
            max(misc, 0.0)

    /** محاسبه دستمزد بر اساس متر طول یا مبلغ ثابت تکمیلی. */
    fun labor(linearMeters: Double, pricePerMeter: Double, fixedExtra: Double): Double =
        max(linearMeters, 0.0) * max(pricePerMeter, 0.0) + max(fixedExtra, 0.0)

    /** قیمت نهایی پروژه با تخفیف درصدی اختیاری. */
    fun finalPrice(mdf: Double, hardware: Double, labor: Double, countertop: Double, misc: Double, discountPercent: Double): Map<String, Double> {
        val subtotal = listOf(mdf, hardware, labor, countertop, misc).sumOf { max(it, 0.0) }
        val discount = subtotal * (max(discountPercent, 0.0).coerceAtMost(100.0) / 100.0)
        return mapOf("subtotal" to subtotal, "discount" to discount, "final" to subtotal - discount)
    }

    /** تبدیل واحد طول؛ خروجی هر سه واحد را هم‌زمان برمی‌گرداند. */
    fun convertLength(value: Double, unit: String): Map<String, Double> {
        require(value >= 0) { "مقدار تبدیل معتبر نیست." }
        val meters = when (unit) {
            "mm" -> value / 1000.0
            "cm" -> value / 100.0
            "m" -> value
            else -> error("واحد ناشناخته است.")
        }
        return mapOf("mm" to meters * 1000.0, "cm" to meters * 100.0, "m" to meters)
    }

    /** خلاصه مشترک Cut List، MDF، PVC و ورق. */
    private fun summarize(pieces: List<CutPiece>, wastePercent: Double, notes: List<String>): CalculationResult {
        val mdfPieces = pieces.filter { it.material.startsWith("MDF") }
        val mdfArea = mdfPieces.sumOf { it.areaM2() }
        val pvcMeters = pieces.sumOf { it.pvcMeters() }
        val sheetInfo = if (mdfArea > 0) sheetEstimate(mdfArea, DEFAULT_SHEET_WIDTH_CM, DEFAULT_SHEET_LENGTH_CM, wastePercent) else mapOf("sheetCount" to 0.0, "adjustedAreaM2" to 0.0, "sheetAreaM2" to 0.0)
        val smartNotes = notes.toMutableList()
        val efficiency = if ((sheetInfo["sheetCount"] ?: 0.0) > 0) {
            mdfArea / ((sheetInfo["sheetCount"] ?: 1.0) * (sheetInfo["sheetAreaM2"] ?: 1.0)) * 100.0
        } else 0.0
        when {
            efficiency in 0.0..55.0 -> smartNotes += "پیشنهاد دستیار: پرت اولیه بالاست؛ چیدمان قطعات روی ورق را در نسخه‌های بعدی با Nesting بهینه کنید."
            efficiency >= 80.0 -> smartNotes += "پیشنهاد دستیار: بهره‌وری تقریبی ورق در این برآورد مناسب است."
        }
        return CalculationResult(
            pieces = pieces,
            metrics = linkedMapOf(
                "mdfAreaM2" to mdfArea,
                "areaWithWasteM2" to (sheetInfo["adjustedAreaM2"] ?: mdfArea),
                "sheetCount" to (sheetInfo["sheetCount"] ?: 0.0),
                "pvcMeters" to pvcMeters,
                "estimatedEfficiencyPercent" to efficiency
            ),
            notes = smartNotes
        )
    }

    /** اعتبارسنجی مشترک ابعاد اصلی یونیت. */
    private fun requireDimensions(width: Double, height: Double, depth: Double, thickness: Double) {
        require(width > 0 && height > 0 && depth > 0 && thickness > 0) { "همه ابعاد اصلی باید بزرگ‌تر از صفر باشند." }
        require(width > 2 * thickness) { "عرض برای دو ضخامت بدنه کافی نیست." }
    }
}
