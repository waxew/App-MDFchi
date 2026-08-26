package com.mdfchi.app

// Activity پایه برنامه؛ تمام صفحات نسخه فعلی داخل همین Activity سبک رندر می‌شوند.
import android.app.Activity
// Context برای SharedPreferences و نگهداری داده‌های محلی استفاده می‌شود.
import android.content.Context
// Intent برای Share، ایمیل و باز کردن لینک بروزرسانی استفاده می‌شود.
import android.content.Intent
// کلاس‌های گرافیکی استاندارد Android برای ساخت UI بدون وابستگی خارجی.
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
// Uri برای لینک‌های mailto و https استفاده می‌شود.
import android.net.Uri
// Bundle ورودی چرخه عمر Activity است.
import android.os.Bundle
// InputType نوع کیبورد فیلدهای عددی/متنی را تعیین می‌کند.
import android.text.InputType
// ابزارهای پایه View و Gravity برای چیدمان برنامه.
import android.view.Gravity
import android.view.View
// Widgetهای استاندارد Android؛ استفاده از این‌ها APK را سبک و سازگار نگه می‌دارد.
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
// JSON برای ذخیره پروژه‌ها و خواندن version.json.
import org.json.JSONArray
import org.json.JSONObject
// URL برای بررسی نسخه جدید از GitHub.
import java.net.URL
// NumberFormat برای نمایش خوانای مبلغ.
import java.text.NumberFormat
// ArrayDeque به‌عنوان Back Stack داخلی برنامه.
import java.util.ArrayDeque
import java.util.Locale
// توابع ریاضی موردنیاز محاسبات ورق و یونیت.
import kotlin.math.ceil
import kotlin.math.max

/**
 * MainActivity هسته نسخه 1.0.2 برنامه MDFچی است.
 *
 * نکات معماری:
 * - برنامه عمداً وابستگی UI شخص ثالث ندارد تا Build ساده و پایدار بماند.
 * - پروژه‌ها در SharedPreferences ذخیره می‌شوند و هنگام Update حذف نمی‌شوند.
 * - تمام ناوبری از Screen و backStack عبور می‌کند تا دکمه Back ابتدا صفحه قبلی را باز کند.
 * - منوی همبرگری در بالای سمت راست همه صفحات در دسترس است.
 */
class MainActivity : Activity() {

    // فایل تنظیمات محلی مشترک برای پروژه‌ها و گزینه‌های Settings.
    private val prefs by lazy {
        getSharedPreferences("mdfchi_projects", Context.MODE_PRIVATE)
    }

    // رنگ چوبی اصلی هویت بصری برنامه.
    private val wood = Color.rgb(139, 94, 60)
    // رنگ کرم پس‌زمینه.
    private val cream = Color.rgb(255, 248, 239)
    // رنگ اصلی نوشته‌ها.
    private val ink = Color.rgb(48, 42, 38)

    /** فهرست صفحات داخلی؛ اضافه شدن صفحه جدید باید ابتدا اینجا ثبت شود. */
    private enum class Screen {
        HOME,
        SETTINGS,
        ABOUT_US,
        CONTACT_US,
        ABOUT_SOFTWARE,
        UNIT_CALCULATOR,
        SHEET_ESTIMATOR,
        COST_CALCULATOR,
        PROJECTS,
        UPDATE
    }

    // صفحه‌ای که اکنون روی نمایشگر است.
    private var currentScreen = Screen.HOME

    // تاریخچه صفحات برای Back واقعی داخل برنامه.
    private val backStack = ArrayDeque<Screen>()

    /** نقطه شروع برنامه. */
    override fun onCreate(savedInstanceState: Bundle?) {
        // اجرای چرخه عمر پایه Android.
        super.onCreate(savedInstanceState)
        // هماهنگی نوارهای سیستم با تم روشن برنامه.
        window.statusBarColor = cream
        window.navigationBarColor = cream
        // شروع همیشه از Home و با تاریخچه خالی.
        backStack.clear()
        renderScreen(Screen.HOME)
    }

    /**
     * Back در این پروژه نباید از صفحه فرعی مستقیماً برنامه را ببندد.
     * Manifest رفتار Predictive Back را برای این Activity غیرفعال کرده تا این مسیر روی Androidهای جدید هم ثابت بماند.
     */
    @Deprecated("Kept for the app's custom navigation on supported Android versions")
    override fun onBackPressed() {
        handleBackNavigation()
    }

    /** منطق واحد Back برای دکمه سیستم و دکمه بالای صفحات. */
    private fun handleBackNavigation() {
        // اگر صفحه قبلی ثبت شده، همان صفحه بدون Push مجدد رندر می‌شود.
        if (backStack.isNotEmpty()) {
            renderScreen(backStack.removeLast())
            return
        }
        // اگر به هر دلیل داخل صفحه فرعی هستیم ولی Stack خالی است، Home را نشان می‌دهیم.
        if (currentScreen != Screen.HOME) {
            renderScreen(Screen.HOME)
            return
        }
        // فقط از Home اجازه خروج از Activity داده می‌شود.
        finish()
    }

    /** ورود به صفحه جدید و ثبت صفحه فعلی در History. */
    private fun navigateTo(screen: Screen) {
        // کلیک دوباره روی همان صفحه History اضافی تولید نمی‌کند.
        if (screen == currentScreen) return
        // صفحه فعلی برای Back ذخیره می‌شود.
        backStack.addLast(currentScreen)
        // صفحه مقصد رندر می‌شود.
        renderScreen(screen)
    }

    /** Router مرکزی صفحات؛ currentScreen همیشه از این مسیر تغییر می‌کند. */
    private fun renderScreen(screen: Screen) {
        // ثبت صفحه جاری برای Back و UI نوار بالا.
        currentScreen = screen
        // انتخاب Renderer مناسب.
        when (screen) {
            Screen.HOME -> renderHome()
            Screen.SETTINGS -> renderSettings()
            Screen.ABOUT_US -> renderAboutUs()
            Screen.CONTACT_US -> renderContactUs()
            Screen.ABOUT_SOFTWARE -> renderAboutSoftware()
            Screen.UNIT_CALCULATOR -> renderUnitCalculator()
            Screen.SHEET_ESTIMATOR -> renderSheetEstimator()
            Screen.COST_CALCULATOR -> renderCostCalculator()
            Screen.PROJECTS -> renderProjects()
            Screen.UPDATE -> renderUpdate()
        }
    }

    /** اسکلت مشترک صفحات: Back سمت چپ، عنوان وسط و Hamburger سمت راست. */
    private fun basePage(title: String, emoji: String): LinearLayout {
        // ریشه عمودی صفحه.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(28))
            setBackgroundColor(cream)
        }

        // Top bar را LTR می‌کنیم تا جای فیزیکی Back/Hamburger مستقل از زبان دستگاه ثابت بماند.
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        // Home نیاز به Back داخلی ندارد؛ فضای خالی تعادل چیدمان را حفظ می‌کند.
        if (currentScreen == Screen.HOME) {
            top.addView(spaceWidth(48))
        } else {
            // دکمه Back از همان History داخلی استفاده می‌کند.
            top.addView(
                smallAction("‹") { handleBackNavigation() },
                LinearLayout.LayoutParams(dp(48), dp(48))
            )
        }

        // عنوان صفحه فضای باقی‌مانده را می‌گیرد.
        top.addView(
            text("$emoji  $title", 26, true, wood).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            },
            LinearLayout.LayoutParams(0, dp(54), 1f)
        )

        // آیکون سه‌خط همیشه در بالا سمت راست قرار می‌گیرد.
        top.addView(
            smallAction("☰") { openHamburger() },
            LinearLayout.LayoutParams(dp(54), dp(48))
        )

        // افزودن Top bar به ریشه.
        root.addView(top)
        return root
    }

    /** منوی همبرگری از سمت راست صفحه باز می‌شود. */
    private fun openHamburger() {
        // بدنه سفید Drawer.
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            setBackgroundColor(Color.WHITE)
        }

        // عنوان و زیرعنوان منو.
        drawer.addView(text("🧰  MDFچی", 24, true, wood).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        drawer.addView(text("جعبه ابزار کابینت‌ساز", 13, false, Color.DKGRAY).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        drawer.addView(space(18))

        // Popup باید قبل از Itemها ساخته شود تا Item بتواند آن را dismiss کند.
        lateinit var popup: PopupWindow

        // Helper داخلی برای ساخت گزینه‌های منو.
        fun item(icon: String, label: String, action: () -> Unit) {
            drawer.addView(drawerItem(icon, label) {
                popup.dismiss()
                action()
            })
        }

        // عرض Drawer حدود 84 درصد صفحه است.
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

        // 0) تنظیمات؛ شامل اعلان‌ها.
        item("⚙️", "تنظیمات") { navigateTo(Screen.SETTINGS) }
        // 1) معرفی به دوستان؛ Share Sheet سیستم را باز می‌کند.
        item("📤", "معرفی به دوستان") { shareApp() }
        // 2) درباره ما.
        item("👥", "درباره ما") { navigateTo(Screen.ABOUT_US) }
        // 3) تماس با ما.
        item("✉️", "تماس با ما") { navigateTo(Screen.CONTACT_US) }
        // 4) درباره نرم‌افزار.
        item("🧰", "درباره نرم‌افزار") { navigateTo(Screen.ABOUT_SOFTWARE) }

        // نمایش نسخه فعلی در پایین Drawer.
        drawer.addView(space(12))
        drawer.addView(text("نسخه ${BuildConfig.VERSION_NAME}", 12, false, Color.GRAY).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // RIGHT عمداً به‌جای END استفاده شده تا منو همیشه از سمت راست باز شود.
        popup.showAtLocation(window.decorView, Gravity.RIGHT or Gravity.TOP, 0, 0)
    }

    /** یک ردیف قابل لمس داخل منوی همبرگری. */
    private fun drawerItem(icon: String, label: String, action: () -> Unit): View {
        // ردیف RTL تا آیکون در سمت راست عنوان قرار بگیرد.
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(255, 249, 242), 16)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(58)).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
        }
        // آیکون گزینه.
        row.addView(text(icon, 22, false, ink).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(42), -1))
        // عنوان گزینه.
        row.addView(text(label, 16, true, ink).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        }, LinearLayout.LayoutParams(0, -1, 1f))
        return row
    }

    /** صفحه اصلی و ورودی ابزارهای اصلی. */
    private fun renderHome() {
        val root = basePage("MDFچی", "🧰")
        // زیرعنوان برنامه.
        root.addView(text("جعبه ابزار کابینت‌ساز", 16, false, ink))
        root.addView(space(14))
        // کارت تزئینی ابزارهای کارگاه.
        root.addView(card("📏  🔩  🪛  🔨  🪚  🪵", "ابزارهای کارگاه همیشه دم دستت", Color.rgb(255, 243, 221)))
        root.addView(space(12))
        // ابزار محاسبه یونیت.
        root.addView(card("📐 محاسبه یونیت", "ابعاد قطعات و لیست برش", Color.rgb(255, 228, 200)) {
            navigateTo(Screen.UNIT_CALCULATOR)
        })
        // ابزار تخمین تعداد ورق.
        root.addView(card("🪵 تعداد ورق", "تخمین MDF با درصد پرت", Color.rgb(231, 242, 223)) {
            navigateTo(Screen.SHEET_ESTIMATOR)
        })
        // ابزار محاسبه هزینه.
        root.addView(card("🧮 محاسبه هزینه", "ورق، یراق و دستمزد", Color.rgb(221, 236, 247)) {
            navigateTo(Screen.COST_CALCULATOR)
        })
        // فهرست پروژه‌های ذخیره‌شده.
        root.addView(card("🧰 پروژه‌های من", "مشاهده پروژه‌های ذخیره‌شده", Color.rgb(246, 221, 227)) {
            navigateTo(Screen.PROJECTS)
        })
        // بررسی نسخه جدید.
        root.addView(card("🔧 بروزرسانی", "بررسی نسخه جدید برنامه", Color.rgb(255, 232, 168)) {
            navigateTo(Screen.UPDATE)
        })
        setScrollable(root)
    }

    /** صفحه تنظیمات برنامه. */
    private fun renderSettings() {
        val root = basePage("تنظیمات", "⚙️")
        root.addView(card("تنظیمات برنامه", "تنظیمات MDFچی روی همین دستگاه ذخیره می‌شود.", Color.rgb(255, 243, 221)))

        // کلید اصلی اعلان‌ها؛ مقدار آن در SharedPreferences نگه داشته می‌شود.
        val notifications = Switch(this).apply {
            text = "اعلان‌ها"
            textSize = 17f
            setTextColor(ink)
            isChecked = prefs.getBoolean("notifications_enabled", true)
            setPadding(dp(12), dp(16), dp(12), dp(12))
            setOnCheckedChangeListener { _, enabled ->
                prefs.edit().putBoolean("notifications_enabled", enabled).apply()
            }
        }
        root.addView(notifications)
        root.addView(text("برای اطلاع‌رسانی‌های داخلی برنامه و نسخه‌های جدید.", 13, false, Color.DKGRAY))

        // کنترل جداگانه اعلان نسخه جدید برای توسعه آینده.
        val updateAlerts = Switch(this).apply {
            text = "اعلان انتشار نسخه جدید"
            textSize = 16f
            setTextColor(ink)
            isChecked = prefs.getBoolean("update_alerts_enabled", true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnCheckedChangeListener { _, enabled ->
                prefs.edit().putBoolean("update_alerts_enabled", enabled).apply()
            }
        }
        root.addView(updateAlerts)
        // توضیح حفظ داده‌ها هنگام نصب Update.
        root.addView(card("ذخیره اطلاعات", "پروژه‌های ذخیره‌شده با بروزرسانی برنامه حذف نمی‌شوند.", Color.WHITE))
        setScrollable(root)
    }

    /** معرفی برنامه از طریق برنامه‌های نصب‌شده روی گوشی. */
    private fun shareApp() {
        // متن اشتراک‌گذاری.
        val shareText = "MDFچی؛ جعبه ابزار محاسبات کابینت‌ساز\nhttps://github.com/waxew/App-MDFchi"
        // Intent استاندارد متن.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MDFچی")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        // کاربر برنامه مقصد را انتخاب می‌کند.
        startActivity(Intent.createChooser(intent, "معرفی MDFچی به دوستان"))
    }

    /** صفحه درباره ما. */
    private fun renderAboutUs() {
        val root = basePage("درباره ما", "👥")
        root.addView(space(28))
        root.addView(centerText("گروه توسعه و برنامه نویسی AS Team", 20, true, wood))
        root.addView(space(14))
        root.addView(centerText("تمامی حقوق مربوط به این برنامه انحصاری میباشد", 15, false, ink))
        setScrollable(root)
    }

    /** صفحه تماس با پشتیبانی. */
    private fun renderContactUs() {
        val root = basePage("تماس با ما", "✉️")
        root.addView(space(28))
        root.addView(centerText("گروه توسعه و برنامه نویسی AS Team", 20, true, wood))
        root.addView(space(18))
        root.addView(centerText("ایمیل پشتیبانی", 16, true, ink))
        root.addView(centerText("as.team.support@gmail.com", 16, false, wood))
        root.addView(space(16))
        // ACTION_SENDTO فقط برنامه‌های سازگار با ایمیل را پیشنهاد می‌دهد.
        root.addView(button("ارسال ایمیل به پشتیبانی") {
            val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:as.team.support@gmail.com"))
            runCatching { startActivity(mail) }
        })
        setScrollable(root)
    }

    /**
     * درباره نرم‌افزار عمداً ساده است.
     * هیچ Package Name، Application ID یا اطلاعات فنی اضافه به کاربر نشان داده نمی‌شود.
     */
    private fun renderAboutSoftware() {
        val root = basePage("درباره نرم‌افزار", "🧰")
        root.addView(space(18))
        // توضیح کوتاه کاربرد برنامه.
        root.addView(centerText("MDFچی یک جعبه ابزار ساده و کاربردی برای کابینت‌سازها و MDFکارهاست.", 17, true, wood))
        root.addView(space(12))
        root.addView(centerText("با برنامه می‌توانید محاسبات یونیت، لیست برش، تعداد تقریبی ورق و هزینه پروژه را سریع‌تر انجام دهید و پروژه‌های خود را روی دستگاه نگه دارید.", 14, false, ink))
        root.addView(space(18))
        // تنها اطلاعات فنی نمایش‌داده‌شده، نسخه برنامه است.
        root.addView(centerText("نسخه ${BuildConfig.VERSION_NAME}", 16, true, ink))
        setScrollable(root)
    }

    /** محاسبه قطعات یک یونیت ساده. */
    private fun renderUnitCalculator() {
        val root = basePage("محاسبه یونیت", "📐")
        root.addView(text("ابعاد بر حسب سانتی‌متر هستند. فرمول این نسخه برای یونیت ساده دوطرفه است.", 13, false, ink))

        // فیلدهای ورودی پروژه.
        val name = field("نام پروژه", "یونیت جدید", false)
        val width = field("عرض یونیت", "80")
        val height = field("ارتفاع یونیت", "72")
        val depth = field("عمق یونیت", "55")
        val thickness = field("ضخامت MDF", "1.6")
        val shelves = field("تعداد طبقه", "1")
        val doors = field("تعداد درب", "2")
        listOf(name, width, height, depth, thickness, shelves, doors).forEach { root.addView(it) }

        // Container نتیجه برای پاک/بازسازی بعد از هر محاسبه.
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // اجرای محاسبات لیست برش.
        root.addView(button("محاسبه لیست برش") {
            result.removeAllViews()
            // تبدیل ورودی‌ها به عدد.
            val w = num(width)
            val h = num(height)
            val d = num(depth)
            val t = num(thickness)
            val shelfCount = shelves.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            // حداقل یک درب؛ جلوگیری از تقسیم بر صفر.
            val doorCount = doors.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1

            // اعتبارسنجی ابعاد نامعتبر.
            if (w <= 0 || h <= 0 || d <= 0 || t <= 0 || w <= 2 * t) {
                result.addView(card("خطا", "ابعاد واردشده معتبر نیستند.", Color.rgb(255, 228, 200)))
                return@button
            }

            // عرض داخلی با کسر ضخامت دو بدنه.
            val inner = w - (2 * t)
            // قطعات پایه.
            val pieces = mutableListOf(
                Piece("بدنه چپ و راست", h, d, 2),
                Piece("کف و سقف", inner, d, 2)
            )
            // طبقات اختیاری.
            if (shelfCount > 0) {
                pieces += Piece("طبقه", inner, max(d - 1, 1.0), shelfCount)
            }
            // درب با خلاصی تقریبی 4 میلی‌متر.
            pieces += Piece("درب تقریبی", max(h - 0.4, 1.0), max((w - 0.4) / doorCount, 1.0), doorCount)

            // تبدیل مجموع cm² قطعات به m².
            val area = pieces.sumOf { it.a * it.b * it.qty } / 10000.0
            // 10 درصد پرت اولیه.
            val adjusted = area * 1.10
            // تعداد تقریبی ورق استاندارد 183×366.
            val sheetsNeeded = ceil(adjusted / (1.83 * 3.66)).toInt()

            // خلاصه مصرف.
            result.addView(card("مصرف تقریبی", "مساحت قطعات: ${one(area)} m²\nبا ۱۰٪ پرت: ${one(adjusted)} m²\nحدود $sheetsNeeded ورق ۱۸۳×۳۶۶", Color.WHITE))
            // ردیف‌های لیست برش.
            pieces.forEach { p ->
                result.addView(card("${p.name} × ${p.qty}", "${one(p.a)} × ${one(p.b)} cm", Color.WHITE))
            }
            // ذخیره پروژه بدون حذف پروژه‌های قبلی.
            result.addView(button("ذخیره پروژه 🧰") {
                saveProject(name.text.toString(), width.text.toString(), height.text.toString(), depth.text.toString(), pieces)
                toastLike(result, "پروژه روی دستگاه ذخیره شد.")
            })
            // هشدار درباره تقریبی بودن درب.
            result.addView(text("ابعاد درب تقریبی است؛ خلاصی، نوع لولا و روش ساخت کارگاه می‌تواند فرمول نهایی را تغییر دهد.", 12, false, Color.DKGRAY))
        })

        root.addView(result)
        setScrollable(root)
    }

    /** تخمین تعداد ورق از روی مساحت. */
    private fun renderSheetEstimator() {
        val root = basePage("تخمین تعداد ورق", "🪵")
        // ورودی‌های قابل تغییر کاربر.
        val area = field("مساحت موردنیاز (m²)", "12")
        val waste = field("درصد پرت", "10")
        val sw = field("عرض ورق (cm)", "183")
        val sh = field("طول ورق (cm)", "366")
        listOf(area, waste, sw, sh).forEach { root.addView(it) }
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        root.addView(button("محاسبه تعداد ورق") {
            result.removeAllViews()
            val need = num(area)
            val wastePc = max(num(waste), 0.0)
            val w = num(sw)
            val h = num(sh)
            // ورودی صفر/منفی اجازه ادامه ندارد.
            if (need <= 0 || w <= 0 || h <= 0) {
                result.addView(card("خطا", "مقادیر واردشده معتبر نیستند.", Color.rgb(255, 228, 200)))
            } else {
                // مساحت با پرت.
                val adjusted = need * (1 + wastePc / 100)
                // مساحت هر ورق از cm² به m².
                val sheetArea = w * h / 10000.0
                // ceil چون ورق کسری قابل خرید نیست.
                result.addView(card("نتیجه", "مساحت با پرت: ${one(adjusted)} m²\nمساحت هر ورق: ${one(sheetArea)} m²\nتعداد تقریبی: ${ceil(adjusted / sheetArea).toInt()} ورق", Color.WHITE))
            }
        })

        root.addView(result)
        setScrollable(root)
    }

    /** برآورد هزینه ورق، یراق و دستمزد. */
    private fun renderCostCalculator() {
        val root = basePage("محاسبه هزینه", "🧮")
        // ورودی‌های محاسبه قیمت.
        val sheets = field("تعداد ورق", "4")
        val sheetPrice = field("قیمت هر ورق (تومان)", "2500000")
        val hardware = field("جمع یراق‌آلات (تومان)", "1500000")
        val labor = field("دستمزد (تومان)", "4000000")
        listOf(sheets, sheetPrice, hardware, labor).forEach { root.addView(it) }
        val result = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        root.addView(button("محاسبه هزینه") {
            result.removeAllViews()
            // فرمول جمع نسخه فعلی.
            val total = num(sheets) * num(sheetPrice) + num(hardware) + num(labor)
            result.addView(card("برآورد", "هزینه نهایی تقریبی: ${money(total)} تومان", Color.WHITE))
        })

        root.addView(result)
        setScrollable(root)
    }

    /** نمایش پروژه‌های ذخیره‌شده به ترتیب جدیدترین. */
    private fun renderProjects() {
        val root = basePage("پروژه‌های من", "🧰")
        // دریافت پروژه‌ها از حافظه محلی.
        val list = loadProjects()
        if (list.isEmpty()) {
            // Empty state.
            root.addView(card("هنوز پروژه‌ای نیست", "از محاسبه یونیت اولین پروژه را ذخیره کن.", Color.WHITE))
        } else {
            // نمایش کارت هر پروژه.
            list.forEach { p ->
                val count = p.optJSONArray("pieces")?.length() ?: 0
                root.addView(card(p.optString("name", "پروژه"), "${p.optString("width")} × ${p.optString("height")} × ${p.optString("depth")} cm\nردیف‌های لیست برش: $count", Color.WHITE))
            }
            // پاک کردن کل لیست با حفظ سایر Settingها.
            root.addView(button("پاک کردن پروژه‌ها") {
                prefs.edit().remove("projects").apply()
                renderScreen(Screen.PROJECTS)
            })
        }
        setScrollable(root)
    }

    /** بررسی نسخه جدید از version.json ریپو. */
    private fun renderUpdate() {
        val root = basePage("بروزرسانی برنامه", "🔧")
        root.addView(card("بروزرسانی برنامه", "نسخه‌های جدید روی نسخه فعلی نصب می‌شوند و پروژه‌های ذخیره‌شده باقی می‌مانند.", Color.WHITE))
        // TextView وضعیت بررسی.
        val status = text("نسخه نصب‌شده: ${BuildConfig.VERSION_NAME}", 15, true, ink)
        root.addView(status)
        // URL دانلود پس از بررسی موفق پر می‌شود.
        var latestUrl: String? = null

        root.addView(button("بررسی نسخه جدید") {
            status.text = "در حال بررسی نسخه جدید…"
            // شبکه خارج از Main Thread اجرا می‌شود.
            Thread {
                try {
                    // خواندن JSON نسخه از شاخه main.
                    val json = JSONObject(URL("https://raw.githubusercontent.com/waxew/App-MDFchi/main/version.json").readText())
                    // versionCode معیار واقعی Update است.
                    val latestCode = json.optInt("versionCode", BuildConfig.VERSION_CODE)
                    // versionName فقط برای نمایش به کاربر است.
                    val latestName = json.optString("versionName", BuildConfig.VERSION_NAME)
                    // URL فایل نهایی یا صفحه دریافت.
                    latestUrl = json.optString("downloadUrl").takeIf { it.isNotBlank() }
                    // تغییر UI روی Main Thread.
                    runOnUiThread {
                        status.text = if (latestCode > BuildConfig.VERSION_CODE) {
                            "نسخه جدید $latestName منتشر شده است."
                        } else {
                            "شما آخرین نسخه (${BuildConfig.VERSION_NAME}) را دارید."
                        }
                    }
                } catch (_: Exception) {
                    // خطاهای شبکه/JSON باعث Crash نمی‌شوند.
                    runOnUiThread {
                        status.text = "بررسی نسخه انجام نشد. اتصال اینترنت را بررسی کنید."
                    }
                }
            }.start()
        })

        root.addView(button("صفحه دریافت نسخه") {
            // اگر URL نداریم کاربر باید ابتدا Check را بزند.
            val url = latestUrl
            if (url == null) {
                status.text = "ابتدا بررسی نسخه را اجرا کنید."
            } else {
                // باز کردن لینک در مرورگر/مدیریت دانلود دستگاه.
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    status.text = "امکان باز کردن لینک دریافت وجود ندارد."
                }
            }
        })
        setScrollable(root)
    }

    /** مدل داده یک ردیف لیست برش. */
    private data class Piece(val name: String, val a: Double, val b: Double, val qty: Int)

    /** ذخیره پروژه به‌شکل JSON داخل SharedPreferences. */
    private fun saveProject(name: String, w: String, h: String, d: String, pieces: List<Piece>) {
        // آرایه قبلی پروژه‌ها یا آرایه خالی.
        val all = JSONArray(prefs.getString("projects", "[]") ?: "[]")
        // تبدیل Pieceها به JSON.
        val parts = JSONArray()
        pieces.forEach {
            parts.put(JSONObject().put("name", it.name).put("a", it.a).put("b", it.b).put("qty", it.qty))
        }
        // افزودن پروژه جدید همراه زمان ذخیره.
        all.put(
            JSONObject()
                .put("name", name.ifBlank { "پروژه بدون نام" })
                .put("width", w)
                .put("height", h)
                .put("depth", d)
                .put("pieces", parts)
                .put("savedAt", System.currentTimeMillis())
        )
        // apply غیرمسدودکننده است و UI را متوقف نمی‌کند.
        prefs.edit().putString("projects", all.toString()).apply()
    }

    /** خواندن امن پروژه‌ها؛ داده خراب باعث Crash نمی‌شود. */
    private fun loadProjects(): List<JSONObject> = try {
        val array = JSONArray(prefs.getString("projects", "[]") ?: "[]")
        // ترتیب معکوس تا جدیدترین پروژه بالا باشد.
        buildList {
            for (i in array.length() - 1 downTo 0) add(array.getJSONObject(i))
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** هر صفحه داخل ScrollView قرار می‌گیرد تا روی نمایشگر کوچک قابل استفاده باشد. */
    private fun setScrollable(content: View) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(cream)
            addView(content)
        }
        setContentView(scroll)
    }

    /** کارت استاندارد فانتزی برنامه. */
    private fun card(title: String, subtitle: String, bg: Int, action: (() -> Unit)? = null): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(bg, 20)
            isClickable = action != null
            isFocusable = action != null
            if (action != null) setOnClickListener { action() }
        }
        // عنوان کارت.
        box.addView(text(title, 18, true, ink))
        // توضیح کارت.
        box.addView(text(subtitle, 13, false, Color.rgb(90, 80, 72)))
        // Margin بیرونی.
        box.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(6), 0, dp(6))
        }
        return box
    }

    /** فیلد ورودی استاندارد با Keyboard عددی یا متنی. */
    private fun field(hint: String, value: String, numeric: Boolean = true): EditText = EditText(this).apply {
        this.hint = hint
        setText(value)
        textSize = 15f
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = rounded(Color.WHITE, 14, Color.rgb(210, 190, 170))
        inputType = if (numeric) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_TEXT
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(5), 0, dp(5))
        }
    }

    /** دکمه اصلی چوبی. */
    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = rounded(wood, 16)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply {
            setMargins(0, dp(7), 0, dp(7))
        }
    }

    /** دکمه کوچک Top bar برای Back و Hamburger. */
    private fun smallAction(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 28f
        setTextColor(wood)
        gravity = Gravity.CENTER
        background = rounded(Color.rgb(255, 242, 226), 15)
        setOnClickListener { action() }
    }

    /** TextView راست‌چین استاندارد. */
    private fun text(value: String, size: Int, bold: Boolean, color: Int): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        gravity = Gravity.RIGHT
        textDirection = View.TEXT_DIRECTION_RTL
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(0f, 1.12f)
    }

    /** TextView وسط‌چین برای صفحات معرفی. */
    private fun centerText(value: String, size: Int, bold: Boolean, color: Int): TextView =
        text(value, size, bold, color).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

    /** GradientDrawable گوشه‌گرد با Stroke اختیاری. */
    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    // Spacer عمودی.
    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    // Spacer افقی.
    private fun spaceWidth(width: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(width), 1)
    }

    // تبدیل امن EditText به Double؛ ورودی نامعتبر صفر می‌شود.
    private fun num(v: EditText) = v.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
    // فرمت یک رقم اعشار.
    private fun one(v: Double) = String.format(Locale.US, "%.1f", v)
    // فرمت مبلغ با جداکننده هزارگان.
    private fun money(v: Double) = NumberFormat.getIntegerInstance(Locale("fa", "IR")).format(v.toLong())
    // تبدیل dp به px بر اساس Density دستگاه.
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    // پیام سبک داخل نتیجه بدون نیاز به Toast.
    private fun toastLike(parent: LinearLayout, message: String) {
        parent.addView(text(message, 14, true, wood))
    }
}
