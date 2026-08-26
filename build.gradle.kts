// فایل Gradle سطح پروژه.
// نسخه پلاگین‌ها در اینجا یک‌جا مشخص می‌شود تا ماژول app فقط آن‌ها را فعال کند.
plugins {
    // پلاگین ساخت اپلیکیشن Android؛ apply false یعنی در سطح root اجرا نمی‌شود.
    id("com.android.application") version "8.8.2" apply false

    // پلاگین رسمی Kotlin برای Android.
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
