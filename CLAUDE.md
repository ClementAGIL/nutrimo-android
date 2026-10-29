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
Jetpack Compose + Material 3 (fixed green brand palette, light/dark), Navigation Compose with type-safe routes,
Room (KSP), DataStore Preferences, OkHttp + kotlinx.serialization, CameraX + ML Kit barcode, WorkManager, Health Connect client.
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
                                  FoodEntities (foods, portions, recipes, ingredients, diary, saved meals + items), DAOs, Converters.
                                  Schema changes: bump the version and add an AutoMigration (schemas in app/schemas)
  network/                        Http (OkHttp wrapper), FoodDto (+ mapping to entities), FoodApis
                                  (NutrimoApi = PC backend, OffApi = Open Food Facts + mapOffProduct)
  Errors.kt                       nullOnError (failures of network / Health Connect → null, cancellation kept)
  health/                         HealthRepository (read-only Health Connect: status, permissions, steps per day,
                                  exercise sessions + active kcal), HealthDay (HealthInfo + day(date, kg)), ImportedActivity (HC exercise type → label, MET
                                  estimate, walks/runs excluded from the budget since their steps count)
  sync/ServerBackup.kt            push / restore the JSON backup on nutrimo-api (/backups), AutoBackupWorker (daily,
                                  Wi-Fi only, retries when away from home)
  demo/                           DemoData (wipe + realistic history: profile, 2 goals, 5 months of weigh-ins, measurements,
                                  water, custom food, recipes incl. a sub-recipe), DemoDiary (30 days of meals, fixed CIQUAL ids)
  reminders/Reminders.kt          notification channel, WorkManager scheduling, weigh-in / meal workers (notify
                                  only when nothing was logged today)
  repository/                     Settings (DataStore), Activity (MET × weight × time, or manual kcal), SavedMeal (save a meal / log it in one tap), Body (profile/weights/goals/calorie target), Food
                                  (seed, local+remote search, barcode, cache, custom), Recipe (recursive
                                  nutrition, cycle check), Diary (entries + water), Backup (JSON export/import)
domain/
  model/                          Nutrients (scale/plus), enums with French labels, NutrientCatalog (micros),
                                  Loggable (food or recipe + unit choices → nutrients), String.normalized()
  calc/                           Energy (Mifflin-St Jeor, deficit, floor), GoalMath (pace <-> date, progress),
                                  Trend (linear regression, estimated end), Bmi, RecipeMath, Activities (MET table), Macros (MacroSplit % → grams, presets),
                                  DayGoals (DailyGoal, GoalTargets + DEFAULT_* goals, DayRecord.achieved = null without data,
                                  streaks; AppSettings.goalTargets(profile) builds the targets)
ui/
  NutrimoRoot.kt                  root: onboarding (outside the graph) or NavHost + bottom bar (4 tabs)
  navigation/                     Routes (@Serializable routes + NavResults keys), NavGraph (all destinations)
  onboarding/                     first launch: profile then first goal (also saves the first weigh-in)
  profile/                        ProfileForm (shared) + ProfileScreen
  goal/                           GoalDraft (pace preset / end date → pace + end date), GoalForm (with calorie
                                  preview and safety warnings, shared), GoalEditScreen
  more/                           "Plus" tab menu (MORE_ENTRIES), ServerScreen (URL + key + test), BackupScreen
                                  (server backup/restore + auto toggle, SAF file export/import), RemindersScreen, AboutScreen, DevScreen (load demo / erase all), MacrosScreen, StepsScreen
  journal/                        JournalScreen (day bar, LazyColumn), JournalViewModel (day → meals, totals, water,
                                  activities; budget = target + burned), ActivityCard (manual + imported sessions), StepsCard (install / connect / progress),
                                  JournalCards (summary ring + macro bars, MealCard, WaterCard; ring/text color by KcalZone:
                                  green ≤ budget, yellow ≤ maintenance = TDEE + burned, red above), EntryUi
                                  (quantityText, SwipeToDelete), DayNutrientsScreen. Every card with a goal has a
                                  GoalSettingsButton → JournalActions.onGoalSettings(route): calories → GoalEdit,
                                  steps and sport → Steps ("Pas et sport"), water → Profile
  history/                        HistoryScreen (journal chart icon): CalendarTab (month grid, one dot per goal: filled =
                                  achieved, ring = missed, none = no data; day detail; streaks) and ChartsTab (7/30-day bars
                                  vs goal for calories, sport, steps, water; averages; estimated loss = Σ(TDEE + sport −
                                  eaten)/7700). HistoryViewModel loads ≥ 180 days (streaks) + Health Connect once per range,
                                  HistoryRecords merges everything into one DayRecord per day. Past days use today's targets.
  food/                           AddFoodScreen/ViewModel (local instant, backend after 600 ms, OFF only on submit;
                                  barcode lookup), QuickTabs (recents/favorites/custom/recipes), FoodRows,
                                  ItemDetailScreen/ViewModel (diary add/edit, or recipe-ingredient pick),
                                  CustomFoodScreen/ViewModel (MACRO_FIELDS, micros, portions), ScannerScreen
                                  (CameraX + ML Kit)
  recipe/                         RecipesScreen (favorites first, then by use count = diary entries, counted live in SQL; kcal/part), RecipeViewScreen (per part / 100 g, delete warns
                                  about parent recipes), RecipeEditScreen/ViewModel (ingredients picked through
                                  AddFood(forRecipe) → ItemDetail → NavResults.INGREDIENT; cycle check)
  weight/                         WeightScreen (Poids / Mensurations switch, each with sub-tabs; FAB adds to the shown one),
                                  Measurements (MEASUREMENT_FIELDS shared by list, dialog and charts), MeasurementCharts
                                  (one line chart per measurement, reuses windowOf), WeightViewModel (progressOf), WeightDashboard,
                                  WeightHistory, WeightDialog (one per day, confirm replace), WeightChartTab (series)
  common/                         Shared UI, split by role (look here before writing a new widget):
                                  Layout (ScreenScaffold, ScrollColumn = standard scrollable body, SectionCard, CardHeader,
                                  AppTabs, EmptyState), Rows (StatRow, ProgressRow, ProgressBar, SwitchRow, GoalSettingsButton),
                                  Inputs (NumberField, ChoiceChips, WideButton), Texts (IntroText, HintText, WarningText),
                                  Dialogs (FormDialog = confirm + Annuler, ConfirmDialog, TextDialog), StatusRunner (action →
                                  status message / busy), Pickers (DateField, Dropdown), Format (fr numbers/dates, dateTime,
                                  signed), ViewModels (appViewModel, Flow.stateWhileSubscribed), NutrientViews (MacroBars, NutrientTable)
  common/chart/                   LineChart (generic Canvas date/value chart: lines, dots, daily bars), ChartPeriods
                                  (ChartPeriod + PeriodChips, windowOf, ChartLegend); used by weight, measurements, history
  theme/Theme.kt                  brand color schemes (cards = surface, page = background), shapes, bold titles + fixed chart colors
                                  (SectionCard: white, 20 dp corners, 1 dp elevation; AppTabs / top bar use the page color)
```

## Key conventions

- Food ids: `ciqual:<code>`, `off:<barcode>`, `custom:<uuid>` — build/test them with `FoodSource.id()` / `owns()`; nutrients per 100 g. `FoodDto` mirrors the API's
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
- Launcher icon (happy flower, dark green): `res/drawable/ic_launcher_foreground.xml` + `ic_launcher_monochrome.xml`
  are generated by `python tools/launcher_icon.py app/src/main/res <preview.html>`; edit the script, not the XML.
- Line endings are LF (`.gitattributes`); write files with `
` only.
- Every commit updates this file (architecture/status) and `README.md`.

## Status

- ✅ Gradle setup, theme, domain calculations, Room schema, network, repositories, backup format.
- ✅ Navigation shell, onboarding, profile, new goal, goals list.
- ✅ Weight tab: dashboard, history, graph.
- ✅ Journal, food search (local / PC / OFF), barcode scan, quantity screen, custom foods, day nutrients.
- ✅ Recipes: list, detail, editor with sub-recipes and cooked weight, log parts in the journal.
- ✅ Steps and workouts from Health Connect (read-only; opt-in budget: steps beyond 5,000 + non-walking sessions).
  Guava is an explicit dependency: Health Connect otherwise hides CameraX's ListenableFuture.
  Every Health Connect read permission must also be declared in the manifest, or the request silently skips it.
  The Pas card has a refresh button (data synced by other apps can arrive while the journal is open).
- ✅ Custom macro targets (Plus → Objectifs de macros), used by the journal bars.
- ✅ Physical activity (journal card; burned kcal raise the day's budget). DB v3 via AutoMigration(2 → 3).
- ✅ Saved meals (journal bookmark icon → "Repas" tab of the search). DB v2 via AutoMigration(1 → 2).
- ✅ Developer section (Plus → Développeur): demo data / erase everything. Use it to test every screen.
- ✅ History (journal top bar): goal calendar with streaks (default tab) + charts for calories, sport, steps, water.
- ✅ Server backup (manual + daily automatic) and restore.
- ✅ Plus tab: server, reminders, JSON backup, goals, about. Measurements live in the Poids tab (list + charts).
- ✅ Tested on the user's phone (Xiaomi): onboarding, journal, server search + caching, product portions, weight tab, graph.
- ⏳ Not fully exercised on a device (no emulator acceleration on the dev PC): test on the phone, then polish.
