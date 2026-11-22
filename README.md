# Nutrimo (Android)

French weight-loss app: calorie counter, recipes, weight goal and weigh-in tracking. Everything personal is stored
on the phone. Food data comes from CIQUAL (bundled, works offline), the user's own food API
([nutrimo-api](../nutrimo-api)) on the home network, and Open Food Facts as a fallback.

## Features

- First launch: profile (sex, birth date, height, activity) and first goal.
- Goal by pace (0.5 / 1 / 2 kg per week or custom) or by end date, with the daily calorie target
  (Mifflin-St Jeor maintenance − deficit, never below 1200 kcal women / 1500 kcal men) and safety warnings.
- Weight tab (hub): one chart with the weight, objective and trend lines over tinted BMI zones (WHO categories for the
  profile height); under the plot, a diamond on each measurement day and a ring on each photo day; 3 months / 6 months / 1 year / all chips
  (the last weigh-in at 3/4 of the width, the goal and trend lines ahead); the trend line follows the weigh-ins of
  the last 30 days (even from before the goal start), pinch to zoom and side drag to move in time; tap a day for a bubble (weight, measurements, photos) with links. Below: Bilan, Mensurations and Photos,
  and a "+" to add a weigh-in, measurements or a photo.
- Bilan: dashboard (weight and time progress bars with their % on the bar line, start / current / target, lost, remaining, BMI
  and the next BMI category down (threshold weight, kg left, date by the trend),
  average loss per week, required pace, calorie target, planned and estimated end dates), weigh-in history
  (one per day, edit / delete, change since the previous one). Goal history in "Mes objectifs".
- Journal: day by day, calories eaten / target / maintenance / remaining, protein-carbs-fat bars. The calorie ring
  is green under the target, yellow above it but still under maintenance (still losing, just slower), red above
  maintenance. 4 meals (breakfast, lunch, dinner, snacks) with subtotals, swipe to delete, tap to edit, copy a meal to a chosen day and meal (today and the same meal by default), move a meal (⋮ menu, e.g. logged on the wrong date), water glasses.
  Tap the summary for every nutrient of the day (sugars, fiber, salt, vitamins, minerals), each with its % of the
  daily reference for the profile's sex and age (ANSES needs; salt, sodium, sugars and saturated fat as WHO / EU limits).
- "Ma journée" (bottom of the journal): the mood of the day in one tap (5 faces) and a short journal (one text,
  prompts as placeholder: gratitude, victory of the day, a few words). Written full screen, save icon at the top;
  a tap outside never closes it, and leaving with unsaved text asks first. Included in the backups.
- Macro targets (Plus → Objectifs de macros): balanced, high-protein or low-carb presets, or your own percentages;
  shows the grams and protein per kg; the journal bars use them.
- Steps and workouts from Health Connect (read only): today's steps vs your goal, workouts recorded by Mi Fitness,
  Google Fit, Samsung Health, Strava… with their measured calories. Optionally added to the calorie budget
  (Plus → Pas et séances): the day's active calories from Health Connect when sport and calorie access are granted, otherwise an
  estimate = steps beyond 5,000 + workouts other than walking/running (their steps already count).
  The Pas card has a refresh button; it also refreshes each time you come back to the app.
- Physical activity: walking, running, cycling, swimming, strength training… with the duration (calories estimated
  from MET values and your weight, net of the resting energy already in your maintenance) or any activity with its kcal; burned calories are added to the day's budget.
- History (chart icon in the journal):
  - Calendar (default): one month at a time, a colored dot per daily goal (calories under the budget, weight stable
    or down, sport calories, steps, water, day rated in "Ma journée" in sunny yellow). Filled = achieved, ring = missed, nothing = no data that day (an unlogged
    day never counts as a success). Tap a day for its details; current and best streak per goal.
  - Charts: calories, sport, steps and water vs their goal over 7 or 30 days, averages, average macros and the
    weight loss your logged calories should produce.
  - Goals: steps and sport in Plus → Pas et sport, water in the profile, calories from the current goal.
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
  optional cooked weight; nutrition per part and per 100 g; log "1,5 part" or grams in the journal. The list shows
  how many times each recipe was logged: favorites first, then the most used.
- Body measurements (waist, hips, chest, arm, thigh), from the Poids hub: history and one evolution chart per
  measurement (3 months, 6 months, 1 year or everything).
  More than 6 months without measurements shrink to a short "…" break on the charts. Tap a point to see its date,
  value and change since the previous measurement.
- Progress photos (Poids -> Photos), only taken in the app and stored privately on the phone (never in the gallery or
  Google backups), unlocked with fingerprint / face / PIN each time, no screenshots:
  - Camera: Face / Profil / Dos, front or back camera, the last photo of the pose shown at 30 % to line up, timer
    3 or 10 s; then a note, the day's weight and measurements. One photo per day and pose (a new one replaces it).
  - Timeline: the weight curve with a point per photo day; tap a day, pick photo A and photo B. More than 6 months
    without a photo shrink to a short "…" break, so years of history stay compact.
  - Comparison: side by side, or B over A with an opacity slider; each photo zooms and moves on its own.
  - Reminder on the 1st of each month (Plus -> Rappels, can be turned off).
- Each journal card with a goal (calories, steps, sport, water) has a settings button opening the screen where
  that goal is set.
- Reminders: weekly weigh-in and daily meal logging (only when nothing was logged that day), monthly photos.
- Daily encouragement (on by default, 19:00, own notification channel): one kind message chosen from your data:
  a lower BMI category, a new kilo lost, a new low; a streak still open today with what is left; goals beaten today
  or yesterday; comfort after a harder day or a few quiet days; else a gentle word or a journal prompt.
- Backup (Plus → Sauvegarde): send everything to the Nutrimo server on your PC (now, or automatically every day
  on Wi-Fi) and restore the latest server backup (data only, never the photos); or export a ZIP file with the data and
  the photos (named with date, pose and weight) and import its data (or an older JSON export).
- Server: set the Nutrimo API address (e.g. `http://192.168.1.20:3000`) and test the connection (Plus → Serveur).

## Launcher icon, splash and tour

A happy flower whose petals are the habit colors, celebrating its checked goals, on the app's dark green (with a
monochrome version for themed icons). At launch it blooms on the splash screen: the stem grows, the petals spin open,
the goals get checked and confetti bursts. The first launch shows a 5-page animated tour (calories, body, habits, streaks; it moves on by itself) before
the profile; Plus → Développeur → "Revoir la présentation" replays it. Icon and splash are generated by `python tools/launcher_icon.py app/src/main/res preview.html`.

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

Release build (no debug tools, faster):

```
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release is signed with the debug key (`~/.android/debug.keystore`): it installs over a debug build without
uninstalling, so the data on the phone is kept. Keep that key: another PC's key would force an uninstall (data loss;
export a backup first).

## Code

Architecture, conventions and the shared UI kit (`ui/common`: layout, rows, inputs, dialogs, charts; `util/Format`:
French numbers and dates) are described in `CLAUDE.md`. Unit tests for the pure logic (encouragement, trend, BMI,
reminder times, weight chart periods, formatting): `./gradlew :app:testDebugUnitTest`.
All the interface text is in `app/src/main/res/values/strings.xml`: translating the app means adding a
`values-<lang>/strings.xml` (see the text rules in `CLAUDE.md`).

## Data sources and licences

- CIQUAL 2020, ANSES (Licence Ouverte / Etalab) — bundled in `app/src/main/assets/ciqual_foods.json`.
- Open Food Facts (Open Database License) — via the Nutrimo API or directly from openfoodfacts.org.
