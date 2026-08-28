package com.mdfchi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * لایه ذخیره‌سازی پروژه‌ها و تنظیمات پروفایل.
 * استفاده از همان SharedPreferences نسخه 1.x باعث می‌شود داده‌های قبلی پس از Update قابل مهاجرت باشند.
 */
class ProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("mdfchi_projects", Context.MODE_PRIVATE)

    /** بارگذاری پروژه‌ها و مهاجرت خودکار داده قدیمی در اولین اجرای نسخه 2. */
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

    /** حذف یک پروژه. */
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
    fun setUserName(value: String) = prefs.edit().putString(KEY_USER_NAME, value.ifBlank { "کاربر" }).apply()

    /** Uri عکس پروفایل که از Document Picker دریافت و Persist شده است. */
    fun profileUri(): String? = prefs.getString(KEY_PROFILE_URI, null)
    fun setProfileUri(value: String?) = prefs.edit().putString(KEY_PROFILE_URI, value).apply()

    /** تنظیمات اعلان‌ها برای توسعه Notification واقعی در نسخه بعد. */
    fun notificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)
    fun setNotificationsEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    fun updateAlertsEnabled(): Boolean = prefs.getBoolean(KEY_UPDATE_ALERTS, true)
    fun setUpdateAlertsEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_UPDATE_ALERTS, value).apply()

    /** ذخیره کل فهرست پروژه‌ها به شکل آرایه JSON. */
    private fun saveAll(projects: List<ProjectRecord>) {
        val array = JSONArray()
        projects.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PROJECTS_V2, array.toString()).apply()
    }

    /** پارس امن ساختار v2؛ یک رکورد خراب بقیه پروژه‌ها را از بین نمی‌برد. */
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

    companion object {
        private const val KEY_PROJECTS_V2 = "projects_v2"
        private const val KEY_LEGACY_PROJECTS = "projects"
        private const val KEY_USER_NAME = "drawer_user_name"
        private const val KEY_PROFILE_URI = "drawer_profile_uri"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_UPDATE_ALERTS = "update_alerts_enabled"
    }
}
