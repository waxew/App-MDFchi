package com.mdfchi.app

import kotlin.math.max
import kotlin.math.min

/**
 * مختصات یک قطعه پس از جانمایی روی ورق.
 * همه اندازه‌ها بر حسب سانتی‌متر هستند تا با موتور اصلی برنامه یکسان بمانند.
 */
data class PlacedCutPiece(
    val sheetIndex: Int,
    val source: CutPiece,
    val xCm: Double,
    val yCm: Double,
    val widthCm: Double,
    val heightCm: Double,
    val rotated: Boolean
)

/** خلاصه یک ورق بهینه‌شده. */
data class OptimizedSheet(
    val index: Int,
    val widthCm: Double,
    val heightCm: Double,
    val placements: List<PlacedCutPiece>
) {
    /** مساحت واقعی قطعات روی این ورق. */
    fun usedAreaM2(): Double = placements.sumOf { it.widthCm * it.heightCm } / 10000.0

    /** درصد استفاده از ورق؛ Kerf در این درصد به عنوان فضای مصرف‌شده قطعه حساب نمی‌شود. */
    fun efficiencyPercent(): Double =
        if (widthCm <= 0 || heightCm <= 0) 0.0
        else usedAreaM2() / (widthCm * heightCm / 10000.0) * 100.0
}

/** خروجی نهایی الگوریتم چیدمان. */
data class CutOptimizationResult(
    val sheets: List<OptimizedSheet>,
    val unplaced: List<CutPiece>,
    val usedAreaM2: Double,
    val purchasedAreaM2: Double,
    val wasteAreaM2: Double,
    val efficiencyPercent: Double,
    val kerfMm: Double,
    val allowRotation: Boolean
)

/**
 * بهینه‌ساز دو بعدی مستقل از Android.
 *
 * این پیاده‌سازی از خانواده MaxRects/Best Short Side Fit الهام مفهومی گرفته است،
 * اما کد آن مستقل برای این پروژه نوشته شده و از سورس پروژه‌های خارجی کپی نشده است.
 * هدف نسخه 3 رسیدن به یک چیدمان عملی و تکرارپذیر در گوشی است، نه حل ریاضی Global Optimum.
 */
object CutOptimizer {

    /** فضای آزاد مستطیلی داخل یک ورق. */
    private data class FreeRect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double
    )

    /** وضعیت در حال ساخت یک ورق. */
    private data class WorkingSheet(
        val index: Int,
        val width: Double,
        val height: Double,
        val freeRects: MutableList<FreeRect>,
        val placements: MutableList<PlacedCutPiece>
    )

    /** یک نمونه فیزیکی از قطعه؛ quantity قبل از بهینه‌سازی expand می‌شود. */
    private data class Item(
        val source: CutPiece,
        val instance: Int,
        val width: Double,
        val height: Double
    )

    /** Candidate انتخاب‌شده برای قرار دادن قطعه. */
    private data class Candidate(
        val sheet: WorkingSheet,
        val rect: FreeRect,
        val width: Double,
        val height: Double,
        val rotated: Boolean,
        val shortSideScore: Double,
        val longSideScore: Double
    )

    /**
     * چیدمان قطعات MDF روی ورق.
     * قطعات غیر MDF (مثلاً HDF پشت‌بند) عمداً وارد این Optimizer نمی‌شوند چون جنس/ورق جدا دارند.
     */
    fun optimize(
        pieces: List<CutPiece>,
        sheetWidthCm: Double = CabinetEngine.DEFAULT_SHEET_WIDTH_CM,
        sheetLengthCm: Double = CabinetEngine.DEFAULT_SHEET_LENGTH_CM,
        kerfMm: Double = 3.2,
        allowRotation: Boolean = true
    ): CutOptimizationResult {
        require(sheetWidthCm > 0 && sheetLengthCm > 0) { "ابعاد ورق باید بزرگ‌تر از صفر باشند." }
        require(kerfMm >= 0) { "ضخامت برش نمی‌تواند منفی باشد." }

        // Kerf از میلی‌متر به سانتی‌متر تبدیل می‌شود.
        val kerfCm = kerfMm / 10.0

        // تنها MDFها وارد چیدمان می‌شوند؛ هر quantity به Item مستقل تبدیل می‌شود.
        val items = pieces
            .filter { it.material.startsWith("MDF", ignoreCase = true) }
            .flatMap { piece ->
                (1..piece.quantity.coerceAtLeast(1)).map { instance ->
                    // width/height در الگوریتم صرفاً دو ضلع مستطیل هستند؛ نام‌گذاری CutPiece حفظ می‌شود.
                    Item(piece, instance, piece.widthCm, piece.lengthCm)
                }
            }
            // قطعات بزرگ‌تر زودتر قرار می‌گیرند؛ این Heuristic معمولاً پرت را کمتر می‌کند.
            .sortedWith(
                compareByDescending<Item> { it.width * it.height }
                    .thenByDescending { max(it.width, it.height) }
            )

        val sheets = mutableListOf<WorkingSheet>()
        val unplacedItems = mutableListOf<Item>()

        items.forEach { item ->
            // ابتدا در ورق‌های موجود بهترین فضای آزاد را پیدا می‌کنیم.
            var candidate = findBestCandidate(item, sheets, kerfCm, allowRotation)

            // اگر جایی نبود، یک ورق جدید ایجاد می‌شود؛ به شرط اینکه قطعه اصولاً روی ورق جا شود.
            if (candidate == null) {
                val newSheet = WorkingSheet(
                    index = sheets.size + 1,
                    width = sheetWidthCm,
                    height = sheetLengthCm,
                    freeRects = mutableListOf(FreeRect(0.0, 0.0, sheetWidthCm, sheetLengthCm)),
                    placements = mutableListOf()
                )
                sheets += newSheet
                candidate = findBestCandidate(item, listOf(newSheet), kerfCm, allowRotation)
            }

            // قطعه‌ای که حتی با Rotation روی ورق جا نشود در Unplaced گزارش می‌شود.
            if (candidate == null) {
                unplacedItems += item
            } else {
                place(candidate, item, kerfCm)
            }
        }

        // ورق خالی احتمالی که فقط برای یک قطعه غیرقابل‌جانمایی ساخته شده حذف می‌شود.
        val nonEmptySheets = sheets.filter { it.placements.isNotEmpty() }

        val publicSheets = nonEmptySheets.map { sheet ->
            OptimizedSheet(
                index = sheet.index,
                widthCm = sheet.width,
                heightCm = sheet.height,
                placements = sheet.placements.toList()
            )
        }

        val usedArea = publicSheets.sumOf { it.usedAreaM2() }
        val purchasedArea = publicSheets.size * (sheetWidthCm * sheetLengthCm / 10000.0)
        val wasteArea = max(purchasedArea - usedArea, 0.0)
        val efficiency = if (purchasedArea > 0) usedArea / purchasedArea * 100.0 else 0.0

        // Unplaced به CutPiece با quantity واقعی تجمیع می‌شود تا UI قابل فهم باشد.
        val unplaced = unplacedItems
            .groupBy { it.source.copy(quantity = 1) }
            .map { (source, instances) -> source.copy(quantity = instances.size) }

        return CutOptimizationResult(
            sheets = publicSheets,
            unplaced = unplaced,
            usedAreaM2 = usedArea,
            purchasedAreaM2 = purchasedArea,
            wasteAreaM2 = wasteArea,
            efficiencyPercent = efficiency,
            kerfMm = kerfMm,
            allowRotation = allowRotation
        )
    }

    /** بهترین Candidate بین همه FreeRectهای ورق‌های فعلی. */
    private fun findBestCandidate(
        item: Item,
        sheets: List<WorkingSheet>,
        kerfCm: Double,
        allowRotation: Boolean
    ): Candidate? {
        val candidates = mutableListOf<Candidate>()

        sheets.forEach { sheet ->
            sheet.freeRects.forEach { free ->
                // حالت عادی قطعه.
                scoreCandidate(item.width, item.height, false, sheet, free, kerfCm)?.let { candidates += it }

                // اگر چرخش مجاز باشد و اضلاع مساوی نباشند، حالت 90 درجه نیز بررسی می‌شود.
                if (allowRotation && item.width != item.height) {
                    scoreCandidate(item.height, item.width, true, sheet, free, kerfCm)?.let { candidates += it }
                }
            }
        }

        // Best Short Side Fit؛ در تساوی، ضلع بلند باقی‌مانده و سپس شماره ورق معیار می‌شود.
        return candidates.minWithOrNull(
            compareBy<Candidate> { it.shortSideScore }
                .thenBy { it.longSideScore }
                .thenBy { it.sheet.index }
                .thenBy { it.rect.y }
                .thenBy { it.rect.x }
        )
    }

    /** امتیازدهی قرارگیری قطعه داخل FreeRect. */
    private fun scoreCandidate(
        width: Double,
        height: Double,
        rotated: Boolean,
        sheet: WorkingSheet,
        free: FreeRect,
        kerfCm: Double
    ): Candidate? {
        // Kerf به‌صورت محافظه‌کارانه بعد از هر قطعه رزرو می‌شود.
        val occupiedWidth = width + kerfCm
        val occupiedHeight = height + kerfCm
        if (occupiedWidth > free.width + EPS || occupiedHeight > free.height + EPS) return null

        val leftoverH = max(free.width - occupiedWidth, 0.0)
        val leftoverV = max(free.height - occupiedHeight, 0.0)
        return Candidate(
            sheet = sheet,
            rect = free,
            width = width,
            height = height,
            rotated = rotated,
            shortSideScore = min(leftoverH, leftoverV),
            longSideScore = max(leftoverH, leftoverV)
        )
    }

    /** ثبت Placement و Split کردن همه FreeRectهایی که با فضای اشغال‌شده برخورد دارند. */
    private fun place(candidate: Candidate, item: Item, kerfCm: Double) {
        val sheet = candidate.sheet
        val x = candidate.rect.x
        val y = candidate.rect.y

        // ابعاد رزروشده شامل Kerf است؛ خود Placement ابعاد واقعی قطعه را نگه می‌دارد.
        val occupied = FreeRect(
            x = x,
            y = y,
            width = candidate.width + kerfCm,
            height = candidate.height + kerfCm
        )

        sheet.placements += PlacedCutPiece(
            sheetIndex = sheet.index,
            source = item.source.copy(quantity = 1),
            xCm = x,
            yCm = y,
            widthCm = candidate.width,
            heightCm = candidate.height,
            rotated = candidate.rotated
        )

        val nextFree = mutableListOf<FreeRect>()
        sheet.freeRects.forEach { free ->
            if (!intersects(free, occupied)) {
                nextFree += free
            } else {
                nextFree += splitFreeRect(free, occupied)
            }
        }

        // مستطیل‌های بسیار کوچک حذف و مستطیل‌های کاملاً داخل دیگری prune می‌شوند.
        sheet.freeRects.clear()
        sheet.freeRects += pruneContained(
            nextFree.filter { it.width > EPS && it.height > EPS }
        )
    }

    /** تشخیص تقاطع دو مستطیل. */
    private fun intersects(a: FreeRect, b: FreeRect): Boolean =
        b.x < a.x + a.width - EPS &&
            b.x + b.width > a.x + EPS &&
            b.y < a.y + a.height - EPS &&
            b.y + b.height > a.y + EPS

    /**
     * Split چهارطرفه MaxRects.
     * ممکن است FreeRectهای حاصل با هم overlap داشته باشند؛ pruneContained موارد زائد کامل را حذف می‌کند.
     */
    private fun splitFreeRect(free: FreeRect, used: FreeRect): List<FreeRect> {
        val out = mutableListOf<FreeRect>()

        // فضای سمت چپ قطعه.
        if (used.x > free.x + EPS) {
            out += FreeRect(free.x, free.y, used.x - free.x, free.height)
        }
        // فضای سمت راست قطعه.
        val freeRight = free.x + free.width
        val usedRight = used.x + used.width
        if (usedRight < freeRight - EPS) {
            out += FreeRect(usedRight, free.y, freeRight - usedRight, free.height)
        }
        // فضای بالای قطعه.
        if (used.y > free.y + EPS) {
            out += FreeRect(free.x, free.y, free.width, used.y - free.y)
        }
        // فضای پایین قطعه.
        val freeBottom = free.y + free.height
        val usedBottom = used.y + used.height
        if (usedBottom < freeBottom - EPS) {
            out += FreeRect(free.x, usedBottom, free.width, freeBottom - usedBottom)
        }

        return out
    }

    /** حذف مستطیل آزاد کوچکی که به‌طور کامل داخل مستطیل آزاد دیگر قرار گرفته است. */
    private fun pruneContained(rects: List<FreeRect>): List<FreeRect> {
        return rects.filterIndexed { index, rect ->
            rects.indices.none { otherIndex ->
                if (index == otherIndex) false
                else contains(rects[otherIndex], rect)
            }
        }
    }

    /** آیا outer کل inner را پوشش می‌دهد؟ */
    private fun contains(outer: FreeRect, inner: FreeRect): Boolean =
        inner.x >= outer.x - EPS &&
            inner.y >= outer.y - EPS &&
            inner.x + inner.width <= outer.x + outer.width + EPS &&
            inner.y + inner.height <= outer.y + outer.height + EPS

    private const val EPS = 0.000001
}
