// تنظیمات Build ماژول اصلی Android.
plugins {
    // این ماژول یک اپلیکیشن قابل نصب Android تولید می‌کند.
    id("com.android.application")
    // سورس اصلی برنامه با Kotlin نوشته شده است.
    id("org.jetbrains.kotlin.android")
}

android {
    // Namespace برای سازگاری آپدیت‌ها ثابت می‌ماند.
    namespace = "com.mdfchi.app"
    compileSdk = 35

    defaultConfig {
        // این شناسه نباید بین نسخه‌ها تغییر کند.
        applicationId = "com.mdfchi.app"
        minSdk = 23
        targetSdk = 35
        // نسخه 2 به‌عنوان ارتقای اصلی و بالاتر از versionCode=3 منتشر می‌شود.
        versionCode = 20
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
