# راهنمای سورس — دستیار هوشمند کابینتسازی 3.0.0

## فایل‌های Kotlin

### `MainActivity.kt`
UI برنامه، Navigation، Back Stack، Drawer، فرم‌ها، Project Search، Settings، CSV/PDF و Backup/Restore را مدیریت می‌کند. فرم پروژه جدید هیچ مقدار نمونه واقعی ندارد و تنها از Hint استفاده می‌کند.

### `CabinetEngine.kt`
تمام فرمول‌های کابینت و ابزارهای تخصصی در این فایل قرار دارند. این فایل تا حد امکان مستقل از Android UI نگه داشته شده تا تست عددی ساده باشد.

### `Models.kt`
مدل‌های `CabinetType`، `CutPiece`، `CalculationResult` و `ProjectRecord` و Serialization پروژه را تعریف می‌کند.

### `ProjectStore.kt`
ذخیره‌سازی SharedPreferences، Migration نسخه 1.x، تنظیمات کارگاه، Backup و Restore JSON را انجام می‌دهد.

### `CutOptimizer.kt`
موتور مستقل چیدمان قطعات MDF روی ورق. از Heuristic نوع Best Short Side Fit استفاده می‌کند و Kerf/Rotation را لحاظ می‌کند. کد مستقل پروژه است.

### `NestingView.kt`
نتیجه Optimizer را با Canvas Android به شکل نمودار ورق و قطعات نمایش می‌دهد. هیچ منطق محاسباتی اصلی داخل View نیست.

### `ReportExporter.kt`
گزارش PDF پروژه را با `PdfDocument` و `StaticLayout` می‌سازد. اطلاعات پروژه، Cut List، راندمان و نکات را صفحه‌بندی می‌کند.

## داده و Update
- SharedPreferences اصلی: `mdfchi_projects`
- پروژه‌های جدید: کلید `projects_v2`
- پروژه‌های نسخه 1.x: کلید `projects` و Migration خودکار
- Application ID: `com.mdfchi.app`
- نسخه فعلی: `3.0.0` / `versionCode 30`

## Signing
Signing Key خصوصی نباید وارد GitHub عمومی شود. بسته خصوصی انتشار شامل Key، Passwordها، Fingerprintها و `info.txt` است. نسخه‌های بعدی باید با همان Key امضا شوند.

## تست‌های مهم قبل از انتشار بعدی
1. Build Release در GitHub Actions
2. Verify v1/v2/v3 signature
3. تطابق Certificate با نسخه قبلی
4. تست محاسبات کابینت و Cut Optimizer
5. تست Backup/Restore
6. تست CSV/PDF روی دستگاه
7. تست Back Stack و Drawer
8. افزایش versionCode
