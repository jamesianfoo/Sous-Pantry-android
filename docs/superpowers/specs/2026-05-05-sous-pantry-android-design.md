# Sous Pantry Android — Design Spec

**Date:** 2026-05-05  
**Target:** Native Android app, full parity with iOS production app  
**Timeline:** 7–10 days  
**Working directory:** `SousPantry_Android/`  
**iOS reference:** `SousPantry_XCode/` (read-only — do not modify)  
**Backend:** `SousPantry_XCode/backend/` — Node.js at `https://api.souspantry.com`

---

## Context

The iOS app is complete and submitted for App Store review. This spec covers the native Android port. The scaffold was salvaged from `android.old/` — all 4 core screens (Home, Pantry, Shopping, Plan & Cook) have real working code, API wiring, and Room persistence. The missing 40% is auth, billing, FCM, settings, and onboarding.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Navigation | Compose Navigation (bottom nav, 4 tabs) |
| Network | Retrofit 2.11 + OkHttp + Gson |
| Local DB | Room 2.7 (Pantry items only) |
| Preferences | DataStore Preferences |
| Images | Coil 2.7 |
| Camera | CameraX 1.4 + ML Kit (barcode + text recognition) |
| Push | Firebase Cloud Messaging (FCM) |
| Auth | Supabase Kotlin SDK (stage 2) |
| Billing | Google Play Billing v7 (stage 2) |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 |

---

## Architecture

```
ui/
  home/         HomeScreen, HomeViewModel
  pantry/       PantryScreen, PantryViewModel
  shopping/     ShoppingScreen, ShoppingViewModel
  plancook/     PlanCookScreen, PlanCookViewModel
  settings/     SettingsScreen, SettingsViewModel      ← new stage 1
  auth/         LoginScreen, SignUpScreen               ← new stage 2
  paywall/      PaywallScreen                          ← new stage 2
  onboarding/   OnboardingScreen                       ← new stage 3
  theme/        Theme.kt, Type.kt
data/
  repository/   PantryRepository
  local/        PantryDatabase, PantryDao, PantryItem
  remote/       ApiService, NetworkModule
  models/       Models.kt
services/
  messaging/    FcmService (FirebaseMessagingService)  ← new stage 1
workers/        (reserved — none active)
navigation/     NavHost.kt
```

---

## Backend API Contract

All routes are on `https://api.souspantry.com`. No auth token required for stage 1 routes.

| Route | Auth | Used by |
|---|---|---|
| `POST /api/identify` | None | Pantry — camera identify |
| `POST /api/receipt/image` | None | Pantry — receipt scan |
| `POST /api/receipt/text` | None | Pantry — receipt text |
| `GET  /api/barcode/:code` | None | Pantry — barcode lookup |
| `POST /api/meals/generate` | None | Plan & Cook |
| `POST /api/meals/trending` | None | Home |
| `POST /api/meals/pantry` | None | Home |
| `POST /api/meals/adventurous` | None | Home |
| `POST /api/meals/suggested` | None | Home |
| `POST /api/shopping/generate` | None | Shopping |
| `POST /api/shopping/staples` | None | Shopping |
| `POST /api/ai/*` | Supabase JWT | Stage 2+ |
| `POST /api/redeem-promo` | Supabase JWT | Stage 2+ |

Auth header format (stage 2): `Authorization: Bearer <supabase-access-token>`

---

## Feature Staging

### Stage 1 — Days 1–6 (current sprint)

| # | Feature | Status | Notes |
|---|---|---|---|
| 1 | Pantry CRUD + barcode + receipt scan | Scaffolded | Polish UI, wire camera flows end-to-end |
| 2 | Home (4 recipe sections) | Scaffolded | Polish loading/error states |
| 3 | Shopping List | Scaffolded | Polish empty states |
| 4 | Plan & Cook | Scaffolded | Polish recipe card expand/collapse |
| 5 | Settings/Profile screen | New | Name, preferences stored in DataStore |
| 6 | FCM Push Notifications | New | Anonymous device token; backend maps token to push events |

### Stage 2 — Days 7–8

| # | Feature | Notes |
|---|---|---|
| 7 | Supabase Auth | Email/password login + signup. Session token persisted in DataStore. JWT attached to `/api/ai/*` calls. |
| 8 | Google Play Billing | GPB v7 native. Subscription paywall screen. Entitlement check gates premium features. |

### Stage 3 — Day 9

| # | Feature | Notes |
|---|---|---|
| 9 | Onboarding | 3-screen Compose HorizontalPager. Shown once on first launch via DataStore `onboarding_complete` flag. |

**Day 10:** Bug fixes, polish, internal test build.

---

## Key Technical Decisions

**Offline behaviour:** Pantry items survive offline via Room. All other features (recipes, shopping, plan/cook) require network — show empty state + retry button, never crash.

**FCM without auth (stage 1):** FCM token registered against anonymous `deviceId` (UUID stored in DataStore on first launch). When auth lands in stage 2, token is re-registered against the Supabase `userId`.

**Camera:** CameraX preview + ML Kit barcode scanner for barcode lookup. CameraX + ML Kit text recognition for receipt scan (image captured → base64 → `POST /api/receipt/image`). Both flows open a full-screen camera composable.

**No dark mode:** Not present in iOS app, not implemented in Android.

**No geofence:** Removed from scope for both platforms.

---

## Risks

| Risk | Mitigation |
|---|---|
| GPB test purchases require signed build + Play Console | Set up Play Console listing during stage 1 so it's ready for stage 2 |
| Supabase Kotlin SDK version conflicts with Compose | Pin to known-good version; spike on Day 7 before full auth build |
| Samsung/OEM battery optimization killing FCM | Guide user to exempt app in Settings screen |
| Stage 2/3 slip | Stage 1 is shippable standalone as internal beta |

---

## Out of Scope

- Dark mode
- Geofence / location auto-sync
- iPad / tablet layout
- Deep links
- Widget
- Apple-specific features (Sign in with Apple, CloudKit, etc.)
