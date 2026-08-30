from pathlib import Path

p = Path("app/src/main/java/com/mdfchi/app/MainActivity.kt")
s = p.read_text()

def between(start: str, end: str, replacement: str) -> None:
    global s
    a = s.index(start)
    b = s.index(end, a)
    s = s[:a] + replacement + s[b:]

s = s.replace("import android.text.InputType\n", "import android.text.Editable\nimport android.text.InputType\nimport android.text.TextWatcher\n")

between(
    "    private enum class Screen {",
    "\n\n    /** ابزارهای تخصصی غیر از چهار نوع یونیت. */",
    """    private enum class Screen {
        HOME, TOOLS, CABINET, TOOL, PROJECTS, PROJECT, OPTIMIZER,
        SETTINGS, UPDATE, ABOUT, CONTACT, SOFTWARE
    }"""
)

s = s.replace(
    "    private var pendingCsv: String? = null\n",
    "    private var pendingCsv: String? = null\n    // پروژه‌ای که برای خروجی PDF انتخاب شده تا پس از برگشت از Document Picker مشخص بماند.\n    private var pendingPdfProject: ProjectRecord? = null\n"
)

between(
    '    @Deprecated("Legacy result API keeps the project dependency-light")\n    override fun onActivityResult',
    "\n\n    /** ثبت صفحه فعلی و رفتن به مقصد. */",
    '''    @Deprecated("Legacy result API keeps the project dependency-light")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_PROFILE -> data?.data?.let { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                store.setProfileUri(uri.toString())
                toast("عکس پروفایل ذخیره شد.")
                render(current)
            }
            REQUEST_CSV -> {
                val uri = data?.data ?: return
                val csv = pendingCsv ?: return
                runCatching {
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        it.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess { toast("لیست برش ذخیره شد.") }
                    .onFailure { toast("ذخیره فایل انجام نشد.") }
                pendingCsv = null
            }
            REQUEST_PDF -> {
                val uri = data?.data ?: return
                val project = pendingPdfProject ?: return
                runCatching {
                    ReportExporter.writeProjectPdf(this, uri, project, optimizeProject(project))
                }.onSuccess { toast("گزارش PDF ذخیره شد.") }
                    .onFailure { toast("ساخت PDF انجام نشد: ${it.message.orEmpty()}") }
                pendingPdfProject = null
            }
            REQUEST_BACKUP_CREATE -> {
                val uri = data?.data ?: return
                runCatching {
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                        writer.write(store.backupJson())
                    }
                }.onSuccess { toast("فایل پشتیبان ذخیره شد.") }
                    .onFailure { toast("ساخت پشتیبان انجام نشد.") }
            }
            REQUEST_BACKUP_OPEN -> {
                val uri = data?.data ?: return
                val raw = runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                if (raw.isNullOrBlank()) {
                    toast("فایل پشتیبان قابل خواندن نیست.")
                    return
                }
                AlertDialog.Builder(this)
                    .setTitle("بازیابی اطلاعات")
                    .setMessage("پروژه‌های فایل پشتیبان با اطلاعات فعلی ادغام شوند یا جایگزین شوند؟")
                    .setPositiveButton("ادغام") { _, _ -> restoreBackup(raw, true) }
                    .setNeutralButton("جایگزینی") { _, _ -> restoreBackup(raw, false) }
                    .setNegativeButton("انصراف", null)
                    .show()
            }
        }
    }
'''
)

s = s.replace(
    "            Screen.PROJECT -> projectDetail()\n            Screen.SETTINGS -> settings()",
    "            Screen.PROJECT -> projectDetail()\n            Screen.OPTIMIZER -> optimizerPage()\n            Screen.SETTINGS -> settings()"
)

s = s.replace(
    '''        root.addView(button("همه ابزارهای تخصصی") { go(Screen.TOOLS) })
        root.addView(button("پروژه‌های ذخیره‌شده") { go(Screen.PROJECTS) })
        root.addView(card("🤖 پیشنهاد دستیار", "بعد از محاسبه، نکات ساخت، تعداد تقریبی ورق و راندمان اولیه هم نمایش داده می‌شود.", Color.WHITE))
''',
    '''        root.addView(button("همه ابزارهای تخصصی") { go(Screen.TOOLS) })
        root.addView(button("پروژه‌های ذخیره‌شده") { go(Screen.PROJECTS) })
        root.addView(card("🧩 بهینه‌ساز برش ورق", "چیدمان قطعات با Kerf، Rotation و نمودار هر ورق", soft[1]) { openLatestOptimizer() })
        root.addView(card("🤖 پیشنهاد دستیار", "بعد از محاسبه، نکات ساخت، تعداد تقریبی ورق و راندمان اولیه هم نمایش داده می‌شود.", Color.WHITE))
'''
)

s = s.replace("دستیار هوشمند کابینت‌سازی", "دستیار هوشمند کابینتسازی")
s = s.replace("نسخه 2.0.0", "نسخه 3.0.0")
p.write_text(s)
