# راهنمای سورس App-MDFchi

این فایل توضیح می‌دهد هر بخش پروژه برای چه ساخته شده است.

- `settings.gradle.kts`: نام پروژه، ماژول‌ها و Repositoryهای Gradle را مشخص می‌کند.
- `build.gradle.kts`: نسخه Android Gradle Plugin و Kotlin را در سطح پروژه تعیین می‌کند.
- `gradle.properties`: حافظه Gradle، UTF-8، AndroidX و تنظیمات عمومی Build را نگه می‌دارد.
- `.gitignore`: فایل‌های تولیدی IDE/Gradle و مخصوصاً کلیدهای امضا را از Commit شدن خارج می‌کند.
- `app/build.gradle.kts`: تنظیمات اصلی Android شامل SDK، حداقل Android، نسخه، شناسه ثابت نصب و Release را تعریف می‌کند.
- `app/proguard-rules.pro`: محل قواعد اختصاصی R8/ProGuard برای نسخه‌هایی است که Minify فعال شود.
- `app/src/main/AndroidManifest.xml`: Permission اینترنت، آیکون، Theme، Activity اصلی و رفتار Back را تعریف می‌کند.
- `app/src/main/java/com/mdfchi/app/MainActivity.kt`: منطق UI، منوی همبرگری، Back Stack، محاسبات، ذخیره پروژه و بررسی بروزرسانی در این فایل است.
- `app/src/main/res/values/styles.xml`: Theme روشن و رنگ نوارهای سیستم را مشخص می‌کند.
- `app/src/main/res/drawable/ic_app_icon.xml`: آیکون برداری جعبه ابزار برنامه است.
- `version.json`: نسخه منتشرشده و مسیر دریافت آن را برای Update Checker اعلام می‌کند.
- `.github/workflows/android-apk.yml`: روی GitHub Actions سورس را Compile کرده و unsigned Release APK می‌سازد.
- `README.md`: معرفی عمومی، امکانات، روش Build و معماری Update را توضیح می‌دهد.
- `CHANGELOG.md`: تغییرات هر نسخه را نگه می‌دارد.

## فایل‌های تولیدشده و فایل‌های برنامه‌نویس

فایل‌های `build/`، `.gradle/`، `.idea/` و APKهای موقت توسط Android Studio/Gradle تولید می‌شوند و نباید به‌عنوان سورس اصلی ویرایش شوند. فایل‌های Gradle، Manifest، Kotlin، Resource XML، Workflow، README و version.json فایل‌هایی هستند که برنامه‌نویس نگهداری می‌کند.

## Signing

برای امنیت، فایل `.jks` و Passwordها در GitHub عمومی وجود ندارند. ZIP خصوصی پروژه شامل `signing/MDFchi-signing-key.jks` و `info.txt` است. گم شدن این کلید باعث می‌شود نتوان نسخه‌های بعدی را به‌عنوان Update روی نسخه منتشرشده نصب کرد.
