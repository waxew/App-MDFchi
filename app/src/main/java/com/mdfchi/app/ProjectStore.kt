package com.mdfchi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * لایه ذخیره‌سازی پروژه‌ها، تنظیمات پروفایل و تنظیمات کارگاهی.
 *
 * نکات مهم:
 * - نام SharedPreferences نسخه‌های قبلی حفظ شده تا Update اطلاعات کاربر را از بین نبرد.
 * - داده‌های نسخه 1.x در اولین اجرا به مدل نسخه جدید مهاجرت می‌شوند.
 * - Backup/Restore به‌صورت JSON مستقل از حافظه داخلی انجام می‌شود.
 */
class ProjectStore(context: Context) {

    // SharedPreferences خصوصی برنامه؛ فقط همین اپ به آن دسترسی مستقیم دارد.
    private val prefs = context.getSharedPreferences("mdfchi_projects", Context.MODE_PRIVATE)

    /** بارگذاری پروژه‌ها و مهاجرت خودکار داده قدیمی در اولین اجرای نسخه‌های جدید. */
    fun loadProjects(): MutableList<ProjectRecord> {
        val v2Raw = prefs.getString(KEY_PROJECTS_V2, null)
        if (!v2Raw.isNullOrBlank()) return parseV2(v2Raw)

        val legacyRaw = prefs.getString(KEY_LEGACY_PROJECTS, null)
        if (!legacyRaw.isNullOrBlank()) {
            val migrated = migrateLegacy(legacyRaw)
            if (migrated.isNotEmpty()) saveAll(migrated)
            return migrated
        }
        return mutableListOf()
    }

    /** درج پروژه جدید یا جایگزینی پروژه موجود با همان id. */
    fun upsert(project: ProjectRecord) {
        val all = loadProjects()
        val index = all.indexOfFirst { it.id == project.id }
        if (index >= 0) all[index] = project else all.add(project)
        saveAll(all)
    }

    /** حذف یک پروژه با شناسه یکتا. */
    fun delete(projectId: String) {
        val all = loadProjects().filterNot { it.id == projectId }
        saveAll(all)
    }

    /** کپی پروژه با شناسه جدید تا نسخه اصلی دست‌نخورده بماند. */
    fun copy(project: ProjectRecord): ProjectRecord {
        val now = System.currentTimeMillis()
        val copied = project.copy(
            id = UUID.randomUUID().toString(),
            name = "کپی ${project.name}",
            createdAt = now,
            updatedAt = now
        )
        upsert(copied)
        return copied
    }

    /** نام کاربر برای Drawer. */
    fun userName(): String = prefs.getString(KEY_USER_NAME, "کاربر") ?: "کاربر"

    /** ذخیره نام کاربر؛ مقدار خالی به نام پیش‌فرض برمی‌گردد. */
    fun setUserName(value: String) =
        prefs.edit().putString(KEY_USER_NAME, value.ifBlank { "کاربر" }).apply()

    /** Uri عکس پروفایل که از Document Picker دریافت و Persist شده است. */
    fun profileUri(): String? = prefs.getString(KEY_PROFILE_URI, null)

    /** ذخیره یا حذف Uri عکس پروفایل. */
    fun setProfileUri(value: String?) = prefs.edit().putString(KEY_PROFILE_URI, value).apply()

    /** وضعیت اعلان‌های عمومی برنامه. */
    fun notificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    /** تغییر وضعیت اعلان‌های عمومی. */
    fun setNotificationsEnabled(value: Boolean) =
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    /** وضعیت اعلان انتشار نسخه جدید. */
    fun updateAlertsEnabled(): Boolean = prefs.getBoolean(KEY_UPDATE_ALERTS, true)

    /** تغییر وضعیت اعلان انتشار نسخه جدید. */
    fun setUpdateAlertsEnabled(value: Boolean) =
        prefs.edit().putBoolean(KEY_UPDATE_ALERTS, value).apply()

    /** عرض پیش‌فرض ورق کارگاه بر حسب سانتی‌متر. */
    fun sheetWidthCm(): Double = readDouble(KEY_SHEET_WIDTH, CabinetEngine.DEFAULT_SHEET_WIDTH_CM)

    /** طول پیش‌فرض ورق کارگاه بر حسب سانتی‌متر. */
    fun sheetLengthCm(): Double = readDouble(KEY_SHEET_LENGTH, CabinetEngine.DEFAULT_SHEET_LENGTH_CM)

    /** Kerf یا ضخامت برش تیغه بر حسب میلی‌متر. */
    fun kerfMm(): Double = readDouble(KEY_KERF_MM, 3.2)

    /** آیا Cut Optimizer اجازه چرخش 90 درجه قطعه را دارد؟ */
    fun allowRotation(): Boolean = prefs.getBoolean(KEY_ALLOW_ROTATION, true)

    /** ذخیره تنظیمات ورق و تیغه. */
    fun setWorkshopSettings(
        sheetWidthCm: Double,
        sheetLengthCm: Double,
        kerfMm: Double,
        allowRotation: Boolean
    ) {
        require(sheetWidthCm > 0 && sheetLengthCm > 0) { "ابعاد ورق معتبر نیستند." }
        require(kerfMm >= 0) { "Kerf نمی‌تواند منفی باشد." }
        prefs.edit()
            .putString(KEY_SHEET_WIDTH, sheetWidthCm.toString())
            .putString(KEY_SHEET_LENGTH, sheetLengthCm.toString())
            .putString(KEY_KERF_MM, kerfMm.toString())
            .putBoolean(KEY_ALLOW_ROTATION, allowRotation)
            .apply()
    }

    /**
     * ساخت Backup قابل حمل.
     * profileUri عمداً در Backup قرار نمی‌گیرد چون مجوز Uri معمولاً فقط روی همان دستگاه معتبر است.
     */
    fun backupJson(): String {
        val projects = JSONArray()
        loadProjects().forEach { projects.put(it.toJson()) }

        val settings = JSONObject()
            .put("userName", userName())
            .put("notificationsEnabled", notificationsEnabled())
            .put("updateAlertsEnabled", updateAlertsEnabled())
            .put("sheetWidthCm", sheetWidthCm())
            .put("sheetLengthCm", sheetLengthCm())
            .put("kerfMm", kerfMm())
            .put("allowRotation", allowRotation())

        return JSONObject()
            .put("schema", BACKUP_SCHEMA)
            .put("app", "Smart Cabinet Assistant")
            .put("createdAt", System.currentTimeMillis())
            .put("projects", projects)
            .put("settings", settings)
            .toString(2)
    }

    /**
     * بازیابی Backup.
     * merge=true پروژه‌های Backup را با پروژه‌های فعلی Merge می‌کند و پروژه هم-id را جایگزین می‌کند.
     */
    fun restoreBackup(raw: String, merge: Boolean = true): Int {
        val root = JSONObject(raw)
        val schema = root.optInt("schema", 0)
        require(schema in 2..BACKUP_SCHEMA) { "نسخه فایل پشتیبان پشتیبانی نمی‌شود." }

        val restored = mutableListOf<ProjectRecord>()
        val array = root.optJSONArray("projects") ?: JSONArray()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            runCatching { ProjectRecord.fromJson(json) }
                .getOrNull()
                ?.let { restored += it }
        }

        val finalProjects = if (merge) {
            val byId = linkedMapOf<String, ProjectRecord>()
            loadProjects().forEach { byId[it.id] = it }
            restored.forEach { byId[it.id] = it }
            byId.values.toList()
        } else {
            restored
        }
        saveAll(finalProjects)

        root.optJSONObject("settings")?.let { settings ->
            setUserName(settings.optString("userName", userName()))
            setNotificationsEnabled(settings.optBoolean("notificationsEnabled", notificationsEnabled()))
            setUpdateAlertsEnabled(settings.optBoolean("updateAlertsEnabled", updateAlertsEnabled()))
            setWorkshopSettings(
                sheetWidthCm = settings.optDouble("sheetWidthCm", sheetWidthCm()),
                sheetLengthCm = settings.optDouble("sheetLengthCm", sheetLengthCm()),
                kerfMm = settings.optDouble("kerfMm", kerfMm()),
                allowRotation = settings.optBoolean("allowRotation", allowRotation())
            )
        }

        return restored.size
    }

    /** ذخیره کل فهرست پروژه‌ها به شکل آرایه JSON. */
    private fun saveAll(projects: List<ProjectRecord>) {
        val array = JSONArray()
        projects.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PROJECTS_V2, array.toString()).apply()
    }

    /** پارس امن ساختار جدید؛ یک رکورد خراب بقیه پروژه‌ها را از بین نمی‌برد. */
    private fun parseV2(raw: String): MutableList<ProjectRecord> {
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<ProjectRecord>()
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                runCatching { ProjectRecord.fromJson(json) }.getOrNull()?.let { result += it }
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    /** مهاجرت آرایه projects نسخه قدیمی. */
    private fun migrateLegacy(raw: String): MutableList<ProjectRecord> {
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<ProjectRecord>()
            for (index in 0 until array.length()) {
                val json: JSONObject = array.optJSONObject(index) ?: continue
                result += ProjectRecord.fromLegacy(json)
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    /** خواندن Double از SharedPreferences با fallback امن. */
    private fun readDouble(key: String, fallback: Double): Double =
        prefs.getString(key, null)?.toDoubleOrNull() ?: fallback

    companion object {
        private const val KEY_PROJECTS_V2 = "projects_v2"
        private const val KEY_LEGACY_PROJECTS = "projects"
        private const val KEY_USER_NAME = "drawer_user_name"
        private const val KEY_PROFILE_URI = "drawer_profile_uri"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_UPDATE_ALERTS = "update_alerts_enabled"
        private const val KEY_SHEET_WIDTH = "sheet_width_cm"
        private const val KEY_SHEET_LENGTH = "sheet_length_cm"
        private const val KEY_KERF_MM = "kerf_mm"
        private const val KEY_ALLOW_ROTATION = "allow_rotation"
        private const val BACKUP_SCHEMA = 3
    }
}
