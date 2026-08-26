// تنظیمات Build ماژول اصلی Android.
plugins {
    // این ماژول یک اپلیکیشن قابل نصب Android تولید می‌کند.
    id("com.android.application")

    // سورس اصلی برنامه با Kotlin نوشته شده است.
    id("org.jetbrains.kotlin.android")
}

android {
    // Namespace کلاس‌های تولیدشده Android؛ برای سازگاری نسخه‌ها ثابت نگه داشته می‌شود.
    namespace = "com.mdfchi.app"

    // SDK مورد استفاده برای کامپایل سورس.
    compileSdk = 35

    defaultConfig {
        // شناسه نصب برنامه؛ تغییر آن باعث می‌شود Android برنامه را اپ جداگانه بداند.
        // برای Update شدن نسخه‌های آینده این مقدار باید همیشه ثابت بماند.
        applicationId = "com.mdfchi.app"

        // حداقل Android 6.0 برای پوشش دستگاه‌های قدیمی‌تر.
        minSdk = 23

        // سطح API هدف نسخه فعلی.
        targetSdk = 35

        // شماره داخلی نسخه؛ برای هر انتشار باید حتماً افزایش پیدا کند.
        versionCode = 3

        // نسخه قابل نمایش به کاربر.
        versionName = "1.0.2"
    }

    buildTypes {
        // خروجی Release همان خروجی پابلیش برنامه است.
        release {
            // فعلاً Minify خاموش است تا نگهداری و عیب‌یابی نسخه‌های اولیه ساده‌تر باشد.
            isMinifyEnabled = false

            // قواعد استاندارد R8/ProGuard به‌علاوه قواعد اختصاصی پروژه.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        // BuildConfig برای خواندن VERSION_NAME و VERSION_CODE داخل برنامه لازم است.
        buildConfig = true
    }

    compileOptions {
        // Java 17 نسخه سازگار با Android Gradle Plugin فعلی است.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Bytecode کد Kotlin نیز برای Java 17 تولید می‌شود.
        jvmTarget = "17"
    }
}
