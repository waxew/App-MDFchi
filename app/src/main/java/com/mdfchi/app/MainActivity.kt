package com.mdfchi.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Activity اصلی «دستیار هوشمند کابینتسازی» نسخه 3.0.0.
 * UI با Android Views استاندارد ساخته شده و موتور محاسبات، مدل‌ها و ذخیره‌سازی در فایل‌های جدا قرار دارند.
 */
class MainActivity : Activity() {

    /** صفحات اصلی برنامه. */
    private enum class Screen {
        HOME, TOOLS, CABINET, TOOL, PROJECTS, PROJECT, OPTIMIZER,
        SETTINGS, UPDATE, ABOUT, CONTACT, SOFTWARE
    }

    /** ابزارهای تخصصی غیر از چهار نوع یونیت. */
    private enum class SmartTool(val label: String, val emoji: String, val subtitle: String) {
        DOOR("محاسبه ابعاد درب", "🚪", "ابعاد هر لنگه با خلاصی قابل تنظیم"),
        DRAWER("محاسبه ابعاد کشو", "🗃️", "جعبه کشو، کف و خلاصی ریل"),
        HINGES("تعداد و محل لولا", "🔩", "تعداد پیشنهادی و فاصله نصب"),
        SHELVES("محاسبه طبقات", "📚", "ابعاد و متراژ طبقات"),
        BACK("محاسبه پشت‌بند", "🧱", "ابعاد و متراژ HDF"),
        COUNTERTOP("محاسبه صفحه کابینت", "📏", "متر طول و مساحت صفحه"),
        MDF_AREA("محاسبه متراژ MDF", "🪵", "مساحت قطعات MDF"),
        SHEETS("تعداد تقریبی ورق", "📐", "برآورد ورق با درصد پرت"),
        PVC("محاسبه نوار PVC", "🧵", "متراژ لبه‌های نوارخور"),
        WASTE("محاسبه پرت ورق", "♻️", "پرت و راندمان مصرف"),
        MDF_PRICE("محاسبه قیمت MDF", "💵", "ورق، قیمت واحد و حمل"),
        HARDWARE("محاسبه یراق‌آلات", "🪛", "لولا، ریل، دستگیره و پایه"),
        LABOR("محاسبه دستمزد", "👷", "متر طول و مبلغ ثابت"),
        FINAL_PRICE("قیمت نهایی پروژه", "🧮", "جمع هزینه‌ها و تخفیف"),
        CONVERTER("تبدیل واحد", "📐", "میلی‌متر، سانتی‌متر و متر")
    }

    /** تعریف یک ورودی فرم؛ مقدار واقعی در پروژه جدید خالی است و hint فقط نمونه را نشان می‌دهد. */
    private data class FieldSpec(val key: String, val label: String, val hint: String, val integer: Boolean = false, val optional: Boolean = false)

    /** خروجی عمومی ابزارهای کوچک. */
    private data class ToolOutput(val rows: List<Pair<String, String>>, val pieces: List<CutPiece> = emptyList(), val notes: List<String> = emptyList())

    private lateinit var store: ProjectStore
    private var current = Screen.HOME
    private val history = ArrayDeque<Screen>()
    private var cabinetType = CabinetType.FLOOR
    private var smartTool = SmartTool.DOOR
    private var editingProject: ProjectRecord? = null
    private var selectedProject: ProjectRecord? = null
    private var lastCalculation: CalculationResult? = null
    private var lastValues: Map<String, String> = emptyMap()
    private var pendingCsv: String? = null
    // پروژه‌ای که برای خروجی PDF انتخاب شده تا پس از برگشت از Document Picker مشخص بماند.
    private var pendingPdfProject: ProjectRecord? = null

    // رنگ‌های هویت بصری فانتزی/کارگاهی.
    private val wood = Color.rgb(126, 83, 55)
    private val woodDark = Color.rgb(72, 50, 38)
    private val cream = Color.rgb(255, 248, 239)
    private val ink = Color.rgb(47, 42, 38)
    private val soft = listOf(
        Color.rgb(255, 232, 205), Color.rgb(231, 242, 223),
        Color.rgb(226, 239, 249), Color.rgb(247, 226, 231)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProjectStore(this)
        window.statusBarColor = cream
        window.navigationBarColor = cream
        render(Screen.HOME)
    }

    /** Back فقط از Home برنامه را می‌بندد؛ در صفحات داخلی به صفحه قبلی برمی‌گردد. */
    @Deprecated("Custom navigation")
    override fun onBackPressed() = goBack()

    /** دریافت عکس پروفایل یا مقصد خروجی CSV. */
    @Deprecated("Legacy result API keeps the project dependency-light")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_PROFILE -> data?.data?.let { uri ->
                // مجوز Uri عکس پروفایل برای اجراهای بعدی برنامه Persist می‌شود.
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
                        // BOM باعث می‌شود فارسی در Excel/LibreOffice درست تشخیص داده شود.
                        it.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        it.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess { toast("لیست برش ذخیره شد.") }
                    .onFailure { toast("ذخیره فایل انجام نشد.") }
                pendingCsv = null
            }

            REQUEST_PDF -> {
                // گزارش PDF بعد از انتخاب مقصد توسط کاربر ساخته می‌شود.
                val uri = data?.data ?: return
                val project = pendingPdfProject ?: return
                runCatching {
                    ReportExporter.writeProjectPdf(this, uri, project, optimizeProject(project))
                }.onSuccess { toast("گزارش PDF ذخیره شد.") }
                    .onFailure { toast("ساخت PDF انجام نشد: ${it.message.orEmpty()}") }
                pendingPdfProject = null
            }

            REQUEST_BACKUP_CREATE -> {
                // Backup کامل پروژه‌ها و تنظیمات به JSON نوشته می‌شود.
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

                // Merge برای حفظ پروژه‌های فعلی و Replace برای بازیابی کامل در نظر گرفته شده است.
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


    /** ثبت صفحه فعلی و رفتن به مقصد. */
    private fun go(screen: Screen) {
        if (screen == current) return
        history.addLast(current)
        render(screen)
    }

    /** Back Stack داخلی. */
    private fun goBack() {
        when {
            history.isNotEmpty() -> render(history.removeLast())
            current != Screen.HOME -> render(Screen.HOME)
            else -> finish()
        }
    }

    /** Router صفحه‌ها. */
    private fun render(screen: Screen) {
        current = screen
        when (screen) {
            Screen.HOME -> home()
            Screen.TOOLS -> tools()
            Screen.CABINET -> cabinetForm()
            Screen.TOOL -> toolForm()
            Screen.PROJECTS -> projects()
            Screen.PROJECT -> projectDetail()
            Screen.OPTIMIZER -> optimizerPage()
            Screen.SETTINGS -> settings()
            Screen.UPDATE -> updatePage()
            Screen.ABOUT -> aboutUs()
            Screen.CONTACT -> contact()
            Screen.SOFTWARE -> aboutSoftware()
        }
    }

    /** اسکلت همه صفحات؛ Hamburger همیشه بالا سمت راست است. */
    private fun page(title: String, emoji: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(28))
            setBackgroundColor(cream)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        bar.addView(if (current == Screen.HOME) spacerW(48) else small("‹") { goBack() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        bar.addView(text("$emoji  $title", 22, true, woodDark).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT }, LinearLayout.LayoutParams(0, dp(56), 1f))
        bar.addView(small("☰") { drawer() }, LinearLayout.LayoutParams(dp(54), dp(48)))
        root.addView(bar)
        return root
    }

    /** خانه؛ چهار یونیت اصلی + ابزارها + پروژه‌ها. */
    private fun home() {
        val root = page("دستیار هوشمند کابینتسازی", "🧰")
        root.addView(text("محاسبه، برآورد و مدیریت پروژه برای کابینت‌سازها و MDFکارها", 14, false, ink))
        root.addView(card("🪚  📏  🔩  🪛  🪵", "از ابعاد یونیت تا لیست برش، ورق، PVC، یراق و قیمت نهایی", Color.rgb(255, 242, 220)))
        root.addView(section("محاسبات اصلی"))
        listOf(CabinetType.FLOOR, CabinetType.WALL, CabinetType.WARDROBE, CabinetType.TALL).forEachIndexed { i, type ->
            root.addView(card("${type.emoji} ${type.label}", "لیست برش و برآورد مواد مصرفی", soft[i]) { openCabinet(type) })
        }
        root.addView(button("همه ابزارهای تخصصی") { go(Screen.TOOLS) })
        root.addView(button("پروژه‌های ذخیره‌شده") { go(Screen.PROJECTS) })
        root.addView(card("🧩 بهینه‌ساز برش ورق", "چیدمان قطعات با Kerf، Rotation و نمودار هر ورق", soft[1]) { openLatestOptimizer() })
        root.addView(card("🤖 پیشنهاد دستیار", "بعد از محاسبه، نکات ساخت، تعداد تقریبی ورق و راندمان اولیه هم نمایش داده می‌شود.", Color.WHITE))
        scroll(root)
    }

    /** همه 19 ابزار تخصصی درخواست‌شده. */
    private fun tools() {
        val root = page("ابزارهای تخصصی", "🛠️")
        root.addView(text("کادرها در پروژه جدید خالی‌اند؛ متن کم‌رنگ فقط نمونه مقدار است.", 13, false, Color.DKGRAY))
        root.addView(section("یونیت‌ها"))
        listOf(CabinetType.FLOOR, CabinetType.WALL, CabinetType.WARDROBE, CabinetType.TALL).forEach { type ->
            root.addView(card("${type.emoji} ${type.label}", "تولید لیست برش، MDF، PVC و ورق", Color.WHITE) { openCabinet(type) })
        }
        root.addView(section("محاسبات جزئی و مالی"))
        SmartTool.entries.forEachIndexed { i, tool ->
            root.addView(card("${tool.emoji} ${tool.label}", tool.subtitle, soft[i % soft.size]) {
                smartTool = tool
                go(Screen.TOOL)
            })
        }
        scroll(root)
    }

    private fun openCabinet(type: CabinetType) {
        cabinetType = type
        editingProject = null
        lastCalculation = null
        lastValues = emptyMap()
        go(Screen.CABINET)
    }

    /** فرم چهار نوع یونیت؛ Label بالای باکس و Hint داخل آن. */
    private fun cabinetForm() {
        val root = page(cabinetType.label, cabinetType.emoji)
        val initial = editingProject?.values ?: emptyMap()
        root.addView(if (editingProject == null)
            text("مقادیر نمونه فقط Hint هستند و لازم نیست چیزی را پاک کنید.", 13, false, Color.DKGRAY)
        else card("حالت ویرایش", "مقادیر واقعی پروژه قبلی برای ویرایش بارگذاری شده‌اند.", soft[2]))

        val specs = mutableListOf(
            FieldSpec("name", "نام پروژه", "مثال: آشپزخانه احمدی"),
            FieldSpec("width", "عرض یونیت (cm)", "مثال: 80"),
            FieldSpec("height", "ارتفاع یونیت (cm)", if (cabinetType in listOf(CabinetType.WARDROBE, CabinetType.TALL)) "مثال: 240" else "مثال: 72"),
            FieldSpec("depth", "عمق یونیت (cm)", if (cabinetType == CabinetType.WALL) "مثال: 32" else "مثال: 55"),
            FieldSpec("thickness", "ضخامت MDF (cm)", "مثال: 1.6"),
            FieldSpec("shelves", "تعداد طبقات", "مثال: 2", integer = true, optional = true)
        )
        if (cabinetType == CabinetType.WARDROBE) specs += FieldSpec("verticalDividers", "تعداد تقسیمات عمودی", "مثال: 1", integer = true, optional = true)
        specs += listOf(
            FieldSpec("doors", "تعداد درب", "مثال: 2", integer = true),
            FieldSpec("backThickness", "ضخامت پشت‌بند/HDF (cm)", "مثال: 0.3", optional = true),
            FieldSpec("wastePercent", "درصد پرت اولیه", "مثال: 10", optional = true)
        )
        val fields = buildFields(root, specs, initial)
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(button("محاسبه هوشمند و تولید لیست برش") {
            runCatching {
                val values = fields.mapValues { it.value.text.toString().trim() }
                val w = number(fields.getValue("width"), "عرض")
                val h = number(fields.getValue("height"), "ارتفاع")
                val d = number(fields.getValue("depth"), "عمق")
                val t = number(fields.getValue("thickness"), "ضخامت MDF")
                val shelves = intOr(fields.getValue("shelves"), 0)
                val doors = integer(fields.getValue("doors"), "تعداد درب").coerceAtLeast(1)
                val back = numOr(fields.getValue("backThickness"), 0.3)
                val waste = numOr(fields.getValue("wastePercent"), 10.0)
                val calc = when (cabinetType) {
                    CabinetType.FLOOR -> CabinetEngine.floorCabinet(w, h, d, t, shelves, doors, back, waste)
                    CabinetType.WALL -> CabinetEngine.wallCabinet(w, h, d, t, shelves, doors, back, waste)
                    CabinetType.WARDROBE -> CabinetEngine.wardrobe(w, h, d, t, shelves, intOr(fields.getValue("verticalDividers"), 0), doors, back, waste)
                    CabinetType.TALL -> CabinetEngine.tallUnit(w, h, d, t, shelves, doors, back, waste)
                    CabinetType.LEGACY -> CabinetEngine.floorCabinet(w, h, d, t, shelves, doors, back, waste)
                }
                lastValues = values
                lastCalculation = calc
                showCabinetResult(result, calc)
            }.onFailure { error(result, it.message ?: "محاسبه انجام نشد.") }
        })
        root.addView(result)
        scroll(root)
    }

    /** نمایش Cut List، متراژ و پیشنهادهای دستیار. */
    private fun showCabinetResult(box: LinearLayout, calc: CalculationResult) {
        box.removeAllViews()
        box.addView(section("خلاصه"))
        listOf(
            "متراژ MDF" to "${fmt(calc.metrics["mdfAreaM2"])} m²",
            "MDF با پرت" to "${fmt(calc.metrics["areaWithWasteM2"])} m²",
            "تعداد تقریبی ورق ۱۸۳×۳۶۶" to "${(calc.metrics["sheetCount"] ?: 0.0).roundToInt()} ورق",
            "نوار PVC" to "${fmt(calc.metrics["pvcMeters"])} m",
            "راندمان تقریبی" to "${fmt(calc.metrics["estimatedEfficiencyPercent"])}٪"
        ).forEach { box.addView(resultCard(it.first, it.second)) }
        box.addView(section("لیست برش"))
        calc.pieces.forEach { p -> box.addView(card(p.name, "${fmt(p.lengthCm)} × ${fmt(p.widthCm)} cm | ${p.quantity} عدد | ${p.material}${if (p.pvcMeters() > 0) " | PVC ${fmt(p.pvcMeters())} m" else ""}", Color.WHITE)) }
        calc.notes.forEach { box.addView(card("🤖 پیشنهاد", it, soft[1])) }
        box.addView(button("ذخیره / بروزرسانی پروژه") { saveProject() })
        box.addView(button("خروجی CSV لیست برش") { exportCsv(lastValues["name"].orEmpty().ifBlank { cabinetType.label }, calc.pieces) })
    }

    /** ذخیره پروژه جدید یا ویرایش‌شده. */
    private fun saveProject() {
        val calc = lastCalculation ?: return toast("ابتدا محاسبه را انجام دهید.")
        val old = editingProject
        val now = System.currentTimeMillis()
        val project = ProjectRecord(
            id = old?.id ?: UUID.randomUUID().toString(),
            name = lastValues["name"].orEmpty().ifBlank { "${cabinetType.label} ${date(now)}" },
            type = cabinetType,
            createdAt = old?.createdAt ?: now,
            updatedAt = now,
            values = lastValues,
            cutList = calc.pieces,
            metrics = calc.metrics,
            notes = calc.notes
        )
        store.upsert(project)
        editingProject = project
        selectedProject = project
        toast("پروژه ذخیره شد.")
    }

    /** فرم عمومی ابزارهای کوچک؛ همه فیلدها از FieldSpec ساخته می‌شوند. */
    private fun toolForm() {
        val root = page(smartTool.label, smartTool.emoji)
        root.addView(text(smartTool.subtitle, 13, false, Color.DKGRAY))
        val fields = buildFields(root, toolSpecs(smartTool), emptyMap())
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // ابزار تبدیل واحد به‌جای فیلد دوم از Spinner استفاده می‌کند.
        if (smartTool == SmartTool.CONVERTER) {
            root.addView(label("واحد ورودی"))
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("میلی‌متر", "سانتی‌متر", "متر"))
            }
            root.addView(spinner, LinearLayout.LayoutParams(-1, dp(52)))
            root.addView(button("تبدیل") {
                result.removeAllViews()
                runCatching { converterOutput(number(fields.getValue("value"), "مقدار"), spinner) }
                    .onSuccess { out -> out.rows.forEach { result.addView(resultCard(it.first, it.second)) } }
                    .onFailure { error(result, it.message ?: "مقدار معتبر نیست.") }
            })
            root.addView(result)
            scroll(root)
            return
        }

        root.addView(button("محاسبه") {
            result.removeAllViews()
            runCatching { calculateTool(smartTool, fields) }
                .onSuccess { out ->
                    out.rows.forEach { result.addView(resultCard(it.first, it.second)) }
                    out.pieces.forEach { p -> result.addView(card(p.name, "${fmt(p.lengthCm)} × ${fmt(p.widthCm)} cm | ${p.quantity} عدد | ${p.material}", Color.WHITE)) }
                    out.notes.forEach { result.addView(card("توضیح", it, soft[0])) }
                }
                .onFailure { error(result, it.message ?: "مقادیر ورودی معتبر نیستند.") }
        })
        root.addView(result)
        scroll(root)
    }

    /** تعریف فیلدهای هر ابزار. */
    private fun toolSpecs(tool: SmartTool): List<FieldSpec> = when (tool) {
        SmartTool.DOOR -> listOf(f("w","عرض دهانه (cm)","مثال: 80"), f("h","ارتفاع دهانه (cm)","مثال: 72"), f("count","تعداد درب","مثال: 2", true), f("gap","خلاصی دور و بین درب‌ها (mm)","مثال: 2"))
        SmartTool.DRAWER -> listOf(f("w","عرض دهانه کشو (cm)","مثال: 60"), f("h","ارتفاع دهانه (cm)","مثال: 20"), f("d","عمق کابینت (cm)","مثال: 55"), f("t","ضخامت بدنه کشو (cm)","مثال: 1.6"), f("clear","خلاصی ریل در هر طرف (mm)","مثال: 12.5"), f("bottom","ضخامت کف (cm)","مثال: 0.3", optional=true))
        SmartTool.HINGES -> listOf(f("h","ارتفاع درب (cm)","مثال: 210"), f("weight","وزن تقریبی درب (kg) - اختیاری","مثال: 18", optional=true))
        SmartTool.SHELVES -> listOf(f("w","عرض داخلی (cm)","مثال: 76.8"), f("d","عمق داخلی (cm)","مثال: 55"), f("count","تعداد طبقات","مثال: 3", true), f("clear","عقب‌نشینی از جلو (cm)","مثال: 1", optional=true))
        SmartTool.BACK -> listOf(f("w","عرض کار (cm)","مثال: 80"), f("h","ارتفاع کار (cm)","مثال: 72"), f("clear","خلاصی کل (mm)","مثال: 2", optional=true))
        SmartTool.COUNTERTOP -> listOf(f("len","طول هر تکه صفحه (cm)","مثال: 300"), f("depth","عمق صفحه (cm)","مثال: 60"), f("count","تعداد تکه","مثال: 2", true))
        SmartTool.MDF_AREA -> listOf(f("len","طول قطعه (cm)","مثال: 72"), f("w","عرض قطعه (cm)","مثال: 55"), f("count","تعداد","مثال: 4", true))
        SmartTool.SHEETS -> listOf(f("area","متراژ MDF (m²)","مثال: 12"), f("sw","عرض ورق (cm)","مثال: 183"), f("sl","طول ورق (cm)","مثال: 366"), f("waste","درصد پرت","مثال: 10", optional=true))
        SmartTool.PVC -> listOf(f("len","طول قطعه (cm)","مثال: 72"), f("w","عرض قطعه (cm)","مثال: 55"), f("count","تعداد قطعه","مثال: 2", true), f("le","لبه طولی نوارخور","مثال: 1", true), f("se","لبه عرضی نوارخور","مثال: 0", true), f("waste","درصد پرت PVC","مثال: 5", optional=true))
        SmartTool.WASTE -> listOf(f("used","مساحت مصرف‌شده (m²)","مثال: 18"), f("bought","مساحت خریداری‌شده (m²)","مثال: 20.1"))
        SmartTool.MDF_PRICE -> listOf(f("count","تعداد ورق","مثال: 5"), f("price","قیمت هر ورق (تومان)","مثال: 2500000"), f("transport","حمل و هزینه جانبی","مثال: 500000", optional=true))
        SmartTool.HARDWARE -> listOf(f("hinges","تعداد لولا","مثال: 12"), f("hp","قیمت هر لولا","مثال: 120000"), f("rails","تعداد جفت ریل","مثال: 4"), f("rp","قیمت هر جفت ریل","مثال: 450000"), f("handles","تعداد دستگیره","مثال: 8"), f("handlep","قیمت هر دستگیره","مثال: 150000"), f("legs","تعداد پایه","مثال: 12"), f("legp","قیمت هر پایه","مثال: 70000"), f("misc","متفرقه","مثال: 500000", optional=true))
        SmartTool.LABOR -> listOf(f("meters","متر طول پروژه","مثال: 8.5"), f("ppm","دستمزد هر متر","مثال: 3000000"), f("extra","مبلغ ثابت/جانبی","مثال: 1000000", optional=true))
        SmartTool.FINAL_PRICE -> listOf(f("mdf","جمع MDF","مثال: 15000000"), f("hardware","جمع یراق","مثال: 4500000"), f("labor","دستمزد","مثال: 12000000"), f("top","صفحه کابینت","مثال: 6000000", optional=true), f("misc","هزینه متفرقه","مثال: 1000000", optional=true), f("discount","درصد تخفیف","مثال: 5", optional=true))
        SmartTool.CONVERTER -> listOf(f("value","مقدار","مثال: 1250"))
    }

    /** اجرای فرمول هر ابزار و تبدیل نتیجه به ردیف‌های UI. */
    private fun calculateTool(tool: SmartTool, fields: Map<String, EditText>): ToolOutput {
        fun n(k: String, label: String = k) = number(fields.getValue(k), label)
        fun i(k: String, label: String = k) = integer(fields.getValue(k), label)
        fun no(k: String, default: Double) = numOr(fields.getValue(k), default)
        return when (tool) {
            SmartTool.DOOR -> CabinetEngine.doorDimensions(n("w"), n("h"), i("count"), n("gap")).let { ToolOutput(listOf("عرض هر درب" to "${fmt(it["doorWidthCm"])} cm", "ارتفاع هر درب" to "${fmt(it["doorHeightCm"])} cm", "تعداد" to "${(it["doorCount"]?:1.0).roundToInt()} لنگه")) }
            SmartTool.DRAWER -> CabinetEngine.drawerDimensions(n("w"), n("h"), n("d"), n("t"), n("clear"), no("bottom",0.3)).let { ToolOutput(listOf("مساحت MDF" to "${fmt(it.metrics["mdfAreaM2"])} m²"), it.pieces, it.notes) }
            SmartTool.HINGES -> CabinetEngine.hinges(n("h"), no("weight",0.0)).let { ToolOutput(listOf("تعداد پیشنهادی" to "${it.first} لولا", "فاصله مراکز از بالای درب" to it.second.joinToString(" ، ") { p -> "${fmt(p)} cm" }), notes=listOf("برند لولا و وزن واقعی درب می‌تواند تعداد نهایی را تغییر دهد؛ دیتاشیت یراق اولویت دارد.")) }
            SmartTool.SHELVES -> CabinetEngine.shelves(n("w"), n("d"), i("count"), no("clear",1.0)).let { ToolOutput(listOf("مساحت MDF" to "${fmt(it.metrics["mdfAreaM2"])} m²"), it.pieces, it.notes) }
            SmartTool.BACK -> CabinetEngine.backPanel(n("w"), n("h"), no("clear",0.0)).let { ToolOutput(listOf("مساحت پشت‌بند" to "${fmt(it.pieces.sumOf { p -> p.areaM2() })} m²"), it.pieces) }
            SmartTool.COUNTERTOP -> CabinetEngine.countertop(n("len"), n("depth"), i("count")).let { ToolOutput(listOf("متر طول صفحه" to "${fmt(it["linearMeters"])} m", "مساحت صفحه" to "${fmt(it["areaM2"])} m²")) }
            SmartTool.MDF_AREA -> ToolOutput(listOf("متراژ MDF" to "${fmt(CabinetEngine.mdfArea(n("len"),n("w"),i("count")))} m²"))
            SmartTool.SHEETS -> CabinetEngine.sheetEstimate(n("area"),n("sw"),n("sl"),no("waste",10.0)).let { ToolOutput(listOf("مساحت هر ورق" to "${fmt(it["sheetAreaM2"])} m²", "متراژ با پرت" to "${fmt(it["adjustedAreaM2"])} m²", "تعداد تقریبی" to "${(it["sheetCount"]?:0.0).roundToInt()} ورق")) }
            SmartTool.PVC -> CabinetEngine.pvc(n("len"),n("w"),i("count"),i("le"),i("se"),no("waste",5.0)).let { ToolOutput(listOf("PVC خالص" to "${fmt(it["baseMeters"])} m", "PVC با پرت" to "${fmt(it["withWasteMeters"])} m")) }
            SmartTool.WASTE -> CabinetEngine.waste(n("used"),n("bought")).let { ToolOutput(listOf("مساحت پرت" to "${fmt(it["wasteAreaM2"])} m²", "درصد پرت" to "${fmt(it["wastePercent"])}٪", "راندمان" to "${fmt(it["efficiencyPercent"])}٪")) }
            SmartTool.MDF_PRICE -> ToolOutput(listOf("جمع MDF" to "${money(CabinetEngine.mdfPrice(n("count"),n("price"),no("transport",0.0)))} تومان"))
            SmartTool.HARDWARE -> ToolOutput(listOf("جمع یراق‌آلات" to "${money(CabinetEngine.hardwarePrice(n("hinges"),n("hp"),n("rails"),n("rp"),n("handles"),n("handlep"),n("legs"),n("legp"),no("misc",0.0)))} تومان"))
            SmartTool.LABOR -> ToolOutput(listOf("دستمزد کل" to "${money(CabinetEngine.labor(n("meters"),n("ppm"),no("extra",0.0)))} تومان"))
            SmartTool.FINAL_PRICE -> CabinetEngine.finalPrice(n("mdf"),n("hardware"),n("labor"),no("top",0.0),no("misc",0.0),no("discount",0.0)).let { ToolOutput(listOf("جمع قبل از تخفیف" to "${money(it["subtotal"]?:0.0)} تومان", "تخفیف" to "${money(it["discount"]?:0.0)} تومان", "قیمت نهایی" to "${money(it["final"]?:0.0)} تومان")) }
            SmartTool.CONVERTER -> throw IllegalStateException("converter-special")
        }
    }

    /** پروژه‌های ذخیره‌شده با جست‌وجوی لحظه‌ای نام و نوع پروژه. */
    private fun projects() {
        val root = page("پروژه‌های من", "📁")
        val all = store.loadProjects().sortedByDescending { it.updatedAt }

        root.addView(label("جست‌وجوی پروژه"))
        val search = EditText(this).apply {
            hint = "نام پروژه یا نوع کابینت را بنویسید"
            setHintTextColor(Color.rgb(160, 150, 142))
            setTextColor(ink)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(Color.WHITE, 14, Color.rgb(213, 195, 178))
            inputType = InputType.TYPE_CLASS_TEXT
        }
        root.addView(search, fieldParams())

        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox)

        fun renderList(query: String) {
            listBox.removeAllViews()
            val normalized = query.trim().lowercase(Locale.getDefault())
            val filtered = if (normalized.isBlank()) all else all.filter { project ->
                project.name.lowercase(Locale.getDefault()).contains(normalized) ||
                    project.type.label.lowercase(Locale.getDefault()).contains(normalized)
            }

            if (filtered.isEmpty()) {
                listBox.addView(
                    card(
                        if (all.isEmpty()) "هنوز پروژه‌ای نیست" else "نتیجه‌ای پیدا نشد",
                        if (all.isEmpty()) "از یکی از چهار نوع یونیت یک پروژه بسازید." else "عبارت جست‌وجو را تغییر دهید.",
                        Color.WHITE
                    )
                )
            }

            filtered.forEach { project ->
                listBox.addView(
                    card(
                        "${project.type.emoji} ${project.name}",
                        "${project.type.label} • ${date(project.updatedAt)}\n${project.cutList.size} ردیف لیست برش",
                        Color.WHITE
                    ) {
                        selectedProject = project
                        go(Screen.PROJECT)
                    }
                )
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        renderList("")
        scroll(root)
    }


    /** نمایش، ویرایش، کپی، حذف و خروجی پروژه. */
    private fun projectDetail() {
        val p = selectedProject ?: return render(Screen.PROJECTS)
        val root = page(p.name, p.type.emoji)
        root.addView(card(p.type.label, "آخرین ویرایش: ${date(p.updatedAt)}", soft[2]))
        p.metrics["mdfAreaM2"]?.let { root.addView(resultCard("متراژ MDF","${fmt(it)} m²")) }
        p.metrics["sheetCount"]?.let { root.addView(resultCard("ورق تقریبی","${it.roundToInt()} ورق")) }
        p.metrics["pvcMeters"]?.let { root.addView(resultCard("PVC","${fmt(it)} m")) }
        root.addView(section("لیست برش"))
        p.cutList.forEach { part -> root.addView(card(part.name,"${fmt(part.lengthCm)} × ${fmt(part.widthCm)} cm | ${part.quantity} عدد | ${part.material}",Color.WHITE)) }
        root.addView(button("ویرایش پروژه") { cabinetType=if(p.type==CabinetType.LEGACY) CabinetType.FLOOR else p.type; editingProject=p; lastValues=p.values; lastCalculation=null; go(Screen.CABINET) })
        root.addView(button("کپی پروژه") { selectedProject=store.copy(p); toast("کپی پروژه ساخته شد."); render(Screen.PROJECT) })
        root.addView(button("بهینه‌سازی برش روی ورق") { go(Screen.OPTIMIZER) })
        root.addView(button("گزارش PDF پروژه") { exportPdf(p) })
        root.addView(button("خروجی CSV") { exportCsv(p.name,p.cutList) })
        root.addView(button("حذف پروژه") { AlertDialog.Builder(this).setTitle("حذف پروژه").setMessage("پروژه «${p.name}» حذف شود؟").setPositiveButton("حذف") { _,_-> store.delete(p.id); selectedProject=null; render(Screen.PROJECTS) }.setNegativeButton("انصراف",null).show() })
        scroll(root)
    }

    /** تنظیمات اعلان‌ها، ورق، تیغه و Backup/Restore. */
    private fun settings() {
        val root = page("تنظیمات", "⚙️")

        root.addView(Switch(this).apply {
            text = "اعلان‌های برنامه"
            textSize = 17f
            setTextColor(ink)
            isChecked = store.notificationsEnabled()
            setOnCheckedChangeListener { _, value -> store.setNotificationsEnabled(value) }
        })
        root.addView(Switch(this).apply {
            text = "اعلان انتشار نسخه جدید"
            textSize = 16f
            setTextColor(ink)
            isChecked = store.updateAlertsEnabled()
            setOnCheckedChangeListener { _, value -> store.setUpdateAlertsEnabled(value) }
        })

        root.addView(section("تنظیمات کارگاه و بهینه‌ساز"))
        root.addView(label("عرض ورق پیش‌فرض (cm)"))
        val sheetWidth = settingField(fmt(store.sheetWidthCm()), "مثال: 183")
        root.addView(sheetWidth, fieldParams())
        root.addView(label("طول ورق پیش‌فرض (cm)"))
        val sheetLength = settingField(fmt(store.sheetLengthCm()), "مثال: 366")
        root.addView(sheetLength, fieldParams())
        root.addView(label("ضخامت برش تیغه / Kerf (mm)"))
        val kerf = settingField(fmt(store.kerfMm()), "مثال: 3.2")
        root.addView(kerf, fieldParams())
        val rotation = Switch(this).apply {
            text = "اجازه چرخش ۹۰ درجه قطعات در چیدمان"
            textSize = 15f
            setTextColor(ink)
            isChecked = store.allowRotation()
        }
        root.addView(rotation)
        root.addView(button("ذخیره تنظیمات کارگاه") {
            runCatching {
                store.setWorkshopSettings(
                    sheetWidthCm = number(sheetWidth, "عرض ورق"),
                    sheetLengthCm = number(sheetLength, "طول ورق"),
                    kerfMm = number(kerf, "Kerf"),
                    allowRotation = rotation.isChecked
                )
            }.onSuccess { toast("تنظیمات کارگاه ذخیره شد.") }
                .onFailure { toast(it.message ?: "تنظیمات معتبر نیستند.") }
        })

        root.addView(section("پشتیبان‌گیری اطلاعات"))
        root.addView(card("اطلاعات پروژه‌ها", "پروژه‌ها محلی هستند، هنگام Update باقی می‌مانند و Backup قابل حمل دارند.", Color.WHITE))
        root.addView(button("تهیه فایل پشتیبان JSON") { createBackupFile() })
        root.addView(button("بازیابی فایل پشتیبان") { openBackupFile() })
        root.addView(button("تنظیمات سیستمی برنامه") {
            runCatching {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        })
        scroll(root)
    }


    /** Update Checker عمومی GitHub. */
    private fun updatePage() {
        val root=page("بروزرسانی","🔄")
        val status=text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}",15,true,ink)
        var url:String?=null
        root.addView(card("آپدیت‌خور","شناسه برنامه و کلید امضا ثابت مانده‌اند؛ نسخه 2 روی نسخه 1.x نصب می‌شود.",soft[1]))
        root.addView(status)
        root.addView(button("بررسی نسخه جدید") { status.text="در حال بررسی…"; Thread { runCatching { JSONObject(URL("https://raw.githubusercontent.com/waxew/App-MDFchi/main/version.json").readText()) }.onSuccess { j-> val code=j.optInt("versionCode",BuildConfig.VERSION_CODE); url=j.optString("downloadUrl").takeIf{it.isNotBlank()}; runOnUiThread { status.text=if(code>BuildConfig.VERSION_CODE) "نسخه جدید ${j.optString("versionName")} موجود است." else "آخرین نسخه نصب است." } }.onFailure { runOnUiThread { status.text="بررسی نسخه انجام نشد." } } }.start() })
        root.addView(button("صفحه دریافت نسخه") { url?.let { runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(it))) } } ?: run { status.text="ابتدا بررسی نسخه را اجرا کنید." } })
        scroll(root)
    }

    private fun aboutUs() { val root=page("درباره ما","👥"); root.addView(spacer(24)); root.addView(center("گروه توسعه فناوری و نرم افزاری as Team",20,true,woodDark)); root.addView(center("طراحی و توسعه ابزارهای کاربردی، سبک و قابل توسعه برای کاربران فارسی‌زبان.",14,false,ink)); scroll(root) }
    private fun contact() { val root=page("ارتباط با ما","✉️"); root.addView(card("پشتیبانی","برای گزارش خطا، پیشنهاد فرمول یا قابلیت جدید با ما در ارتباط باشید.",soft[2])); root.addView(button("ارسال ایمیل") { runCatching { startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:AS.Developers.Support@Gmail.Com"))) } }); root.addView(spacer(120)); root.addView(divider()); root.addView(center("گروه توسعه فناوری و نرم افزاری as Team",16,true,ink)); root.addView(center("AS.Developers.Support@Gmail.Com",14,false,wood)); scroll(root) }
    /** درباره نرم‌افزار طبق قالب نهایی مشترک؛ بدون نمایش Package Name یا اطلاعات فنی اضافی. */
    private fun aboutSoftware() {
        val root = page("درباره نرم‌افزار", "ℹ️")
        root.addView(center("دستیار هوشمند کابینتسازی", 21, true, woodDark))
        root.addView(center("یک دستیار تخصصی آفلاین برای محاسبه کابینت و کمد، مدیریت پروژه، تولید لیست برش، برآورد MDF/PVC/یراق و بهینه‌سازی چیدمان قطعات روی ورق.", 14, false, ink))
        root.addView(center("امکانات خروجی PDF و CSV، Backup/Restore و نگهداری پروژه‌ها باعث می‌شود اطلاعات کارگاه همیشه قابل انتقال و بازیابی باشند.", 14, false, ink))
        root.addView(spacer(14))
        root.addView(divider())
        root.addView(spacer(10))
        root.addView(center("راه‌های ارتباطی با ما:", 15, true, ink))
        root.addView(center("AS.Developers.Support@Gmail.Com", 14, false, wood))
        root.addView(spacer(16))
        root.addView(center("نسخه ${BuildConfig.VERSION_NAME}", 16, true, ink))
        root.addView(spacer(90))
        root.addView(divider())
        root.addView(center("Develop by AS Team Group", 14, true, ink))
        scroll(root)
    }


    /** Drawer نهایی مشترک: پروفایل، تنظیمات/اشتراک، گزینه‌های اختصاصی و درباره نرم‌افزار. */
    private fun drawer() {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(236, 222, 205))
            }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            setOnClickListener { pickProfile() }
        }
        store.profileUri()?.let { runCatching { image.setImageURI(Uri.parse(it)) } }
        if (image.drawable == null) image.setImageResource(android.R.drawable.ic_menu_camera)
        body.addView(image, LinearLayout.LayoutParams(dp(96), dp(96)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        body.addView(center("👤  ${store.userName()}", 16, true, ink).apply { setOnClickListener { editName() } })
        body.addView(center("برای تغییر عکس روی تصویر بزنید", 11, false, Color.GRAY))
        body.addView(divider())

        lateinit var popup: PopupWindow
        fun item(icon: String, label: String, action: () -> Unit) {
            body.addView(drawerRow(icon, label) {
                popup.dismiss()
                action()
            })
        }

        popup = PopupWindow(
            body,
            (resources.displayMetrics.widthPixels * 0.86).toInt(),
            LinearLayout.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            isOutsideTouchable = true
            elevation = dp(18).toFloat()
        }

        // دو گزینه اول طبق قالب مشترک همه برنامه‌ها ثابت هستند.
        item("⚙️", "تنظیمات") { go(Screen.SETTINGS) }
        item("📤", "معرفی به دوستان") { share() }
        body.addView(divider())

        // گزینه‌های اختصاصی دستیار کابینتسازی.
        item("🏠", "خانه") { history.clear(); render(Screen.HOME) }
        item("🛠️", "ابزارهای تخصصی") { go(Screen.TOOLS) }
        item("📁", "پروژه‌های من") { go(Screen.PROJECTS) }
        item("🧩", "بهینه‌ساز برش") { openLatestOptimizer() }
        item("🔄", "بروزرسانی") { go(Screen.UPDATE) }
        body.addView(divider())

        // راه‌های تماس داخل About Software قرار دارند؛ آیتم جداگانه Contact حذف شده است.
        item("ℹ️", "درباره نرم‌افزار") { go(Screen.SOFTWARE) }
        body.addView(center("نسخه ${BuildConfig.VERSION_NAME}", 11, false, Color.GRAY))
        popup.showAtLocation(window.decorView, Gravity.RIGHT or Gravity.TOP, 0, 0)
    }


    private fun pickProfile(){ startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)},REQUEST_PROFILE) }
    private fun editName(){ val input=EditText(this).apply{hint="نام کاربر";if(store.userName()!="کاربر")setText(store.userName())}; AlertDialog.Builder(this).setTitle("نام کاربر").setView(input).setPositiveButton("ذخیره"){_,_->store.setUserName(input.text.toString().trim())}.setNegativeButton("انصراف",null).show() }
    private fun share(){ val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,"دستیار هوشمند کابینتسازی");putExtra(Intent.EXTRA_TEXT,"دستیار هوشمند کابینتسازی؛ ابزار محاسبات کابینت و MDF\nhttps://github.com/waxew/App-MDFchi")}; startActivity(Intent.createChooser(intent,"معرفی برنامه")) }

    /** Converter به Spinner نیاز دارد؛ برای آن صفحه را بعد از ساخت فیلد استاندارد تکمیل می‌کنیم. */
    private fun buildFields(
        parent: LinearLayout,
        specs: List<FieldSpec>,
        initial: Map<String, String>
    ): Map<String, EditText> {
        val out = linkedMapOf<String, EditText>()
        specs.forEach { spec ->
            val labelText = if (spec.optional && !spec.label.contains("اختیاری")) {
                "${spec.label} (اختیاری)"
            } else {
                spec.label
            }
            parent.addView(label(labelText))

            val edit = EditText(this).apply {
                hint = spec.hint
                setHintTextColor(Color.rgb(160, 150, 142))
                // فرم جدید خالی است؛ فقط در Edit پروژه قبلی مقدار واقعی Load می‌شود.
                setText(initial[spec.key].orEmpty())
                textSize = 15f
                setTextColor(ink)
                textDirection = View.TEXT_DIRECTION_RTL
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(8), dp(14), dp(8))
                background = rounded(Color.WHITE, 14, Color.rgb(213, 195, 178))
                inputType = when {
                    spec.key == "name" -> InputType.TYPE_CLASS_TEXT
                    spec.integer -> InputType.TYPE_CLASS_NUMBER
                    else -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
            }
            parent.addView(edit, fieldParams())
            out[spec.key] = edit
        }
        return out
    }

    /** باز کردن Optimizer برای آخرین پروژه ذخیره‌شده. */
    private fun openLatestOptimizer() {
        val latest = store.loadProjects().maxByOrNull { it.updatedAt }
        if (latest == null) {
            toast("ابتدا یک پروژه ذخیره کنید.")
            return
        }
        selectedProject = latest
        go(Screen.OPTIMIZER)
    }

    /** اجرای Optimizer با تنظیمات فعلی ورق و تیغه. */
    private fun optimizeProject(project: ProjectRecord): CutOptimizationResult =
        CutOptimizer.optimize(
            pieces = project.cutList,
            sheetWidthCm = store.sheetWidthCm(),
            sheetLengthCm = store.sheetLengthCm(),
            kerfMm = store.kerfMm(),
            allowRotation = store.allowRotation()
        )

    /** صفحه چیدمان قطعات همراه نمودار، راندمان و مختصات. */
    private fun optimizerPage() {
        val project = selectedProject ?: return render(Screen.PROJECTS)
        val root = page("بهینه‌ساز برش", "🧩")
        root.addView(
            card(
                project.name,
                "${project.type.label} • ورق ${fmt(store.sheetWidthCm())}×${fmt(store.sheetLengthCm())} cm • Kerf ${fmt(store.kerfMm())} mm",
                soft[2]
            )
        )

        val optimization = runCatching { optimizeProject(project) }.getOrElse {
            root.addView(card("بهینه‌سازی انجام نشد", it.message ?: "خطای ناشناخته", Color.rgb(255, 228, 220)))
            scroll(root)
            return
        }

        root.addView(resultCard("تعداد ورق", "${optimization.sheets.size} ورق"))
        root.addView(resultCard("راندمان کل", "${fmt(optimization.efficiencyPercent)}٪"))
        root.addView(resultCard("مساحت مصرف‌شده", "${fmt(optimization.usedAreaM2)} m²"))
        root.addView(resultCard("پرت سطحی تقریبی", "${fmt(optimization.wasteAreaM2)} m²"))
        root.addView(resultCard("چرخش قطعات", if (optimization.allowRotation) "فعال" else "غیرفعال"))

        if (optimization.unplaced.isNotEmpty()) {
            val message = optimization.unplaced.joinToString("\n") { piece ->
                "${piece.name}: ${fmt(piece.lengthCm)}×${fmt(piece.widthCm)} cm — ${piece.quantity} عدد"
            }
            root.addView(card("⚠️ قطعات خارج از ابعاد ورق", message, Color.rgb(255, 228, 220)))
        }

        root.addView(section("نمودار چیدمان"))
        root.addView(NestingView(this, optimization), LinearLayout.LayoutParams(-1, -2))
        root.addView(section("مختصات قطعات"))

        optimization.sheets.forEach { sheet ->
            root.addView(
                card(
                    "ورق ${sheet.index}",
                    "راندمان ${fmt(sheet.efficiencyPercent())}٪ • ${sheet.placements.size} قطعه",
                    soft[(sheet.index - 1) % soft.size]
                )
            )
            sheet.placements.forEach { placement ->
                root.addView(
                    card(
                        placement.source.name,
                        "X=${fmt(placement.xCm)} • Y=${fmt(placement.yCm)} • ${fmt(placement.heightCm)}×${fmt(placement.widthCm)} cm${if (placement.rotated) " • چرخش ۹۰°" else ""}",
                        Color.WHITE
                    )
                )
            }
        }

        root.addView(button("گزارش PDF پروژه") { exportPdf(project) })
        root.addView(button("تنظیم ابعاد ورق و Kerf") { go(Screen.SETTINGS) })
        scroll(root)
    }

    /** شروع ساخت PDF در محل انتخائ‌شده توسط کاربر. */
    private fun exportPdf(project: ProjectRecord) {
        pendingPdfProject = project
        val safe = safeFileName(project.name, "project")
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "$safe-report.pdf")
            },
            REQUEST_PDF
        )
    }

    /** ساخت Backup JSON. */
    private fun createBackupFile() {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "cabinet-assistant-backup-$stamp.json")
            },
            REQUEST_BACKUP_CREATE
        )
    }

    /** انتخاب Backup JSON برای بازیابی. */
    private fun openBackupFile() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            },
            REQUEST_BACKUP_OPEN
        )
    }

    /** بازیابی Backup و اعلام تعداد پروژه‌های خوانده‌شده. */
    private fun restoreBackup(raw: String, merge: Boolean) {
        runCatching { store.restoreBackup(raw, merge) }
            .onSuccess { count ->
                toast("$count پروژه از فایل پشتیبان بازیابی شد.")
                render(Screen.SETTINGS)
            }
            .onFailure { toast("بازیابی انجام نشد: ${it.message.orEmpty()}") }
    }

    /** فیلد تنظیمات؛ اینجا مقدار واقعی فعلی عمداً نمایش داده می‌شود چون کاربر در حال Edit تنظیمات است. */
    private fun settingField(value: String, hintText: String): EditText = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.rgb(160, 150, 142))
        setText(value)
        textSize = 15f
        setTextColor(ink)
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        textDirection = View.TEXT_DIRECTION_RTL
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = rounded(Color.WHITE, 14, Color.rgb(213, 195, 178))
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    /** LayoutParams مشترک کادرهای ورودی. */
    private fun fieldParams() = LinearLayout.LayoutParams(-1, dp(54)).apply {
        setMargins(0, 0, 0, dp(8))
    }


    /** برای Converter چون Spinner جزو Map فیلدها نیست، محاسبه آن جدا انجام می‌شود. */
    private fun converterOutput(value:Double,spinner:Spinner):ToolOutput{ val unit=when(spinner.selectedItemPosition){0->"mm";1->"cm";else->"m"}; val d=CabinetEngine.convertLength(value,unit); return ToolOutput(listOf("میلی‌متر" to "${fmt(d["mm"])} mm","سانتی‌متر" to "${fmt(d["cm"])} cm","متر" to "${fmt(d["m"])} m")) }

    /** FieldSpec helper. */
    private fun f(k:String,l:String,h:String,integer:Boolean=false,optional:Boolean=false)=FieldSpec(k,l,h,integer,optional)

    /** خروجی CSV با Storage Access Framework. */
    private fun exportCsv(name: String, pieces: List<CutPiece>) {
        if (pieces.isEmpty()) return toast("لیست برش خالی است.")
        pendingCsv = buildString {
            appendLine("پروژه,$name")
            appendLine("قطعه,طول_cm,عرض_cm,تعداد,متریال,PVC_m,توضیح")
            pieces.forEach {
                appendLine("\"${it.name}\",${fmt(it.lengthCm)},${fmt(it.widthCm)},${it.quantity},\"${it.material}\",${fmt(it.pvcMeters())},\"${it.note}\"")
            }
        }
        val safe = safeFileName(name, "cutlist")
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "$safe-cutlist.csv")
            },
            REQUEST_CSV
        )
    }

    /** پاک‌سازی نام فایل بدون حذف حروف فارسی. */
    private fun safeFileName(value: String, fallback: String): String =
        value.replace(Regex("[^\u0600-\u06FFa-zA-Z0-9_-]+"), "_").ifBlank { fallback }


    // ---------- UI helpers ----------
    private fun card(title:String,subtitle:String,bg:Int,action:(()->Unit)?=null):View=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(15),dp(13),dp(15),dp(13));background=rounded(bg,18);isClickable=action!=null;isFocusable=action!=null;if(action!=null)setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(5),0,dp(5))};addView(text(title,17,true,ink));addView(text(subtitle,13,false,Color.rgb(92,82,74)))}
    private fun resultCard(t:String,v:String)=card(t,v,Color.WHITE)
    private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;textSize=14f;setTextColor(Color.WHITE);isAllCaps=false;background=rounded(wood,15);setOnClickListener{a()};layoutParams=LinearLayout.LayoutParams(-1,dp(52)).apply{setMargins(0,dp(6),0,dp(6))}}
    private fun small(t:String,a:()->Unit)=TextView(this).apply{text=t;textSize=27f;setTextColor(wood);gravity=Gravity.CENTER;background=rounded(Color.rgb(255,239,220),14);setOnClickListener{a()}}
    private fun drawerRow(icon:String,title:String,a:()->Unit)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(10),dp(8),dp(10),dp(8));background=rounded(Color.rgb(255,250,244),14);setOnClickListener{a()};layoutParams=LinearLayout.LayoutParams(-1,dp(52));addView(text(icon,21,false,ink).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(40),-1));addView(text(title,15,true,ink).apply{gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT},LinearLayout.LayoutParams(0,-1,1f))}
    private fun text(v:String,s:Int,b:Boolean,c:Int)=TextView(this).apply{text=v;textSize=s.toFloat();setTextColor(c);gravity=Gravity.RIGHT;textDirection=View.TEXT_DIRECTION_RTL;if(b)setTypeface(typeface,Typeface.BOLD);setLineSpacing(0f,1.12f)}
    private fun center(v:String,s:Int,b:Boolean,c:Int)=text(v,s,b,c).apply{gravity=Gravity.CENTER_HORIZONTAL;textAlignment=View.TEXT_ALIGNMENT_CENTER;setPadding(dp(8),dp(5),dp(8),dp(5))}
    private fun label(v:String)=text(v,13,true,ink).apply{setPadding(dp(4),dp(4),dp(4),dp(4))}
    private fun section(v:String)=text(v,17,true,woodDark).apply{setPadding(0,dp(14),0,dp(6))}
    private fun rounded(fill:Int,radius:Int,stroke:Int?=null)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;setColor(fill);cornerRadius=dp(radius).toFloat();if(stroke!=null)setStroke(dp(1),stroke)}
    private fun divider()=View(this).apply{setBackgroundColor(Color.rgb(229,218,207));layoutParams=LinearLayout.LayoutParams(-1,dp(1))}
    private fun spacer(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(h))}
    private fun spacerW(w:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(dp(w),1)}
    private fun scroll(v:View)=setContentView(ScrollView(this).apply{setBackgroundColor(cream);addView(v)})
    private fun error(box:LinearLayout,msg:String){box.removeAllViews();box.addView(card("ورودی نامعتبر",msg,Color.rgb(255,228,220)))}
    private fun toast(v:String){Toast.makeText(this,v,Toast.LENGTH_SHORT).show()}

    // ---------- input/format helpers ----------
    /** تبدیل ارقام فارسی/عربی و جداکننده‌ها به فرمت عدد استاندارد. */
    private fun normalizeNumber(value: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        return buildString {
            value.trim().forEach { char ->
                when {
                    persian.indexOf(char) >= 0 -> append('0' + persian.indexOf(char))
                    arabic.indexOf(char) >= 0 -> append('0' + arabic.indexOf(char))
                    char == '٫' || char == ',' || char == '٬' -> append('.')
                    else -> append(char)
                }
            }
        }
    }

    private fun number(e: EditText, label: String): Double =
        normalizeNumber(e.text.toString()).toDoubleOrNull()?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("$label را وارد کنید.")

    private fun integer(e: EditText, label: String): Int =
        normalizeNumber(e.text.toString()).toIntOrNull()?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("$label را به‌صورت عدد صحیح وارد کنید.")

    private fun numOr(e: EditText, default: Double) =
        normalizeNumber(e.text.toString()).toDoubleOrNull() ?: default

    private fun intOr(e: EditText, default: Int) =
        normalizeNumber(e.text.toString()).toIntOrNull() ?: default

    private fun fmt(v:Double?):String{val n=v?:0.0;return if(abs(n-n.roundToInt())<0.0001)n.roundToInt().toString() else String.format(Locale.US,"%.2f",n)}
    private fun money(v:Double)=NumberFormat.getIntegerInstance(Locale("fa","IR")).format(v.toLong())
    private fun date(t:Long)=SimpleDateFormat("yyyy/MM/dd",Locale.US).format(Date(t))
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_PROFILE = 1001
        private const val REQUEST_CSV = 1002
        private const val REQUEST_PDF = 1003
        private const val REQUEST_BACKUP_CREATE = 1004
        private const val REQUEST_BACKUP_OPEN = 1005
    }
}
