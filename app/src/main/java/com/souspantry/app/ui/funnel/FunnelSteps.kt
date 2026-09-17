package com.souspantry.app.ui.funnel

/**
 * Data for the onboarding funnel — exact mirror of the shipped iOS funnel
 * (Features/Onboarding/Funnel/). Every step id, option id, label, and emoji is
 * copied from the source-of-truth prompt; the only platform adaptation is
 * `app_store` → `play_store` "Google Play".
 */

enum class StepKind { SINGLE, MULTI, TEXT, SLIDER, STEPPER, INFO, OTHER_COUNTRY, FINALE }

data class FunnelOption(
    val id       : String,
    val label    : String,
    val emoji    : String = "",
    val sublabel : String = "",
)

data class InfoContent(
    val emoji      : String,
    val title      : String,
    val body       : String,
    val stat       : String = "",
    val caption    : String = "",
    val sourceName : String = "",
    val sourceUrl  : String = "",
)

data class FunnelStep(
    val id       : String,
    val kind     : StepKind,
    val title    : String = "",
    val subtitle : String = "",
    val options  : List<FunnelOption> = emptyList(),
    val min      : Int = 0,
    val max      : Int = 0,
    val info     : InfoContent? = null,
)

// ── Region data ──────────────────────────────────────────────────────────────

val REGION_CURRENCY = mapOf(
    "GB" to "GBP", "EU" to "EUR", "US" to "USD", "CA" to "CAD", "AU" to "AUD",
    "NZ" to "NZD", "MY" to "MYR", "SG" to "SGD", "TH" to "THB",
)

val CURRENCY_SYMBOL = mapOf(
    "GBP" to "£", "EUR" to "€", "USD" to "$", "CAD" to "$", "AUD" to "$",
    "NZD" to "$", "MYR" to "RM", "SGD" to "$", "THB" to "฿",
)

private fun slug(name: String) =
    name.lowercase().replace("'", "").replace(".", "").replace(" ", "_")

private fun shops(vararg names: String) = names.map { FunnelOption(slug(it), it) }

val REGION_SHOPS: Map<String, List<FunnelOption>> = mapOf(
    "GB" to shops("Tesco", "Sainsbury's", "Asda", "Morrisons", "Aldi", "Lidl"),
    "EU" to shops("Lidl", "Aldi", "Carrefour", "REWE", "Auchan"),
    "US" to shops("Walmart", "Kroger", "Costco", "Trader Joe's", "Whole Foods", "Safeway"),
    "CA" to shops("Loblaws", "No Frills", "Metro", "Sobeys", "Costco", "Walmart"),
    "NZ" to shops("Woolworths", "New World", "Pak'nSave", "Four Square"),
    "MY" to shops("Lotus's", "Giant", "AEON", "Mydin", "Jaya Grocer", "Village Grocer"),
    "SG" to shops("FairPrice", "Cold Storage", "Sheng Siong", "Giant"),
    "TH" to shops("Lotus's", "Big C", "Tops", "Makro", "Villa Market"),
    "AU" to shops("ALDI", "Coles", "Woolworths", "IGA"),
)

private const val FACT_BODY =
    "Sous Pantry helps you use what you already have — so less food, and less money, ends up in the bin."

val FOOD_WASTE_FACTS: Map<String, InfoContent> = mapOf(
    "GB" to InfoContent("🤯", "A UK family of four wastes", FACT_BODY, "£1,000 a year", "on food that ends up binned", "WRAP · Love Food Hate Waste", "https://www.lovefoodhatewaste.com"),
    "US" to InfoContent("🤯", "A US family of four loses", FACT_BODY, "$1,500 a year", "to uneaten food", "USDA", "https://www.usda.gov/foodlossandwaste/consumers"),
    "CA" to InfoContent("🤯", "The average Canadian household wastes", FACT_BODY, "$1,300 a year", "on food thrown out", "Love Food Hate Waste Canada", "https://lovefoodhatewaste.ca/about/food-waste/"),
    "NZ" to InfoContent("🤯", "The average Kiwi household wastes", FACT_BODY, "$1,510 a year", "on uneaten food", "Love Food Hate Waste NZ", "https://lovefoodhatewaste.co.nz/food-waste/"),
    "EU" to InfoContent("🤯", "EU households waste", FACT_BODY, "69 kg", "of food per person, every year", "Eurostat", "https://ec.europa.eu/eurostat/web/products-eurostat-news/w/ddn-20251016-2"),
    "MY" to InfoContent("🤯", "Malaysians throw away", FACT_BODY, "6,000 tonnes", "of edible food every day", "SWCorp Malaysia", "https://www.nst.com.my/news/nation/2026/06/1463430/malaysians-throw-away-6000-tonnes-edible-food-daily-watch"),
    "SG" to InfoContent("🤯", "Singapore throws away", FACT_BODY, "over 750,000 tonnes", "of food every year", "Singapore NEA", "https://www.nea.gov.sg/our-services/waste-management/waste-statistics-and-overall-recycling"),
    "TH" to InfoContent("🤯", "Thailand wastes", FACT_BODY, "146 kg", "of food per person, every year", "Thailand Pollution Control Dept", "https://www.nationthailand.com/thailand/general/40034873"),
    "AU" to InfoContent("🤯", "The average Aussie household wastes", FACT_BODY, "$2,500 a year", "in avoidable food waste", "End Food Waste Australia", "https://endfoodwaste.com.au/fact-library/"),
)

val FOOD_WASTE_FACT_DEFAULT = InfoContent(
    "🤯", "The average person wastes", FACT_BODY, "79 kg", "of food a year",
    "UN Food Waste Index 2024", "https://www.unep.org/resources/publication/food-waste-index-report-2024",
)

// ── Mood → AI-prompt phrases (also fed to the meal-plan prompt) ──────────────

val MOOD_PHRASES = mapOf(
    "speedy"          to "quick, speedy meals with short prep and cook time",
    "low_calorie"     to "lighter, lower-calorie meals",
    "family_favs"     to "family-friendly crowd-pleasers",
    "healthy_comfort" to "healthy comfort food",
    "fakeaway"        to "healthier 'fakeaway' versions of takeaway favourites",
    "gut_friendly"    to "gut-friendly meals (fibre-rich, fermented, whole foods)",
    "protein_packed"  to "high-protein, protein-packed meals",
)

// ── The steps, in exact order ────────────────────────────────────────────────

val FUNNEL_STEPS: List<FunnelStep> = listOf(
    FunnelStep("goal", StepKind.SINGLE, "What are you trying to achieve?", options = listOf(
        FunnelOption("meal_prep", "Meal prep for the week", "📅"),
        FunnelOption("simple_recipes", "Find super simple recipes", "✨"),
        FunnelOption("feed_myself", "Feed myself", "🍽️"),
        FunnelOption("feed_family", "Feed my family", "👨‍👩‍👧"),
    )),
    FunnelStep("barrier", StepKind.SINGLE, "What prevents you from doing that?", options = listOf(
        FunnelOption("lack_of_time", "Lack of time", "⏱️"),
        FunnelOption("too_tired", "Too tired after work", "😪"),
        FunnelOption("cooking_difficult", "I find cooking difficult", "🤔"),
        FunnelOption("no_inspiration", "I struggle for inspiration", "💭"),
    )),
    FunnelStep("empathy", StepKind.INFO, info = InfoContent(
        "🧑‍🍳", "Planning meals is time-consuming",
        "We take away that stress and give you delicious meals to cook and enjoy.",
    )),
    FunnelStep("save_more", StepKind.SINGLE, "Do you feel you could save more on your shopping?", options = listOf(
        FunnelOption("definitely", "Definitely"),
        FunnelOption("most_likely", "Most likely"),
        FunnelOption("somewhat", "Somewhat"),
        FunnelOption("not_really", "Not really"),
    )),
    FunnelStep("name", StepKind.TEXT, "What's your name?"),
    FunnelStep("age", StepKind.SINGLE, "How old are you?", "We only use this to personalise your experience.", options = listOf(
        FunnelOption("24_under", "24 and under"),
        FunnelOption("25_34", "25–34"),
        FunnelOption("35_44", "35–44"),
        FunnelOption("45_54", "45–54"),
        FunnelOption("55_plus", "55+"),
    )),
    FunnelStep("region", StepKind.SINGLE, "Where are you from?", "We use this to set your currency, cuisine, and a few local touches.", options = listOf(
        FunnelOption("GB", "United Kingdom", "🇬🇧", "GBP"),
        FunnelOption("EU", "Europe", "🇪🇺", "EUR"),
        FunnelOption("US", "United States", "🇺🇸", "USD"),
        FunnelOption("CA", "Canada", "🇨🇦", "CAD"),
        FunnelOption("AU", "Australia", "🇦🇺", "AUD"),
        FunnelOption("NZ", "New Zealand", "🇳🇿", "NZD"),
        FunnelOption("MY", "Malaysia", "🇲🇾", "MYR"),
        FunnelOption("SG", "Singapore", "🇸🇬", "SGD"),
        FunnelOption("TH", "Thailand", "🇹🇭", "THB"),
        FunnelOption("OTHER", "My country isn't listed", "🌍"),
    )),
    FunnelStep("other_country", StepKind.OTHER_COUNTRY, "Which country are you in?",
        "We don't have local shops for your country yet. You can still add your store's website manually later — and tell us where you are so we can prioritise it."),
    FunnelStep("dietary", StepKind.MULTI, "Any dietary needs?", "Pick all that apply.", min = 0, max = 4, options = listOf(
        FunnelOption("none", "None", "🚫"),
        FunnelOption("vegetarian", "Vegetarian", "🥕"),
        FunnelOption("vegan", "Vegan", "🌱"),
        FunnelOption("pescatarian", "Pescatarian", "🐟"),
    )),
    FunnelStep("allergies", StepKind.MULTI, "Any allergies?", "Pick all that apply.", min = 0, max = 8, options = listOf(
        FunnelOption("none", "None", "🚫"),
        FunnelOption("gluten_free", "Gluten free", "🌾"),
        FunnelOption("dairy_free", "Dairy free", "🥛"),
        FunnelOption("nut_free", "Nut free", "🥜"),
        FunnelOption("egg_free", "Egg free", "🥚"),
        FunnelOption("shellfish_free", "Shellfish free", "🦐"),
        FunnelOption("sesame_free", "Sesame free", "🌰"),
        FunnelOption("soy_free", "Soy free", "🫘"),
    )),
    FunnelStep("proteins", StepKind.MULTI, "What do you like?", "Pick the proteins you enjoy.", min = 0, max = 4, options = listOf(
        FunnelOption("beef", "Beef", "🥩"),
        FunnelOption("pork", "Pork", "🥓"),
        FunnelOption("chicken", "Chicken", "🍗"),
        FunnelOption("fish", "Fish", "🐟"),
    )),
    FunnelStep("moods", StepKind.MULTI, "What are you in the mood for?", "Pick up to 3.", min = 1, max = 3, options = listOf(
        FunnelOption("speedy", "Speedy Meals", "⚡️"),
        FunnelOption("low_calorie", "Low Calorie", "⚖️"),
        FunnelOption("family_favs", "Family Favs", "👨‍👩‍👧"),
        FunnelOption("healthy_comfort", "Healthy Comfort", "🥗"),
        FunnelOption("fakeaway", "Fakeaway", "🥡"),
        FunnelOption("gut_friendly", "Gut Friendly", "🧑"),
        FunnelOption("protein_packed", "Protein Packed", "💪"),
    )),
    FunnelStep("cook_time", StepKind.SINGLE, "How long do you usually spend cooking?", "We'll match recipes to your routine.", options = listOf(
        FunnelOption("15_30", "15–30 min"),
        FunnelOption("30_45", "30–45 min"),
        FunnelOption("45_60", "45–60 min"),
        FunnelOption("60_plus", "60+ min"),
    )),
    FunnelStep("days", StepKind.MULTI, "Which days will you cook?", "Pick the days you want meals planned for.", min = 1, max = 7, options = listOf(
        FunnelOption("mon", "Monday"), FunnelOption("tue", "Tuesday"), FunnelOption("wed", "Wednesday"),
        FunnelOption("thu", "Thursday"), FunnelOption("fri", "Friday"), FunnelOption("sat", "Saturday"),
        FunnelOption("sun", "Sunday"),
    )),
    FunnelStep("household", StepKind.STEPPER, "How many are you cooking for?", "We'll scale your meals and budget.", min = 1, max = 12),
    FunnelStep("budget", StepKind.SLIDER, "What's your weekly budget?", "Slide to what you're happy to spend on those days.", min = 20, max = 310),
    FunnelStep("shop", StepKind.MULTI, "Choose your shop", "We'll plan your weekly shop around it — pick any you use.", min = 1, max = 8),
    FunnelStep("appliances", StepKind.MULTI, "What appliances do you have?", "Select at least one to plan with.", min = 1, max = 4, options = listOf(
        FunnelOption("microwave", "Microwave", "📟"),
        FunnelOption("hob", "Hob", "🔥"),
        FunnelOption("oven", "Oven", "🍞"),
        FunnelOption("air_fryer", "Air fryer", "🍟"),
    )),
    FunnelStep("value_savings", StepKind.INFO),
    FunnelStep("referral", StepKind.SINGLE, "Where did you hear about us?", options = listOf(
        FunnelOption("instagram", "Instagram", "📷"),
        FunnelOption("tiktok", "TikTok", "🎵"),
        FunnelOption("play_store", "Google Play", "▶️"),
        FunnelOption("word_of_mouth", "Word of mouth", "💬"),
        FunnelOption("others", "Others", "✳️"),
    )),
    FunnelStep("finale", StepKind.FINALE, info = InfoContent(
        "🥗", "your plan is ready to build",
        "we'll turn everything you told us into a week of dinners and one shopping list",
    )),
)
