# Mirror iOS: Home dashboard (Android)

Source of truth: `Sous Pantry/Features/Home/HomeView.swift`, `Services/StreakService.swift`, `Features/Home/PriceTrendsSheet.swift`, `Services/HaulStore.swift`. Reference screenshots: `Branding & Design System/All Screens/IMG_3211.PNG` and `IMG_3212.PNG` (Home, 48-item pantry, nothing expiring). Android target: `ui/home/HomeScreen.kt` + `HomeViewModel.kt`.

## Current gap

Android Home already has the hero (time-of-day greeting + first name over `hero_vegetables` with the 35/65/100% cream gradient), a navy **Pantry Audit Due** card (title, "N items need review", up to 4 rows with dot + "Today"/"N days"), the navy **Recent Haul** card (🛒 title, meal-time lime chip, "No scan recorded yet." / hard-coded "Last recipe scan was yesterday.", empty-state "Let's get you started" + two CTAs, "See what to cook →", PRICE TRENDS row with a no-op tap), a white **Pantry Health** card (donut, 4-row legend, "~$N at risk" / "Nothing expiring soon"), a white **Cooking streak** card (title, flame pill ≥3, meals line, savings pill, badge-ladder progress, DAILY STREAK dots, streak message) and a stray row of "No {ingredient}" chips between Recent Haul and Pantry Health. All streak numbers are hard-coded zeros (`HomeState` defaults), `estimatedRecipeCount` uses `items * 1.2` instead of the iOS formula, and nothing on the screen navigates with the iOS side-effects (expiring filter, pending Sous message).

iOS additionally has: dietary/restriction/avoid badges **inside the hero** (not a separate row); a latest-award icon next to the name; the audit card is **orange gradient** with "Last checked …", "Added X ago" sub-lines, "Expires today/tomorrow/in N days", "+ N more items expiring" and a **View Expiring Items** CTA; Recent Haul derives the last-scan label from the eReceipt store sessions, the iOS meal estimate, a stroked secondary button, and the Price Trends row opens a **Price Trends sheet**; the streak card is backed by a persisted `StreakService`, shows an "X earned! 🏅" line, and (when any award is earned) an **Awards** row that opens the **Awards sheet**; a 15-second **🎉 Congrats!** banner overlays the top when a new award is unlocked. Pull-to-refresh exists on iOS but only refetches trending recipes that are never rendered, so Android may skip it.

**Not on the iOS Home screen** (verify before building anything): the recipe carousel helpers (`TrendingCard`, `sectionHeader`, `loadingCarousel`, `sectionError` "Couldn't load recipes" + Retry), the quick-win sheet ("Sous Pantry is cooking…"), and `HomeRecipeDetailSheet` ("% match", "Suggested by Sous Pantry") all exist in `HomeView.swift` but nothing in `body` renders or presents them (`showQuickWinSheet` and `selectedDetail` are never set). `HomeService.swift` fetchers are likewise unused by Home. Do not port them to Home.

## Section order on iOS (top → bottom)

`ScrollView` → hero (33% of screen height, full-bleed under the status bar) → `VStack(spacing: 24)` padded 20 horizontal, 40 bottom:
1. Pantry Audit Due — only when `auditItems` non-empty
2. Recent Haul — always
3. Pantry Health — only when pantry non-empty
4. Cooking streak — always

Overlay: achievement banner at top (z 10). Sheets: Awards, Price Trends.

### 1. Hero / greeting header

- Copy: `"Good morning,"` (hour 5–11), `"Good afternoon,"` (12–16), `"Good evening,"` (else). 16pt bold `#5F5E5A`. Below: first word of `session.userName`, or `"Chef"` if empty, 36pt semibold `#0A2A1F`. Block bottom-left, padding 20 horizontal, 20 bottom. Top row reserves two empty 36×36 slots (avatar/bell) — render nothing.
- After the name (HStack spacing 8): icon of the **latest earned award** (`StreakService.earnedRewards().last`), 22pt semibold, gold `#C4965A` if `tier == .navy` else green `#2D5A3D`, bounce once. Hidden when no awards. Android: Material icon mapping per award symbol (basket/leaf/bolt/flame/medal/trophy/star/laurel/crown) — iOS uses SF Symbols, so pick nearest.
- Dietary badge row (horizontal scroll, spacing 6, top padding 4) shown only when `session.dietaryContext` is non-empty. Capsules, `caption2` semibold white, padding 8×3: dietary types in **green** (`vegan→Vegan, vegetarian→Vegetarian, pescatarian→Pescatarian, no_beef→No Beef, no_pork→No Pork, paleo→Paleo, keto→Keto, halal→Halal`), restrictions in **gold** (`gluten_free→GF, nut_free→Nut-Free, dairy_free→Dairy-Free, shellfish_free→Shellfish-Free, egg_free→Egg-Free, soy_free→Soy-Free, pregnancy→Pregnancy Safe`), then avoid-ingredients sorted as `"No {name}"` in **slate** `#647080`. Android: move the existing FlowRow of "No X" chips here and add the other two groups from `KEY_DIETARY_TYPES` / `KEY_FOOD_RESTRICTIONS`. Android chips currently have slate text on 12% slate; iOS is white text on solid colour.
- Data: `UserPreferencesRepository.userName`, `dietaryTypes`, `foodRestrictions`, `avoidIngredients`. Not tappable.

### 2. Pantry Audit Due card ("Save the food")

- Visible iff `auditItems` non-empty. `auditItems` = items with `expiryDate != nil && expiryDate <= now + 7 days` (includes already expired), sorted by expiry ascending. Android's `HomeState.auditItems` already matches.
- Background: linear gradient topLeading `#D1380D` → bottomTrailing `#F28514`, corner 16, padding 18, shadow `#E6591A` @45% radius 10 y 5. (Android is currently **navy** — wrong.)
- Title row: `exclamationmark.triangle.fill` in `#FFBF4D` (subheadline) + `"Pantry Audit Due"` title3 bold white.
- Subtitle (caption, white 75%): `"{lastChecked} · {N} item(s) need review"` where lastChecked = `"Never checked"` if `lastAuditDate` nil, `"Last checked today"`, `"Last checked yesterday"`, `"Last checked {d} days ago"`.
- Rows (max 4, spacing 10): 7pt dot (expired `#F05959`, today `#FF8C33`, ≤3 days `#FFCC33`, else white 50%), name subheadline semibold white 1 line, sub-line caption2 white 60% `"Added today" | "Added yesterday" | "Added {d} days ago" (d<7) | "Added {w} week(s) ago"`, trailing label caption semibold: `"Expired"` (`#FF8080`), `"Expires today"` (`#FFBF66`), `"Expires tomorrow"`, `"Expires in {d} days"` (white 85%). Day diff = calendar days from now to expiry (negative → expired).
- If more than 4: `"+ {N-4} more items expiring"` caption white 70%.
- CTA (full width, white 20% fill, corner 10, padding 12×16, white text): `checklist` icon + `"View Expiring Items"` subheadline semibold + spacer + chevron. Tap → `session.showExpiringFilter = true; selectedTab = 1` (Pantry tab opens with the "Expiring" special filter selected, and Pantry stamps `lastAuditDate = now`). Android: navigate to `pantry` route with an argument/flag that sets `specialFilter = "Expiring"` in `PantryScreen` and writes a new `KEY_LAST_AUDIT_DATE`.
- Dependency: new DataStore key `last_audit_date` (Long). On iOS it is set whenever the Pantry "Expiring" filter is selected (PantryView.swift L287-310).

### 3. Recent Haul card

- Always shown. Navy `#162437`, corner 18, shadow navy @45% radius 12 y 6. Inner `VStack(spacing 14)` padding 18.
- Title row: `"🛒"` 22pt, `"Recent Haul"` 15pt semibold white, spacer, meal chip only when pantry non-empty: `"TODAY'S BREAKFAST"` (5–10h) / `"TODAY'S LUNCH"` (11–14h) / `"TONIGHT'S DINNER"` (else), 10pt bold tracking 0.8, `#2CFF05` on `#2CFF05` @15% capsule, padding 9×4. Android matches.
- Subtitle 12pt white: `"No scan recorded yet."` when no store has ever synced, else `"Last recipe scan was today." | "… yesterday." | "… {d} days ago."` from the most recent `lastSyncDate` across `StoreSessionStore.shared.stores`. (Copy says "recipe scan" on iOS — keep verbatim.) Android: eReceipt state only keeps item counts per store (`EReceiptViewModel.lastSync: Map<String, Int>`); needs a persisted per-store last-sync timestamp. Until then show `"No scan recorded yet."`, never the hard-coded "yesterday".
- Empty pantry: `"Let's get you started"` 20pt bold white; primary `"Add new items →"` (15pt bold white, green `#2D5A3D`, corner 12, vertical 13) → Pantry tab; secondary `"View shopping list →"` (15pt semibold white 85%, white 10% fill **+ 1pt stroke white 18%**, corner 12) → Shopping tab. Android lacks the stroke.
- Non-empty: `"{N} meals you could cook right now. Hungry?"` 20pt bold where `N = min(max(max(1, count/8), 3), 25)` (48 items → 6, matches screenshot). Android's `items*1.2` is wrong. CTA `"See what to cook →"` (green, as above) → Plan & Cook tab **and** stores `pendingSousMessage = "Suggest {N} meals I can make with my current pantry."` which Plan & Cook sends as the opening chat message. Android: add the pref/arg and consume it in `PlanCookScreen` (no consumer exists yet).
- Price Trends row, only when pantry non-empty: 1pt divider white 12%; row padding 18×14: `chart.line.uptrend.xyaxis` 15pt white, column `"PRICE TRENDS"` 10pt bold tracking 1.2 white 50% / `"Tap to see spend insights"` 13pt medium white 90%, spacer, chevron 11pt semibold white 40%. Tap → **Price Trends sheet** (large detent).
- Also loaded on appear: `HaulStore.lastHaulCost/Count/StoreName` — fetched but **not rendered** anywhere in `body`. Skip.

### 3a. Price Trends sheet (`PriceTrendsSheet.swift`)

Nav title `"Price Trends"` (large), cream background, close `xmark.circle.fill` 28pt navy 25%. Sections (`VStack spacing 22`, padding 20/8/40), headers 11pt semibold tracking 1.2 slate:
- `TOP MOVERS`: items with ≥2 `PriceRecord`s, top 5 by |% change| oldest→newest. Row: name subheadline semibold navy; `"{old} → {new} / {unit}"` caption slate (AUD, 2dp; unit defaults `"each"`); store capsule caption2 white on green; `"+12%"`-style capsule bold white on red if up else green. Empty: `"No price changes yet. Record 2+ prices for an item to see movement."`
- `MONTHLY SPEND`: last 3 months of `HaulStore.haulHistory` cost sums, newest first; row = 3-letter month (44pt) + bar (softMint 40% track, green fill, height 16, corner 6) + whole-dollar total. Empty: `"Record a few shops to see monthly spending trends."`
- `BEST STORE FOR YOU` (hidden when none): items priced at ≥2 stores, latest price per store, cheapest vs priciest, savings > $0.05, top 3: `"🛒 {item}: {cheap} saves ~{$x.xx} vs {pricey}"`.
- `SPENDING STREAK` (hidden unless ≥2): consecutive most-recent hauls with `cost <= threshold` (`spendThreshold` pref, default 150): `"Under $150/shop"` bold + `"You've stayed under $150/shop for {n} visits in a row 🎉"` on softMint card with gold flame.
- Empty/white cards corner 12, shadow navy 6% radius 4 y 2. Currency: iOS hard-codes `en_AU`; Android should use the wizard currency per the eReceipt spec.
- **Dependencies (none exist on Android):** `price_records` table (`id, pantryItemId, pricePerUnit, unitLabel, store, customStore?, datePurchased`; iOS keeps the 3 most recent per item) written by receipt scan / eReceipt sync / Add Price Record; `haul_history` (date, cost, count, storeName?) written by `recordHaul` after each scan/sync; `spend_threshold` pref.

### 4. Pantry Health card

- Visible iff pantry non-empty. White card corner 16, padding 16, shadow navy 8% radius 8 y 3. Title `"🫙"` 20pt + `"Pantry Health"` title3 bold navy.
- Donut 100×100: base ring softMint `#E3EDE6` stroke 18, arcs from -90° in palette order green `#2D5A3D`, navy `#162437`, gold `#C4965A`, system orange `#FF9500`, purple `#8C40BF` for the top-5 categories by count (fraction of total). Centre: count title2 bold navy + `"items"` caption2 slate. Android uses 110dp and `#C76F3F` for the 4th colour — change to 100dp / `#FF9500`.
- Category key must be `displayCategory` (iOS `PantryItem.normalizeCategory`), falling back to `"Other"`. Android groups raw `category` and has no `normalizeCategory` equivalent anywhere (grep confirms) — port iOS `PantryItem.normalizeCategory` as a shared helper and use it here and in the Pantry screen's category chips.
- Legend: top 4, 8pt dot, name caption navy 1 line, `"{pct}%"` caption semibold slate (Int truncation). Divider. Then `exclamationmark.triangle.fill` gold + `"~${atRisk*4.50, 0dp} at risk"` gold when `auditItems.count > 0`, else `checkmark.circle.fill` green + `"Nothing expiring soon"` green, caption semibold.
- Not tappable. (The iOS `"Add items to your pantry to see your kitchen health snapshot."` branch is unreachable because the card is hidden when empty.)

### 5. Cooking streak card (meals + DAILY STREAK + Awards)

- Always shown. `soCard()`: padding 16, system background (white), **corner 14**, shadow navy 7% radius 8 y 2. Inner spacing 16 with `Divider()`s between the three sections.
- Section 1: `"🍳"` 16pt + `"Cooking streak"` 16pt bold navy; trailing pill when `currentStreak >= 3`: `flame.fill` 12pt + `"{n} day(s)"` 12pt bold, both system orange `#FF9500`, on orange 12% capsule, padding 8×4. Lines: `"{mealsThisWeek} meal(s) cooked this week"` 15pt bold navy; `"~ {$} saved from eating out"` 12pt semibold green on softMint capsule (padding 10×4) where `$ = "$" + Int(round(mealsThisWeek * 16))` — **no decimals** (Android prints `$0.00`). Progress bar height 5 (gray 15% track, green fill) then optional **earned line** 13pt bold green `"{prevTitle} earned! {icon}"` (omitted before the first tier) then goal line 12pt slate `"{remaining} more meal(s) to {title} {icon}"`; past the top tier: earned `"Grand Legendary Chef unlocked! 👑"`, goal `"You've reached the top — legend!"`. Ladder identical to Android's `nextBadgeInfo` (5 Home Cook 🏅 … 3000 Grand Legendary Chef 👑). Android is missing the earned line and the unlocked-top earned text.
- Section 2: `"DAILY STREAK"` 11pt semibold tracking 1.2 slate; 7 cells Mon…Sun (11pt semibold, vertical padding 10, corner 8, spacing 6): cooked → green/white, today → navy/white, future → gray 15%/slate, past → gray 25%/slate. `cookedWeekdays` = today-minus-0…min(mealsThisWeek,7)-1 plus `lastCookedDate`'s weekday (only when `lastCookedDate` exists). Message 13pt navy: `"Cook today to start a streak. Keep going!"` (0), `"1-day streak — Great start! Keep going!"`, `"{n}-day streak 🔥 Keep going!"` (2–6), `"{n}-day streak 🔥 You're on fire! Keep going!"` (7–13), `"{n}-day streak 🔥 You're unstoppable!"` (14+). Android matches except it ignores the Sydney-day rule (iOS uses `Australia/Sydney` for all day boundaries — UNCLEAR whether Android should keep AEST or use device zone; iOS comment says AEST deliberately).
- Section 3 (only when `earnedRewards()` non-empty): tappable row on softMint 45%, corner 10, padding 12×10: `"Awards"` 15pt semibold navy + chevron 12pt slate; `"{count} earned"` 12pt medium slate. Tap → **Awards sheet**.
- Data: `StreakService` — `currentStreak`, `mealsThisWeek` (reset when ISO week changes), `totalMealsCooked`, `lastCookedDate`, per-award `streak_awarded_{case}` flags. `recordMealCooked()` is called from the Plan & Cook cook-confirm paths (`RecipeDetailSheet`, `ExternalRecipeSheet`): same day → counters +1; new consecutive day → streak +1; gap → streak = 1; then evaluates awards and sets `bannerReward`. `checkStreakExpiry()` on launch zeroes the streak after a 2+ day gap.
- **Dependency:** Android has no streak store. Add a `StreakRepository` (DataStore keys `streak_currentStreak`, `streak_mealsThisWeek`, `streak_totalMealsCooked`, `streak_lastCookedDate`, `streak_weekStart`, `streak_awarded_{rawValue}` ×24, `streak_bannerReward`) and call `recordMealCooked()` from `PlanCookViewModel.markCooked` and `MyPlansViewModel.markCooked` (both currently only delete pantry items). The 24 award ids/titles/thresholds are in `StreakService.swift` L21-228; the compound chef tiers also need the count of custom recipes (`MyRecipeRepository.recipes.size`).

### 5a. Awards sheet (`AwardsSheet`)

Nav title `"Awards"`, cream, close `xmark` 12pt bold slate in 28pt slate-12% circle. Subtitle `"{earned}/24 awards completed"` 14pt medium slate. 2-column grid (spacing 12) of 24 cells (padding 12, corner 14): 44pt icon tile (corner 10), title 13pt bold (green if earned, slate 45% if locked with zero progress, else navy), 3pt progress bar only when in progress, status 11pt medium (green if earned else slate 55%). Status strings per award family: `"Unlocked — {need} meals cooked"` / `"{have} meals cooked — {toGo} to go"`, `"Unlocked — 7-day streak"` / `"{n}-day streak — {toGo} to go"`, `"{have} / 5 quick meals"`, `"{have} scanned — {toGo} to go"`, `"{have} rescued — {toGo} to go"`, compound `"{m}/{M} meals · {r}/{R} own recipes"`. Earned card tints: amber `#F7F5E6`, teal softMint 60%, navy `#EBF2FC`, gold `#FCF7E0`, gray white. Last grid item spans both columns: lock icon + `"More awards coming"` 15pt bold slate 50% + `"New badges will appear here as we add more ways to celebrate your cooking."` + `"Stay tuned"`. Scan/rescue/speed counters are 0 on iOS too ("not wired yet") — mirror as 0.

### 6. Achievement banner ("Congrats" toast)

On Home appear, if `StreakService.bannerReward` is set: clear it, show the banner for 15 s (spring in from top, padding 16/12, white card corner 14, shadow navy 12% radius 10 y 4): award icon 20pt (gold if navy tier else green), `"🎉 Congrats!"` 12pt bold navy, `"You earned yourself a {title}!"` 13pt semibold navy (2 lines), spacer, `xmark` 11pt bold slate in 22pt gray-15% circle that dismisses early. Android: same, driven by the new `streak_bannerReward` pref.

### 7. Footer links

There is no separate footer on iOS. `"Add new items →"`, `"View shopping list →"` and `"See what to cook →"` are the Recent Haul CTAs described in §3 (tabs 1, 3, 2 respectively = Android routes `pantry`, `shopping`, `plancook`).

## Dependencies (must exist before the matching section can be real)

1. `StreakRepository` (DataStore) + `recordMealCooked()` hooks in both `markCooked` paths — unblocks §1 badge icon, §5 numbers, §5a, §6.
2. `price_records` Room table + `PantryItem.priceHistory` relation, written by receipt scan, eReceipt sync and an "Add Price Record" UI — unblocks TOP MOVERS / BEST STORE in §3a.
3. `haul_history` (per scan/sync: date, cost, count, storeName) — unblocks MONTHLY SPEND / SPENDING STREAK in §3a.
4. Per-store `lastSyncDate` persisted by eReceipt sync — unblocks the Recent Haul subtitle.
5. `last_audit_date` pref written by the Pantry "Expiring" filter; `showExpiringFilter` and `pendingSousMessage` signals consumed by Pantry / Plan & Cook.
Until 1–4 land, render the iOS empty states (zeros, "No scan recorded yet.", sheet empty cards), not placeholders.

## Polish details (iOS values)

- Page background cream `#F7F3EC`; hero image bleeds under the status bar; hero height = 33% of screen height (Android uses fixed 330dp — use fraction of window height).
- Card stack: horizontal padding 20, inter-card spacing **24** (Android 20), bottom padding 40.
- Navy cards (`#162437`): corner 18, shadow navy 45% radius 12 y 6. White cards: Pantry Health corner 16 / shadow 8% r8 y3; streak `soCard` corner 14 / shadow 7% r8 y2; audit card corner 16 orange gradient.
- Palette: navy `#162437`, green `#2D5A3D`, cream `#F7F3EC`, softMint `#E3EDE6`, gold `#C4965A`, slate `#647080`, lime `#2CFF05`, system orange `#FF9500`, muted gray `#5F5E5A`, deep green `#0A2A1F`.
- Type: greeting 16 bold / name 36 semibold; card titles title3 bold (≈20) or 15–16; body 12–15; tracked caps labels 10–11 with tracking 1.2.
- Buttons inside navy cards: corner 12, vertical padding 13, 15pt.

## Test cases

1. Fresh install, empty pantry: hero shows greeting + "Chef"; no audit card; Recent Haul shows no meal chip, "No scan recorded yet.", "Let's get you started", both CTAs (secondary has a faint border); no Price Trends row; no Pantry Health card; streak card shows "0 meals cooked this week", "~ $0 saved from eating out", "5 more meals to Home Cook 🏅", today's cell navy, "Cook today to start a streak. Keep going!"; no Awards row.
2. Add 48 items with no expiry: Recent Haul says "6 meals you could cook right now. Hungry?" with the meal chip; Price Trends row appears; Pantry Health shows "48 / items", 4 legend rows with %, "Nothing expiring soon" in green.
3. Set one item to expire tomorrow and one to 2 days ago: orange audit card appears first with "Never checked · 2 items need review", rows "Expired" (pink) and "Expires tomorrow"; Pantry Health switches to "~$9 at risk" in gold.
4. Add 6 expiring items: card lists 4 and "+ 2 more items expiring"; tap "View Expiring Items" → Pantry opens on the Expiring filter; back on Home the subtitle reads "Last checked today · …".
5. Tap "See what to cook →": Plan & Cook opens and the chat auto-sends "Suggest 6 meals I can make with my current pantry."
6. Mark a meal cooked in Plan & Cook, return Home: "1 meal cooked this week", "~ $16 saved…", today's cell green, "1-day streak — Great start! Keep going!"; mark 4 more today → "🎉 Congrats! You earned yourself a Home Cook!" banner, "Home Cook earned! 🏅" line, Awards row "1 earned", a medal icon beside the name.
7. Open Awards: "1/24 awards completed", Home Cook cell tinted amber/green, Super Home Cook shows progress "5 meals cooked — 15 to go", last cell "More awards coming".
8. Change device time so 2 days pass, relaunch: streak resets to 0 but total meals and awards persist; new ISO week resets "meals cooked this week".
9. Set dietary type Vegan, restriction Gluten-free and avoid "Alcohol": hero shows green "Vegan", gold "GF", slate "No Alcohol" chips under the name; no chip row elsewhere on the page.
10. Tap Price Trends with no price/haul data: sheet opens with TOP MOVERS and MONTHLY SPEND empty cards only; after two hauls under $150 and two prices for one item, the row, bar and "2 visits in a row" streak appear.
