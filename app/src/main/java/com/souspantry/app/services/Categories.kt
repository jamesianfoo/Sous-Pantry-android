package com.souspantry.app.services

/**
 * Canonical pantry categories, ported verbatim from iOS
 * `PantryItem.categoryOrder` / `PantryItem.normalizeCategory`. The Home
 * "Pantry Health" donut and the Pantry category chips group by
 * [normalizeCategory] (falling back to "Other"), exactly as iOS does.
 */
object Categories {

    val ORDER: List<String> = listOf(
        "Fruits", "Vegetables", "Meat & Seafood", "Dairy & Eggs",
        "Bakery & Bread", "Cereals & Breakfast", "Pantry Staples",
        "Snacks", "Beverages", "Frozen Foods", "Condiments & Sauces",
        "Canned & Packaged", "Health & Nutrition", "Other",
    )

    /** Clean, consistent category label for display and grouping; null only when [raw] is null. */
    fun normalizeCategory(raw: String?): String? {
        val r = raw?.lowercase() ?: return null

        // If the raw value already IS a canonical category (e.g. the user
        // selected "Pantry Staples" from the picker), return it immediately
        // instead of falling through to the keyword matching below.
        ORDER.firstOrNull { it.lowercase() == r }?.let { return it }

        if (r.contains("fruit") || r.contains("berr") || r.contains("apple") ||
            r.contains("orange") || r.contains("citrus") || r.contains("mango") ||
            r.contains("banana") || r.contains("grape") || r.contains("melon") ||
            r.contains("pineapple") || r.contains("stone fruit")) return "Fruits"

        if (r.contains("vegetable") || r.contains("veggie") || r.contains("salad") ||
            r.contains("carrot") || r.contains("broccoli") || r.contains("spinach") ||
            r.contains("onion") || r.contains("potato") || r.contains("tomato") ||
            r.contains("lettuce") || r.contains("cucumber") || r.contains("zucchini")) return "Vegetables"

        if (r.contains("meat") || r.contains("beef") || r.contains("chicken") ||
            r.contains("pork") || r.contains("lamb") || r.contains("fish") ||
            r.contains("seafood") || r.contains("poultry") || r.contains("turkey") ||
            r.contains("sausage") || r.contains("deli") || r.contains("mince")) return "Meat & Seafood"

        if (r.contains("dairy") || r.contains("milk") || r.contains("cheese") ||
            r.contains("yogurt") || r.contains("yoghurt") || r.contains("egg") ||
            r.contains("butter") || r.contains("cream")) return "Dairy & Eggs"

        // iOS: `... || r.contains("roll") && !r.contains("spring roll")` — && binds tighter than ||.
        if (r.contains("bread") || r.contains("bakery") || r.contains("baked") ||
            r.contains("pastry") || r.contains("loaf") || r.contains("baguette") ||
            r.contains("croissant") || (r.contains("roll") && !r.contains("spring roll"))) return "Bakery & Bread"

        if (r.contains("cereal") || r.contains("breakfast") || r.contains("oat") ||
            r.contains("muesli") || r.contains("granola") || r.contains("porridge")) return "Cereals & Breakfast"

        if (r.contains("snack") || r.contains("chip") || r.contains("crisp") ||
            r.contains("biscuit") || r.contains("cookie") || r.contains("chocolate") ||
            r.contains("candy") || r.contains("loll") || r.contains("confection") ||
            (r.contains("sweet") && !r.contains("sweet potato"))) return "Snacks"

        if (r.contains("beverage") || r.contains("drink") || r.contains("juice") ||
            r.contains("water") || r.contains("coffee") || r.contains("tea") ||
            r.contains("soda") || r.contains("cola") || r.contains("wine") ||
            r.contains("beer") || r.contains("alcohol") || r.contains("soft drink") ||
            r.contains("smoothie")) return "Beverages"

        if (r.contains("frozen")) return "Frozen Foods"

        if (r.contains("sauce") || r.contains("condiment") || r.contains("dressing") ||
            r.contains("oil") || r.contains("vinegar") || r.contains("mustard") ||
            r.contains("ketchup") || r.contains("spice") || r.contains("seasoning") ||
            r.contains("herb") || r.contains("marinade") || r.contains("relish") ||
            r.contains("chutney")) return "Condiments & Sauces"

        if (r.contains("canned") || r.contains("tinned")) return "Canned & Packaged"

        if (r.contains("pasta") || r.contains("rice") || r.contains("noodle") ||
            r.contains("grain") || r.contains("legume") || r.contains("bean") ||
            r.contains("lentil") || r.contains("flour") || r.contains("sugar") ||
            r.contains("stock") || r.contains("broth") || r.contains("plant-based") ||
            r.contains("preserved")) return "Pantry Staples"

        if (r.contains("health") || r.contains("supplement") || r.contains("protein") ||
            r.contains("vitamin") || r.contains("nutrition")) return "Health & Nutrition"

        return "Other"
    }
}
