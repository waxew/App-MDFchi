# راهنمای سورس — نسخه 2.0.0

این فایل برای کسی نوشته شده که پروژه را در Android Studio باز می‌کند و می‌خواهد سریع بفهمد هر بخش چه کاری انجام می‌دهد.

## فایل‌های Kotlin

### `MainActivity.kt`
رابط کاربری Native Android، صفحات، Navigation/Back Stack، Drawer، پروفایل، فرم‌های ابزارها، پروژه‌ها و خروجی CSV را مدیریت می‌کند. هر ورودی به‌صورت Label بالای کادر و Hint کم‌رنگ داخل کادر ساخته می‌شود.

### `CabinetEngine.kt`
موتور مستقل محاسبات است. فرمول‌های کابینت زمینی، دیواری، کمد، یونیت ایستاده، درب، کشو، لولا، طبقه، پشت‌بند، صفحه، MDF، ورق، PVC، پرت، قیمت، یراق، دستمزد و تبدیل واحد در این فایل قرار دارند. این فایل عمداً Android UI import ندارد.

### `Models.kt`
مدل‌های `CabinetType`، `CutPiece`، `CalculationResult` و `ProjectRecord` را تعریف می‌کند. سریال‌سازی JSON و مهاجرت مدل پروژه قدیمی نیز اینجاست.

### `ProjectStore.kt`
SharedPreferences را مدیریت می‌کند. پروژه‌های نسخه 2 را ذخیره و داده `projects` نسخه 1.x را در اولین بار به `projects_v2` مهاجرت می‌دهد. نام/عکس پروفایل و تنظیمات اعلان نیز اینجا نگه داشته می‌شوند.

## فایل‌های Android

### `AndroidManifest.xml`
Permission اینترنت برای Update Checker، نام برنامه، آیکون، Theme و Activity Launcher را تعریف می‌کند.

### `app/build.gradle.kts`
Application ID ثابت، minSdk/targetSdk و نسخه `2.0.0 / 20` را نگه می‌دارد.

### `ic_app_icon.xml`
آیکون Vector برنامه است؛ روی DPIهای مختلف بدون افت کیفیت مقیاس می‌شود.

### `styles.xml`
Theme روشن و رنگ‌های Status/Navigation bar را تعریف می‌کند.

## انتشار

`.github/workflows/android-apk.yml` روی GitHub Actions یک APK Release بدون امضا می‌سازد. Signing Key خصوصی نباید در ریپوی Public قرار گیرد. خروجی نهایی باید با Key موجود در بسته خصوصی پروژه امضا و با `apksigner verify` کنترل شود.
