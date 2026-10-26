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
- 🚧 Journal, food search, recipes, settings — see `docs/PLAN.md` for the full scope.

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
