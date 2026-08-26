package com.mdfchi.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mdfchi_projects", Context.MODE_PRIVATE) }
    private val wood = Color.rgb(139, 94, 60)
    private val cream = Color.rgb(255, 248, 239)
    private val ink = Color.rgb(48, 42, 38)
    private var isHome = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cream
        window.navigationBarColor = cream
        showHome()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isHome) super.onBackPressed() else showHome()
    }

    private fun basePage(title: String, emoji: String, back: Boolean = true): LinearLayout {
        isHome = !back
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(28))
            setBackgroundColor(cream)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (back) {
            top.addView(smallAction("‹") { showHome() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        } else {
            top.addView(spaceWidth(48))
        }
        top.addView(text("$emoji  $title", 26, true, wood).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        top.addView(smallAction("☰") { openHamburger() }, LinearLayout.LayoutParams(dp(54), dp(48)))
        root.addView(top)
        return root
    }

    private fun openHamburger() {
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            setBackgroundColor(Color.WHITE)
        }
        drawer.addView(text("🧰  MDFچی", 24, true, wood).apply { gravity = Gravity.CENTER_HORIZONTAL })
        drawer.addView(text("جعبه ابزار کابینت‌ساز", 13, false, Color.DKGRAY).apply { gravity = Gravity.CENTER_HORIZONTAL })
        drawer.addView(space(18))

        lateinit var popup: PopupWindow
        fun item(icon: String, label: String, action: () -> Unit) {
            drawer.addView(drawerItem(icon, label) {
                popup.dismiss()
                action()
            })
        }

        popup = PopupWindow(
            drawer,
            (resources.displayMetrics.widthPixels * 0.84f).toInt(),
            LinearLayout.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            isOutsideTouchable = true
            elevation = dp(18).toFloat()
        }

        item("⚙️", "تنظیمات") { showSettings() }
        item("📤", "معرفی به دوستان") { shareApp() }
        item("👥", "درباره ما") { showAboutUs() }
        item("✉️", "تماس با ما") { showContactUs() }
        item("🧰", "درباره نرم‌افزار") { showAboutSoftware() }
        drawer.addView(space(12))
        drawer.addView(text("نسخه ${BuildConfig.VERSION_NAME}", 12, false, Color.GRAY).apply { gravity = Gravity.CENTER_HORIZONTAL })
        popup.showAtLocation(window.decorView, Gravity.END or Gravity.TOP, 0, 0)
    }

    private fun drawerItem(icon: String, label: String, action: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(255, 249, 242), 16)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, dp(4), 0, dp(4)) }
        }
        row.addView(text(label, 16, true, ink).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT }, LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(text(icon, 22, false, ink).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(42), -1))
        return row
    }

    private fun showHome() {
        val root = basePage("MDFچی", "🧰", false)
        root.addView(text("جعبه ابزار کابینت‌ساز", 16, false, ink))
        root.addView(space(14))
        root.addView(card("📏  🔩  🪛  🔨  🪚  🪵", "ابزارهای کارگاه همیشه دم دستت", Color.rgb(255, 243, 221)))
        root.addView(space(12))
        root.addView(card("📐 محاسبه یونیت", "ابعاد قطعات و لیست برش", Color.rgb(255, 228, 200)) { showUnitCalculator() })
        root.addView(card("🪵 تعداد ورق", "تخمین MDF با درصد پرت", Color.rgb(231, 242, 223)) { showSheetEstimator() })
        root.addView(card("🧮 محاسبه هزینه", "ورق، یراق و دستمزد", Color.rgb(221, 236, 247)) { showCostCalculator() })
        root.addView(card("🧰 پروژه‌های من", "مشاهده پروژه‌های ذخیره‌شده", Color.rgb(246, 221, 227)) { showProjects() })
        root.addView(card("🔧 بروزرسانی", "بررسی نسخه جدید برنامه", Color.rgb(255, 232, 168)) { showUpdate() })
        setScrollable(root)
    }

    private fun showSettings(scrollToNotifications: Boolean = false) {
        val root = basePage("تنظیمات", "⚙️")
        root.addView(card("تنظیمات برنامه", "تنظیمات MDFچی روی همین دستگاه ذخیره می‌شود.", Color.rgb(255, 243, 221)))

        val notifications = Switch(this).apply {
            text = "اعلان‌ها"
            textSize = 17f
            setTextColor(ink)
            isChecked = prefs.getBoolean("notifications_enabled", true)
            setPadding(dp(12), dp(if (scrollToNotifications) 20 else 12), dp(12), dp(12))
            setOnCheckedChangeListener { _, enabled -> prefs.edit().putBoolean("notifications_enabled", enabled).apply() }
        }
        root.addView(notifications)
        root.addView(text("برای اطلاع‌رسانی‌های داخلی برنامه و نسخه‌های جدید.", 13, false, Color.DKGRAY))

        val updateAlerts = Switch(this).apply {
            text = "اعلان انتشار نسخه جدید"
            textSize = 16f
            setTextColor(ink)
            isChecked = prefs.getBoolean("update_alerts_enabled", true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnCheckedChangeListener { _, enabled -> prefs.edit().putBoolean("update_alerts_enabled", enabled).apply() }
        }
        root.addView(updateAlerts)
        root.addView(card("ذخیره اطلاعات", "پروژه‌های ذخیره‌شده با بروزرسانی برنامه حذف نمی‌شوند.", Color.WHITE))
        setScrollable(root)
    }

    private fun shareApp() {
        val shareText = "MDFچی؛ جعبه ابزار محاسبات کابینت‌ساز\nhttps://github.com/waxew/App-MDFchi"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MDFچی")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "معرفی MDFچی به دوستان"))
    }

    private fun showAboutUs() {
        val root = basePage("درباره ما", "👥")
        root.addView(space(28))
        root.addView(centerText("گروه توسعه و برنامه نویسی AS Team", 20, true, wood))
        root.addView(space(14))
        root.addView(centerText("تمامی حقوق مربوط به این برنامه انحصاری میباشد", 15, false, ink))
        setScrollable(root)
    }

    private fun showContactUs() {
        val root = basePage("تماس با ما", "✉️")
        root.addView(space(28))
        root.addView(centerText("گروه توسعه و برنامه نویسی AS Team", 20, true, wood))
        root.addView(space(18))
        root.addView(centerText("ایمیل پشتیبانی", 16, true, ink))
        root.addView(centerText("as.team.support@gmail.com", 16, false, wood))
        root.addView(space(16))
        root.addView(button("ارسال ایمیل به پشتیبانی") {
            val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:as.team.support@gmail.com"))
            runCatching { startActivity(mail) }
        })
        setScrollable(root)
    }

    private fun showAboutSoftware() {
        val root = basePage("درباره نرم‌افزار", "🧰")
        root.addView(card("MDFچی", "جعبه ابزار محاسبات و مدیریت پروژه برای کابینت‌سازها و MDFکارها", Color.rgb(255, 243, 221)))
        root.addView(centerText("نسخه ${BuildConfig.VERSION_NAME}", 16, true, ink))
        root.addView(space(12))
        root.addView(centerText("محاسبه یونیت، لیست برش، تعداد ورق، هزینه و ذخیره پروژه", 14, false, Color.DKGRAY))
        setScrollable(root)
    }

    private fun showUnitCalculator() {
        val root = basePage("محاسبه یونیت", "📐")
        root.addView(text("ابعاد بر حسب سانتی‌متر هستند. فرمول نسخه ۱ برای یونیت ساده دوطرفه است.", 13, false, ink))
        val name = field("نام پروژه", "یونیت جدید", false)
        val width = field("عرض یونیت", "80")
        val height = field("ارتفاع یونیت", "72")
        val depth = field("عمق یونیت", "55")
        val thickness = field("ضخامت MDF", "1.6")
        val shelves = field("تعداد طبقه", "1")
        val doors = field("تعداد درب", "2")
        listOf(name, width, height, depth, thickness, shelves, doors).forEach { root.addView(it) }
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(button("محاسبه لیست برش") {
            result.removeAllViews()
            val w = num(width); val h = num(height); val d = num(depth); val t = num(thickness)
            val shelfCount = shelves.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            val doorCount = doors.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (w <= 0 || h <= 0 || d <= 0 || t <= 0 || w <= 2 * t) {
                result.addView(card("خطا", "ابعاد واردشده معتبر نیستند.", Color.rgb(255, 228, 200)))
                return@button
            }
            val inner = w - (2 * t)
            val pieces = mutableListOf(
                Piece("بدنه چپ و راست", h, d, 2),
                Piece("کف و سقف", inner, d, 2)
            )
            if (shelfCount > 0) pieces += Piece("طبقه", inner, max(d - 1, 1.0), shelfCount)
            pieces += Piece("درب تقریبی", max(h - 0.4, 1.0), max((w - 0.4) / doorCount, 1.0), doorCount)
            val area = pieces.sumOf { it.a * it.b * it.qty } / 10000.0
            val adjusted = area * 1.10
            val sheetsNeeded = ceil(adjusted / (1.83 * 3.66)).toInt()
            result.addView(card("مصرف تقریبی", "مساحت قطعات: ${one(area)} m²\nبا ۱۰٪ پرت: ${one(adjusted)} m²\nحدود $sheetsNeeded ورق ۱۸۳×۳۶۶", Color.WHITE))
            pieces.forEach { p -> result.addView(card("${p.name} × ${p.qty}", "${one(p.a)} × ${one(p.b)} cm", Color.WHITE)) }
            result.addView(button("ذخیره پروژه 🧰") {
                saveProject(name.text.toString(), width.text.toString(), height.text.toString(), depth.text.toString(), pieces)
                toastLike(result, "پروژه روی دستگاه ذخیره شد.")
            })
            result.addView(text("ابعاد درب تقریبی است؛ خلاصی و نوع لولا ممکن است فرمول نهایی کارگاه را تغییر دهد.", 12, false, Color.DKGRAY))
        })
        root.addView(result)
        setScrollable(root)
    }

    private fun showSheetEstimator() {
        val root = basePage("تخمین تعداد ورق", "🪵")
        val area = field("مساحت موردنیاز (m²)", "12")
        val waste = field("درصد پرت", "10")
        val sw = field("عرض ورق (cm)", "183")
        val sh = field("طول ورق (cm)", "366")
        listOf(area, waste, sw, sh).forEach { root.addView(it) }
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(button("محاسبه تعداد ورق") {
            result.removeAllViews()
            val need = num(area); val wastePc = max(num(waste), 0.0); val w = num(sw); val h = num(sh)
            if (need <= 0 || w <= 0 || h <= 0) {
                result.addView(card("خطا", "مقادیر واردشده معتبر نیستند.", Color.rgb(255, 228, 200)))
            } else {
                val adjusted = need * (1 + wastePc / 100)
                val sheetArea = w * h / 10000.0
                result.addView(card("نتیجه", "مساحت با پرت: ${one(adjusted)} m²\nمساحت هر ورق: ${one(sheetArea)} m²\nتعداد تقریبی: ${ceil(adjusted / sheetArea).toInt()} ورق", Color.WHITE))
            }
        })
        root.addView(result)
        setScrollable(root)
    }

    private fun showCostCalculator() {
        val root = basePage("محاسبه هزینه", "🧮")
        val sheets = field("تعداد ورق", "4")
        val sheetPrice = field("قیمت هر ورق (تومان)", "2500000")
        val hardware = field("جمع یراق‌آلات (تومان)", "1500000")
        val labor = field("دستمزد (تومان)", "4000000")
        listOf(sheets, sheetPrice, hardware, labor).forEach { root.addView(it) }
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(button("محاسبه هزینه") {
            result.removeAllViews()
            val total = num(sheets) * num(sheetPrice) + num(hardware) + num(labor)
            result.addView(card("برآورد", "هزینه نهایی تقریبی: ${money(total)} تومان", Color.WHITE))
        })
        root.addView(result)
        setScrollable(root)
    }

    private fun showProjects() {
        val root = basePage("پروژه‌های من", "🧰")
        val list = loadProjects()
        if (list.isEmpty()) {
            root.addView(card("هنوز پروژه‌ای نیست", "از محاسبه یونیت اولین پروژه را ذخیره کن.", Color.WHITE))
        } else {
            list.forEach { p ->
                val count = p.optJSONArray("pieces")?.length() ?: 0
                root.addView(card(p.optString("name", "پروژه"), "${p.optString("width")} × ${p.optString("height")} × ${p.optString("depth")} cm\nردیف‌های لیست برش: $count", Color.WHITE))
            }
            root.addView(button("پاک کردن پروژه‌ها") { prefs.edit().remove("projects").apply(); showProjects() })
        }
        setScrollable(root)
    }

    private fun showUpdate() {
        val root = basePage("بروزرسانی برنامه", "🔧")
        root.addView(card("آپدیت‌خور از نسخه اول", "شناسه برنامه ثابت می‌ماند و نسخه‌های بعدی با versionCode بالاتر روی همین برنامه نصب می‌شوند. داده‌های محلی حفظ می‌شوند.", Color.WHITE))
        val status = text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}", 15, true, ink)
        root.addView(status)
        var latestUrl: String? = null
        root.addView(button("بررسی نسخه جدید") {
            status.text = "در حال بررسی نسخه جدید…"
            Thread {
                try {
                    val json = JSONObject(URL("https://raw.githubusercontent.com/waxew/App-MDFchi/main/version.json").readText())
                    val latestCode = json.optInt("versionCode", BuildConfig.VERSION_CODE)
                    val latestName = json.optString("versionName", BuildConfig.VERSION_NAME)
                    latestUrl = json.optString("downloadUrl").takeIf { it.isNotBlank() }
                    runOnUiThread {
                        status.text = if (latestCode > BuildConfig.VERSION_CODE) "نسخه جدید $latestName منتشر شده است." else "شما آخرین نسخه (${BuildConfig.VERSION_NAME}) را دارید."
                    }
                } catch (_: Exception) {
                    runOnUiThread { status.text = "بررسی نسخه انجام نشد. اتصال اینترنت را بررسی کنید." }
                }
            }.start()
        })
        root.addView(button("صفحه دریافت نسخه") {
            latestUrl?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                ?: run { status.text = "ابتدا بررسی نسخه را اجرا کنید." }
        })
        setScrollable(root)
    }

    private data class Piece(val name: String, val a: Double, val b: Double, val qty: Int)

    private fun saveProject(name: String, w: String, h: String, d: String, pieces: List<Piece>) {
        val all = JSONArray(prefs.getString("projects", "[]") ?: "[]")
        val parts = JSONArray()
        pieces.forEach { parts.put(JSONObject().put("name", it.name).put("a", it.a).put("b", it.b).put("qty", it.qty)) }
        all.put(JSONObject().put("name", name.ifBlank { "پروژه بدون نام" }).put("width", w).put("height", h).put("depth", d).put("pieces", parts).put("savedAt", System.currentTimeMillis()))
        prefs.edit().putString("projects", all.toString()).apply()
    }

    private fun loadProjects(): List<JSONObject> = try {
        val array = JSONArray(prefs.getString("projects", "[]") ?: "[]")
        buildList { for (i in array.length() - 1 downTo 0) add(array.getJSONObject(i)) }
    } catch (_: Exception) { emptyList() }

    private fun setScrollable(content: View) {
        val scroll = ScrollView(this).apply { setBackgroundColor(cream); addView(content) }
        setContentView(scroll)
    }

    private fun card(title: String, subtitle: String, bg: Int, action: (() -> Unit)? = null): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(bg, 20)
            isClickable = action != null
            isFocusable = action != null
            if (action != null) setOnClickListener { action() }
        }
        box.addView(text(title, 18, true, ink))
        box.addView(text(subtitle, 13, false, Color.rgb(90, 80, 72)))
        box.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(6), 0, dp(6)) }
        return box
    }

    private fun field(hint: String, value: String, numeric: Boolean = true): EditText = EditText(this).apply {
        this.hint = hint
        setText(value)
        textSize = 15f
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = rounded(Color.WHITE, 14, Color.rgb(210, 190, 170))
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else inputType = InputType.TYPE_CLASS_TEXT
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(5)) }
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = rounded(wood, 16)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(7), 0, dp(7)) }
    }

    private fun smallAction(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 28f
        setTextColor(wood)
        gravity = Gravity.CENTER
        background = rounded(Color.rgb(255, 242, 226), 15)
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(0f, 1.12f)
    }

    private fun centerText(value: String, size: Int, bold: Boolean, color: Int): TextView = text(value, size, bold, color).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        setPadding(dp(8), dp(6), dp(8), dp(6))
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun spaceWidth(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun num(v: EditText) = v.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
    private fun one(v: Double) = String.format(Locale.US, "%.1f", v)
    private fun money(v: Double) = NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(v.toLong())
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toastLike(parent: LinearLayout, message: String) { parent.addView(text(message, 14, true, wood)) }
}
