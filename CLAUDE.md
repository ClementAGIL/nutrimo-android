# CLAUDE.md

Guidance for Claude (and any contributor) working in this repository. `AGENTS.md` points here.

## What this project is

**Nutrimo**, a French weight-loss Android app: calorie counter (foods from CIQUAL / Open Food Facts / custom),
recipes with per-part nutrition, weight goal with a recommended calorie target, weigh-ins with dashboard, history
and graph. All personal data stays on the phone (Room). Food search also queries the user's own API running on
their PC (`../nutrimo-api`), and falls back to the public Open Food Facts API.
The full product plan is in `docs/PLAN.md`. UI text is **French only**.

## Stack

Kotlin 2.1, AGP 8.12, Gradle 8.14 (JDK 17), compileSdk/targetSdk 36, **minSdk 31**.
Jetpack Compose + Material 3 (dynamic colors, light/dark), Navigation Compose with type-safe routes,
Room (KSP), DataStore Preferences, OkHttp + kotlinx.serialization, CameraX + ML Kit barcode, WorkManager.
Material 3 experimental APIs are opted in globally (app/build.gradle.kts).
Deliberate choices: **manual DI** (`data/AppContainer.kt`, no Hilt), **no Retrofit** (the server URL is set at
runtime; `data/network/Http.kt` is enough), **no chart library** (the weight graph is drawn on a Compose Canvas).
No unit tests for now (decided with the user).

## Commands

| Command                         | What it does                                                |
| ------------------------------- | ----------------------------------------------------------- |
| `./gradlew assembleDebug`       | Build `app/build/outputs/apk/debug/app-debug.apk`           |
| `./gradlew installDebug`        | Build and install on the connected phone (adb)              |
| `./gradlew lint`                | Android lint                                                |

`local.properties` must contain `sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk` (forward slashes).

## Architecture (`app/src/main/java/com/clement/nutrimo/`)

```
NutrimoApp.kt / MainActivity.kt   Application (holds AppContainer) / single activity
data/
  AppContainer.kt                 creates db, Http, repositories; seeds CIQUAL on first launch
  db/                             Room: BodyEntities (profile, weights, measurements, goals, water),
                                  FoodEntities (foods, portions, recipes, ingredients, diary), DAOs, Converters
  network/                        Http (OkHttp wrapper), FoodDto (+ mapping to entities), FoodApis
                                  (NutrimoApi = PC backend, OffApi = Open Food Facts + mapOffProduct)
  sync/ServerBackup.kt            push / restore the JSON backup on nutrimo-api (/backups), AutoBackupWorker (daily,
                                  Wi-Fi only, retries when away from home)
  reminders/Reminders.kt          notification channel, WorkManager scheduling, weigh-in / meal workers (notify
                                  only when nothing was logged today)
  repository/                     Settings (DataStore), Body (profile/weights/goals/calorie target), Food
                                  (seed, local+remote search, barcode, cache, custom), Recipe (recursive
                                  nutrition, cycle check), Diary (entries + water), Backup (JSON export/import)
domain/
  model/                          Nutrients (scale/plus), enums with French labels, NutrientCatalog (micros),
                                  Loggable (food or recipe + unit choices → nutrients), String.normalized()
  calc/                           Energy (Mifflin-St Jeor, deficit, floor), GoalMath (pace <-> date, progress),
                                  Trend (linear regression, estimated end), Bmi, RecipeMath
ui/
  NutrimoRoot.kt                  root: onboarding (outside the graph) or NavHost + bottom bar (4 tabs)
  navigation/                     Routes (@Serializable routes + NavResults keys), NavGraph (all destinations)
  onboarding/                     first launch: profile then first goal (also saves the first weigh-in)
  profile/                        ProfileForm (shared) + ProfileScreen
  goal/                           GoalDraft (pace preset / end date → pace + end date), GoalForm (with calorie
                                  preview and safety warnings, shared), GoalEditScreen
  more/                           "Plus" tab menu (MORE_ENTRIES), ServerScreen (URL + key + test), BackupScreen
                                  (server backup/restore + auto toggle, SAF file export/import), MeasurementsScreen, RemindersScreen, AboutScreen
  journal/                        JournalScreen (day bar, LazyColumn), JournalViewModel (day → meals, totals, water),
                                  JournalCards (summary ring + macro bars, MealCard, WaterCard), EntryUi
                                  (quantityText, SwipeToDelete), DayNutrientsScreen
  food/                           AddFoodScreen/ViewModel (local instant, backend after 600 ms, OFF only on submit;
                                  barcode lookup), QuickTabs (recents/favorites/custom/recipes), FoodRows,
                                  ItemDetailScreen/ViewModel (diary add/edit, or recipe-ingredient pick),
                                  CustomFoodScreen/ViewModel (MACRO_FIELDS, micros, portions), ScannerScreen
                                  (CameraX + ML Kit)
  recipe/                         RecipesScreen (list + kcal/part), RecipeViewScreen (per part / 100 g, delete warns
                                  about parent recipes), RecipeEditScreen/ViewModel (ingredients picked through
                                  AddFood(forRecipe) → ItemDetail → NavResults.INGREDIENT; cycle check)
  weight/                         WeightScreen (3 sub-tabs + FAB), WeightViewModel (progressOf), WeightDashboard,
                                  WeightHistory, WeightDialog (one per day, confirm replace), WeightChartTab
                                  (series + window) on LineChart (generic Canvas date/value chart)
  common/                         ViewModels (appViewModel), Format (fr numbers/dates), Components (ScreenScaffold,
                                  ScrollColumn = standard scrollable screen body, SectionCard(onClick), …), Pickers,
                                  NutrientViews (MacroBars with reference targets, NutrientTable with micros)
  theme/Theme.kt                  Material 3 theme + fixed chart/macro colors
```

## Key conventions

- Food ids: `ciqual:<code>`, `off:<barcode>`, `custom:<uuid>`; nutrients per 100 g. `FoodDto` mirrors the API's
  `Food` JSON and the bundled `assets/ciqual_foods.json` (generated by `npm run export:ciqual-json` in the API repo).
- Entities use UUID string ids + `updatedAt` so a future backup/sync API can merge data.
- Diary entries store a **nutrient snapshot**: editing a food or recipe never changes past days.
- One weigh-in per day (`weights.date` unique); saving on a day that has one replaces it.
- Screens: `ui/<feature>/XxxScreen.kt` + `XxxViewModel.kt` exposing `StateFlow` UI state; create VMs with
  `appViewModel { container, handle -> ... }`; read nav args with `handle.toRoute<XxxRoute>()`.
- Results between screens go through `savedStateHandle` with the keys in `NavResults`.

## Code quality rules

- Short files (< ~300 lines) and functions (< ~40 lines), one job each. Split screens into small composables.
- No copy-paste: shared UI goes to `ui/common`, shared math to `domain/calc`.
- Constants in `UPPER_SNAKE_CASE` at the top of the file. Comments explain why. No commented-out code.
- Every file starts with a one-line comment saying what it is for.
- Every commit updates this file (architecture/status) and `README.md`.

## Status

- ✅ Gradle setup, theme, domain calculations, Room schema, network, repositories, backup format.
- ✅ Navigation shell, onboarding, profile, new goal, goals list.
- ✅ Weight tab: dashboard, history, graph.
- ✅ Journal, food search (local / PC / OFF), barcode scan, quantity screen, custom foods, day nutrients.
- ✅ Recipes: list, detail, editor with sub-recipes and cooked weight, log parts in the journal.
- ✅ Server backup (manual + daily automatic) and restore.
- ✅ Plus tab: server, reminders, JSON backup, measurements, goals, about.
- ✅ Installs and launches on the user's phone (Xiaomi, Android 15+); lint clean (only dependency-update notices).
- ⏳ Not fully exercised on a device (no emulator acceleration on the dev PC): test on the phone, then polish.
