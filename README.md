# Sous Pantry — Android

The Android app for Sous Pantry: track what's in your pantry, cook from it with an AI chef, and turn what you're missing into a shopping list. It mirrors the shipped iOS app (`Sous-Pantry-ios`) feature for feature.

Kotlin · Jetpack Compose (Material 3) · Hilt · Room · DataStore · Retrofit/OkHttp · Coil

## Getting started

**Requirements:** Android Studio (recent), JDK 17. The app targets SDK 35 and runs on Android 8.0 (API 26) and up.

1. Clone the repo and open the project root in Android Studio.
2. Get `local.properties` from James and put it in the project root. It holds the app's private settings, is git-ignored, and must never be committed.
3. **File → Sync Project with Gradle Files**, then **Run**.

Settings in `local.properties` are compiled into the app, so changing them needs a rebuild, not just a sync.

## How it works

**No provider keys ship in the app.** AI features and recipe images go through a server-side proxy shared with the iOS app, so both apps show the same results and images.

### The optional local backend

`backend/` is a small Express server kept for features that haven't moved to the proxy yet:

- **Barcode lookup** (`/api/barcode`)
- **Recipe text scanning** (`/api/recipe/scan-text`)

It's also the fallback for AI features when the app's settings aren't configured. To run it on a USB-connected phone:

```bash
cd backend && npm install && npm start
```

```bash
adb reverse tcp:3010 tcp:3010
```

Use `npm start`, not `npm run dev`. The tunnel drops whenever the phone is unplugged.

## Project layout

```
app/src/main/java/com/souspantry/app/
├── data/          Room entities, DAOs, DataStore preferences, models
├── navigation/    NavHost, bottom nav, app start gating
├── services/      AI client, recipe links, images, ingredient logic
└── ui/
    ├── funnel/    21-step onboarding funnel (region, shops, diet, moods)
    ├── pantry/    Pantry list, barcode and paper-receipt scanning
    ├── plancook/  Discover chat, recipe cards, cooking session, saved recipes, plans
    ├── shopping/  Shopping list
    ├── ereceipt/  Supermarket eReceipt sync (every region)
    ├── account/   Account, dietary and meal-mood editors
    └── theme/     Design system colours and type scale
backend/           Optional Node backend (see above)
```

Worth knowing before you change these areas:

- **Pantry staples** (`IngredientStaples.kt`): salt, pepper, water, sugar and basic oils always count as on hand. Matching is exact, so "salted butter" and "olive oil" are real ingredients.
- **Pasted recipe URLs** (`RecipeLinkResolver.kt`): a link the user pasted is always opened directly and never replaced by a search.
- **What to buy** (`BuyList.kt`): built from the recipe page's real ingredients when the page publishes them; the AI's guess is only a fallback.
- **eReceipt sync**: one store-agnostic pipeline for every supermarket. Stores per region live in `REGION_STORES`. Don't add per-store parsing.

## Tests

Unit tests cover the pure logic: the link resolver, pantry staples, the buy list and ingredient cleaner, image slugs and receipt parsing.

```bash
./gradlew testDebugUnitTest
```

## Troubleshooting

**"I'm having trouble reaching the kitchen brain"** is a catch-all error. Check in this order:

1. `local.properties` is set up, and the app was rebuilt (not just synced) after it last changed.
2. `adb logcat -s PlanCookVM:E` shows the real exception.
3. If you're using the local backend: it's running, it's the copy in *this* repo (only it has `/api/meals/chat`), and `adb reverse` is active.

eReceipt failures log under `adb logcat -s EReceipt:E`.

## Status

Known gaps compared with iOS:

- Play Billing is not wired up yet; purchases are a local stub.
- No price history (iOS records prices from receipts).
- Trial-ending notification and the spotlight tour are not built.
