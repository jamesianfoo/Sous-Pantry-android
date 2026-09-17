# Sous Pantry — Android

The Android app for Sous Pantry: track what's in your pantry, cook from it with an AI chef, and turn what you're missing into a shopping list. It mirrors the shipped iOS app (`Sous-Pantry-ios`) feature for feature.

Kotlin · Jetpack Compose (Material 3) · Hilt · Room · DataStore · Retrofit/OkHttp · Coil

## Getting started

**Requirements:** Android Studio (recent), JDK 17. The app targets SDK 35 and runs on Android 8.0 (API 26) and up.

1. Clone the repo and open the project root in Android Studio.
2. Create `local.properties` in the project root (it's git-ignored — never commit it):

   ```properties
   sdk.dir=/Users/<you>/Library/Android/sdk
   SP_PROXY_SECRET=<app secret>
   UNSPLASH_ACCESS_KEY=<unsplash access key>
   BASE_URL=http://127.0.0.1:3010
   ```

3. **File → Sync Project with Gradle Files**, then **Run**.

`SP_PROXY_SECRET` and `UNSPLASH_ACCESS_KEY` are compiled into `BuildConfig`, so changing either needs a rebuild, not just a sync.

### Configuration

| Key | Required | What it does |
|---|---|---|
| `SP_PROXY_SECRET` | Yes, for AI features | App-scoped token the Worker checks as `Authorization: Bearer …`. Same value as the iOS app's worker secret. **Not** an Anthropic key. |
| `UNSPLASH_ACCESS_KEY` | No | Cuisine-generic stand-in photos while a dish's real image generates. Without it, cards show a cuisine colour instead. |
| `BASE_URL` | Only for backend-only features | The optional local Node backend (see below). Release builds use `https://api.souspantry.com`. |

## How it works

**No provider keys ship in the app.** Every model call goes through the shared Cloudflare Worker (`souspantry-images.souspantry.workers.dev`), which holds the Anthropic and ILMU keys server-side:

- `POST /anthropic` — Anthropic Messages pass-through, used by `services/DirectClaude.kt` for chat, shopping lists and receipt parsing. An `X-SP-Region` header lets the Worker route some countries (e.g. Malaysia) to ILMU; responses come back in the same shape either way.
- `GET /img/<slug>.webp` and `POST /generate` — the recipe image store shared with iOS (`services/RecipeImages.kt`). The slug must match the Worker's `slugKey()` exactly, or Android looks up a different image than iOS generated.

### The optional local backend

`backend/` is a small Express server kept for features that haven't moved to the Worker yet:

- **Barcode lookup** (`/api/barcode`)
- **Recipe text scanning** (`/api/recipe/scan-text`)

It's also the fallback for AI features when `SP_PROXY_SECRET` is blank. To run it on a USB-connected phone:

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
├── services/      Worker/AI client, recipe links, images, ingredient logic
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
- **Pasted recipe URLs** (`RecipeLinkResolver.kt`): a link the user pasted is always opened directly and never replaced by a search. It scrapes pages with a bare HTTP client so the app secret is never sent to third-party sites.
- **What to buy** (`BuyList.kt`): built from the recipe page's real ingredients when the page publishes them; the AI's guess is only a fallback.
- **eReceipt sync**: one store-agnostic pipeline for every supermarket. Stores per region live in `REGION_STORES`. Don't add per-store parsing.

## Tests

Unit tests cover the pure logic: the link resolver, pantry staples, the buy list and ingredient cleaner, image slugs and receipt parsing.

```bash
./gradlew testDebugUnitTest
```

## Troubleshooting

**"I'm having trouble reaching the kitchen brain"** is a catch-all error. Check in this order:

1. `app/build/generated/source/buildConfig/debug/com/souspantry/app/BuildConfig.java` contains a non-empty `SP_PROXY_SECRET`. If it's empty, rebuild after editing `local.properties`.
2. `adb logcat -s PlanCookVM:E` shows the real exception.
3. If you're using the local backend: it's running, it's the copy in *this* repo (only it has `/api/meals/chat`), and `adb reverse` is active.

eReceipt failures log under `adb logcat -s EReceipt:E`.

## Status

Known gaps compared with iOS:

- Play Billing is not wired up yet; purchases are a local stub.
- No price history (iOS records prices from receipts).
- Trial-ending notification and the spotlight tour are not built.
