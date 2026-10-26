# Nutrimo (Android)

French weight-loss app: calorie counter, recipes, weight goal and weigh-in tracking. Everything personal is stored
on the phone. Food data comes from CIQUAL (bundled, works offline), the user's own food API
([nutrimo-api](../nutrimo-api)) on the home network, and Open Food Facts as a fallback.

## Features

- First launch: profile (sex, birth date, height, activity) and first goal.
- Goal by pace (0.5 / 1 / 2 kg per week or custom) or by end date, with the daily calorie target
  (Mifflin-St Jeor maintenance − deficit, never below 1200 kcal women / 1500 kcal men) and safety warnings.
- Weight tab: dashboard (weight and time progress bars, start / current / target, lost, remaining, BMI,
  average loss per week, required pace, calorie target, planned and estimated end dates), weigh-in history
  (one per day, edit / delete, change since the previous one) and graph (weight, objective line, trend line
  projected to the estimated end date; 1 / 3 / 6 months or everything). Goal history in "Mes objectifs".
- Journal: day by day, calories eaten / target / remaining, protein-carbs-fat bars, 4 meals (breakfast, lunch,
  dinner, snacks) with subtotals, swipe to delete, tap to edit, copy a meal from the day before, water glasses.
  Tap the summary for every nutrient of the day (sugars, fiber, salt, vitamins, minerals).
- Calorie history (chart icon in the journal): daily calories vs target over 7 or 30 days, average, days on
  target, average macros, and the weight loss your logged calories should produce.
- Food search: instant results on the phone (CIQUAL + foods already used + custom foods + recipes), then the
  Nutrimo server on the home Wi-Fi; away from home, press search on the keyboard to query Open Food Facts.
  Foods you pick are cached on the phone. Recents, favorites (star) and "Mes aliments" tabs.
- Saved meals: the bookmark icon of a journal meal saves it (e.g. "Petit-déj habituel"); add it again in one tap
  from the "Repas" tab of the search.
- Barcode scan (camera, on-device ML Kit): phone cache → server → Open Food Facts → create the food.
- Quantity in grams, product portions ("1 pot (125 g)") or your own portions; live nutrient preview.
- Custom foods: name + label values per 100 g mandatory (kcal, fat, saturated fat, carbs, sugars, protein, salt);
  fiber, brand, barcode, vitamins and minerals optional.
- Recipes: ingredients picked from the same search (foods in grams, other recipes in parts), number of parts,
  optional cooked weight; nutrition per part and per 100 g; log "1,5 part" or grams in the journal.
- Body measurements (waist, hips, chest, arm, thigh).
- Reminders: weekly weigh-in and daily meal logging (only when nothing was logged that day).
- Backup (Plus → Sauvegarde): send everything to the Nutrimo server on your PC (now, or automatically every day
  on Wi-Fi) and restore the latest server backup; or export / import a JSON file.
- Server: set the Nutrimo API address (e.g. `http://192.168.1.20:3000`) and test the connection (Plus → Serveur).

## Demo data

Plus → Développeur → "Charger les données de démo" replaces all data with a realistic history (5 months of
weigh-ins, 30 days of meals, recipes…) so every screen can be tried; "Tout effacer" goes back to onboarding.

## Testing against the PC server over USB

`adb reverse tcp:3000 tcp:3000` lets the phone reach the PC's API as `http://localhost:3000` (no firewall rule needed).

## Build and install

Requirements: JDK 17, Android SDK (platform 36). Create `local.properties` with
`sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk`.

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Data sources and licences

- CIQUAL 2020, ANSES (Licence Ouverte / Etalab) — bundled in `app/src/main/assets/ciqual_foods.json`.
- Open Food Facts (Open Database License) — via the Nutrimo API or directly from openfoodfacts.org.
