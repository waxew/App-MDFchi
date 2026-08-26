// این فایل ساختار کلی پروژه Gradle را تعریف می‌کند.
// Android Studio هنگام باز کردن پروژه ابتدا این فایل را می‌خواند.

pluginManagement {
    // مخزن‌های موردنیاز برای پیدا کردن پلاگین Android و Kotlin.
    repositories {
        // مخزن رسمی Google برای Android Gradle Plugin.
        google()
        // مخزن عمومی کتابخانه‌های JVM/Kotlin.
        mavenCentral()
        // مخزن رسمی پلاگین‌های Gradle.
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // وابستگی‌ها باید از مخزن‌های تعریف‌شده در همین فایل دریافت شوند.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // کتابخانه‌های Android.
        google()
        // کتابخانه‌های عمومی JVM/Kotlin.
        mavenCentral()
    }
}

// نام پروژه در Android Studio.
rootProject.name = "App-MDFchi"

// تنها ماژول اجرایی فعلی پروژه app است.
include(":app")
