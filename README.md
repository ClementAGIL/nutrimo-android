# Nutrimo (Android)

French weight-loss app: calorie counter, recipes, weight goal and weigh-in tracking. Everything personal is stored
on the phone. Food data comes from CIQUAL (bundled, works offline), the user's own food API
([nutrimo-api](../nutrimo-api)) on the home network, and Open Food Facts as a fallback.

## Features

- 🚧 In progress — see `docs/PLAN.md` for the full scope.

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
