package com.mdfchi.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * نوع یونیت‌هایی که موتور محاسبات نسخه 2.0.0 پشتیبانی می‌کند.
 * name انگلیسی برای ذخیره پایدار و label فارسی فقط برای رابط کاربری است.
 */
enum class CabinetType(val label: String, val emoji: String) {
    FLOOR("کابینت زمینی", "🗄️"),
    WALL("کابینت دیواری", "🧱"),
    WARDROBE("کمد دیواری", "🚪"),
    TALL("یونیت ایستاده", "📦"),
    LEGACY("پروژه نسخه قدیمی", "🧰");

    companion object {
        /** تبدیل امن مقدار ذخیره‌شده به enum. */
        fun fromStored(value: String?): CabinetType =
            entries.firstOrNull { it.name == value } ?: LEGACY
    }
}

/** یک ردیف از لیست برش. واحد مرجع طول و عرض سانتی‌متر است. */
data class CutPiece(
    val name: String,
    val lengthCm: Double,
    val widthCm: Double,
    val quantity: Int,
    val material: String = "MDF",
    val longEdges: Int = 0,
    val shortEdges: Int = 0,
    val note: String = ""
) {
    /** مساحت کل این ردیف با احتساب تعداد، بر حسب متر مربع. */
    fun areaM2(): Double = (lengthCm * widthCm * quantity) / 10000.0

    /** متراژ نوار PVC همین ردیف با توجه به لبه‌های علامت‌گذاری‌شده. */
    fun pvcMeters(): Double =
        ((lengthCm * longEdges) + (widthCm * shortEdges)) * quantity / 100.0

    /** تبدیل به JSON برای ذخیره محلی. */
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("lengthCm", lengthCm)
        .put("widthCm", widthCm)
        .put("quantity", quantity)
        .put("material", material)
        .put("longEdges", longEdges)
        .put("shortEdges", shortEdges)
        .put("note", note)

    companion object {
        /** خواندن CutPiece از JSON با fallback برای داده‌های قدیمی. */
        fun fromJson(json: JSONObject): CutPiece = CutPiece(
            name = json.optString("name", "قطعه"),
            lengthCm = json.optDouble("lengthCm", json.optDouble("a", 0.0)),
            widthCm = json.optDouble("widthCm", json.optDouble("b", 0.0)),
            quantity = json.optInt("quantity", json.optInt("qty", 1)).coerceAtLeast(1),
            material = json.optString("material", "MDF"),
            longEdges = json.optInt("longEdges", 0).coerceAtLeast(0),
            shortEdges = json.optInt("shortEdges", 0).coerceAtLeast(0),
            note = json.optString("note", "")
        )
    }
}

/** خروجی کامل یک محاسبه تخصصی. */
data class CalculationResult(
    val pieces: List<CutPiece>,
    val metrics: Map<String, Double>,
    val notes: List<String>
)

/** مدل پروژه ذخیره‌شده نسخه 2. */
data class ProjectRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: CabinetType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val values: Map<String, String>,
    val cutList: List<CutPiece>,
    val metrics: Map<String, Double>,
    val notes: List<String>
) {
    /** سریال‌سازی پروژه به JSON. */
    fun toJson(): JSONObject {
        val valueObject = JSONObject()
        values.forEach { (key, value) -> valueObject.put(key, value) }

        val metricObject = JSONObject()
        metrics.forEach { (key, value) -> metricObject.put(key, value) }

        val piecesArray = JSONArray()
        cutList.forEach { piecesArray.put(it.toJson()) }

        val notesArray = JSONArray()
        notes.forEach { notesArray.put(it) }

        return JSONObject()
            .put("schema", 2)
            .put("id", id)
            .put("name", name)
            .put("type", type.name)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
            .put("values", valueObject)
            .put("cutList", piecesArray)
            .put("metrics", metricObject)
            .put("notes", notesArray)
    }

    companion object {
        /** خواندن پروژه جدید. */
        fun fromJson(json: JSONObject): ProjectRecord {
            val values = linkedMapOf<String, String>()
            val valueObject = json.optJSONObject("values") ?: JSONObject()
            valueObject.keys().forEach { key -> values[key] = valueObject.optString(key, "") }

            val metrics = linkedMapOf<String, Double>()
            val metricObject = json.optJSONObject("metrics") ?: JSONObject()
            metricObject.keys().forEach { key -> metrics[key] = metricObject.optDouble(key, 0.0) }

            val pieces = mutableListOf<CutPiece>()
            val piecesArray = json.optJSONArray("cutList") ?: JSONArray()
            for (index in 0 until piecesArray.length()) {
                val item = piecesArray.optJSONObject(index) ?: continue
                pieces += CutPiece.fromJson(item)
            }

            val notes = mutableListOf<String>()
            val notesArray = json.optJSONArray("notes") ?: JSONArray()
            for (index in 0 until notesArray.length()) notes += notesArray.optString(index, "")

            return ProjectRecord(
                id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                name = json.optString("name", "پروژه بدون نام"),
                type = CabinetType.fromStored(json.optString("type")),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                values = values,
                cutList = pieces,
                metrics = metrics,
                notes = notes
            )
        }

        /** مهاجرت پروژه‌های نسخه 1.x به ساختار جدید بدون حذف اطلاعات کاربر. */
        fun fromLegacy(json: JSONObject): ProjectRecord {
            val legacyPieces = mutableListOf<CutPiece>()
            val parts = json.optJSONArray("pieces") ?: JSONArray()
            for (index in 0 until parts.length()) {
                val item = parts.optJSONObject(index) ?: continue
                legacyPieces += CutPiece(
                    name = item.optString("name", "قطعه"),
                    lengthCm = item.optDouble("a", 0.0),
                    widthCm = item.optDouble("b", 0.0),
                    quantity = item.optInt("qty", 1).coerceAtLeast(1)
                )
            }

            val values = linkedMapOf(
                "width" to json.optString("width", ""),
                "height" to json.optString("height", ""),
                "depth" to json.optString("depth", "")
            )

            return ProjectRecord(
                name = json.optString("name", "پروژه منتقل‌شده"),
                type = CabinetType.LEGACY,
                createdAt = json.optLong("savedAt", System.currentTimeMillis()),
                updatedAt = System.currentTimeMillis(),
                values = values,
                cutList = legacyPieces,
                metrics = mapOf("mdfAreaM2" to legacyPieces.filter { it.material == "MDF" }.sumOf { it.areaM2() }),
                notes = listOf("این پروژه از نسخه 1.x به ساختار نسخه 2 منتقل شده است.")
            )
        }
    }
}
