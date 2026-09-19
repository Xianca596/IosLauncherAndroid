package com.example.ioslauncheronandroid.data

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.ApplicationInfo

enum class WidgetCategory(val title: String, val emoji: String) {
    Productivity("Productivity", "✅"),
    Communication("Communication", "💬"),
    Media("Media & Photos", "🎵"),
    Utilities("Utilities", "🛠"),
    Social("Social", "👥"),
    Shopping("Shopping & Food", "🛍"),
    Finance("Finance", "💳"),
    Travel("Travel & Maps", "🧭"),
    Health("Health & Fitness", "❤️"),
    News("News & Reading", "📰"),
    Games("Games", "🎮"),
    Other("Other", "🧩"),
}

object WidgetCategorizer {
    private val Splitter = Regex("[^a-z0-9]+")

    private val keywords: Map<WidgetCategory, List<String>> = mapOf(
        WidgetCategory.Productivity to listOf("calendar", "task", "todo", "note", "keep", "docs", "sheets", "drive", "office", "reminder", "planner", "notion", "evernote", "focus", "habit", "diary", "journal"),
        WidgetCategory.Communication to listOf("message", "messag", "sms", "mms", "chat", "mail", "gmail", "outlook", "dialer", "phone", "contact", "meet", "duo", "teams", "slack", "telegram", "signal", "call"),
        WidgetCategory.Media to listOf("music", "player", "video", "youtube", "spotify", "podcast", "radio", "photo", "gallery", "camera", "netflix", "prime", "hotstar", "audio", "song", "wallpaper", "picture", "image"),
        WidgetCategory.Utilities to listOf("clock", "alarm", "weather", "battery", "calculator", "flashlight", "torch", "compass", "storage", "files", "wifi", "settings", "search", "assistant", "translate", "recorder", "cleaner", "security", "vpn", "network", "scanner", "app vault", "applock", "shortcut"),
        WidgetCategory.Social to listOf("instagram", "facebook", "twitter", "snapchat", "reddit", "linkedin", "tiktok", "pinterest", "whatsapp", "threads", "discord", "tinder", "social"),
        WidgetCategory.Shopping to listOf("amazon", "flipkart", "swiggy", "zomato", "shop", "cart", "food", "delivery", "grocery", "order", "myntra", "meesho", "blinkit", "restaurant", "cafe", "coffee"),
        WidgetCategory.Finance to listOf("bank", "pay", "wallet", "upi", "gpay", "phonepe", "paytm", "money", "stock", "invest", "crypto", "finance", "loan", "bill", "expense", "hdfc", "sbi", "icici", "coin"),
        WidgetCategory.Travel to listOf("maps", "map", "uber", "ola", "train", "irctc", "flight", "travel", "transit", "taxi", "navigation", "metro", "bus", "trip", "hotel", "booking", "tnstc", "location"),
        WidgetCategory.Health to listOf("fit", "health", "workout", "yoga", "sleep", "meditation", "step", "gym", "run", "diet", "calorie", "medic", "pill", "water", "detox"),
        WidgetCategory.News to listOf("news", "feed", "read", "book", "kindle", "medium", "magazine", "article", "blog", "story", "daily"),
        WidgetCategory.Games to listOf("game", "games", "puzzle", "chess", "ludo", "arcade", "candy"),
    )

    fun classify(text: String, appCategory: Int = -1): WidgetCategory {
        val t = text.lowercase()

        val tokens = t.split(Splitter).filter { it.isNotEmpty() }
        fun hits(k: String) = if (' ' in k) k in t else tokens.any { it == k || (k.length >= 4 && it.startsWith(k)) }
        val scores = keywords.mapValues { (_, words) -> words.count(::hits) }
        val best = scores.maxByOrNull { it.value }
        if (best != null && best.value > 0) return best.key
        return when (appCategory) {
            ApplicationInfo.CATEGORY_SOCIAL -> WidgetCategory.Social
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> WidgetCategory.Media
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> WidgetCategory.Productivity
            ApplicationInfo.CATEGORY_NEWS -> WidgetCategory.News
            ApplicationInfo.CATEGORY_MAPS -> WidgetCategory.Travel
            ApplicationInfo.CATEGORY_GAME -> WidgetCategory.Games
            else -> WidgetCategory.Other
        }
    }

    fun category(context: Context, info: AppWidgetProviderInfo?): WidgetCategory {
        if (info == null) return WidgetCategory.Other
        val pm = context.packageManager
        val pkg = info.provider.packageName
        val appCategory = runCatching { pm.getApplicationInfo(pkg, 0).let { if (android.os.Build.VERSION.SDK_INT >= 26) it.category else -1 } }.getOrDefault(-1)
        val appLabel = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("")
        val widgetLabel = runCatching { info.loadLabel(pm) }.getOrDefault("")
        return classify("$widgetLabel $appLabel $pkg", appCategory)
    }
}
