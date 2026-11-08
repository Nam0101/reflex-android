# Reflex Android Base

A multi-module Android starter built on [Shopify's Reflex architecture](https://shopify.engineering/reflex) and
Clean Architecture, written with Jetpack Compose. Fork it, run `scripts/rename-package.sh`, and replace the
Pokédex sample with your features.

Created by **NamNV**, on top of the `android-showcase` base project.

## Why Reflex

The whole app (state, business rules, navigation) lives in a pure-JVM **Core** that knows nothing about Android.
The Compose **Mobile** layer renders Core state and sends user actions back. Because Core is plain Kotlin, the
same app also runs headless in a terminal, where a **CLI** and the behavior tests drive it through a small JSON
protocol, exactly as a user would, in milliseconds and with no emulator.

```text
╭──────────────────────────── Mobile · Android + Compose ─────────────────────────────╮
│  :app  (Activity, Navigation 3 host, platform services, debug introspection socket) │
│  :feature:onboarding-ui  :feature:pokedex-ui  :core:designsystem                    │
╰──────────────────────────────────────────┬──────────────────────────────────────────╯
                     renders state · sends actions
╭──────────────────────────────────────────┴─────────────── Core · pure JVM ──────────╮
│  :shell                ReflexApp: composition root, navigation, deep links          │
│  :feature:onboarding   :feature:pokedex     ViewModels, routes, introspection nodes │
│  :core:viewmodel       ReflexViewModel (update → Next(state, effects)), navigators  │
│  :core:introspection   nodes, typed actions + JSON schema, NDJSON session           │
│  :core:data            Ktor, Room 3 (bundled SQLite), DataStore                     │
│  :core:domain          models, AppError, repository ports, use cases                │
╰──────────────────────────────────────────┬──────────────────────────────────────────╯
                     ls / do  (one JSON object per line)
╭──────────────────────────────────────────┴─────────────────────── CLI · JVM ────────╮
│  :cli   headless app with HTTP fixtures · socket client for a debug build           │
│         behavior tests: precondition → act → assert state                           │
╰─────────────────────────────────────────────────────────────────────────────────────╯
```

### The pieces

- **ReflexViewModel**: `State` (immutable, serializable), `Action` (what the user can do), `Message` (actions plus
  effect results). `update(state, message)` is pure and returns the next state plus effects (`task`, `stream`,
  `Effect.Cancel`); the base class runs the effects and feeds their results back as messages.
- **Navigation is state**: `StackNavigator` and `TabNavigator` own the screens (route + model). Compose draws them
  with Navigation 3 `NavDisplay`, which adds transitions and predictive back.
- **Introspection**: each screen exposes a node with its name, state as JSON, and actions with typed inputs
  (JSON schema from the `KSerializer`). Containers lend actions to the focused screen (`goBack`, `selectTab`).
- **Manual DI**: `Platform` (what Android provides) → `AppGraph` (data) → `ReflexApp` (screens). No framework.
- **Errors as values**: repositories return `Result<T, AppError>`; screens hold `AppError?`.

## Sample app

Language picker (English / Tiếng Việt, per-app locale) → onboarding pager → Pokédex from
[PokeAPI](https://pokeapi.co) with paging, detail with stats, favorites in Room, and `reflex://pokemon/{id}`
deep links. The first screen is decided by Core (`ResolveLaunchStep`) behind the system splash screen.

## Tech stack

Versions live in `gradle/libs.versions.toml`.

- Kotlin, JVM 21, Gradle Kotlin DSL with `build-logic` convention plugins and type-safe project accessors
- Jetpack Compose (BOM), Material 3, Navigation 3, Lifecycle Compose, Core SplashScreen, AppCompat per-app locales
- Ktor Client (OkHttp), kotlinx.serialization, kotlinx.coroutines, kotlin-result
- Room 3 with the bundled SQLite driver, DataStore Preferences, Coil 3
- JUnit 6, Ktor MockEngine, Baseline Profiles + Macrobenchmark, Spotless + ktlint
- Min SDK 26, target/compile SDK 37

## Getting started

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug
./gradlew test                   # unit + behavior tests
./gradlew spotlessApply          # format
./gradlew assembleRelease        # R8; signed with keystore.properties, or the debug key when it is absent
```

Release signing reads `keystore.properties` at the root (git-ignored):

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## CLI

```bash
./gradlew :cli:installDist
cli/build/install/reflex/bin/reflex headless
ls
do select {"language":"English"}
do continue
do finish
do open {"id":25}
```

Headless runs use the HTTP fixtures in `cli/src/main/resources/fixtures` and a temporary data folder. Any
request without a fixture is reported.

`do` returns once the action is dispatched; effects (disk, network, navigation) finish asynchronously, so a
script reads the result with `ls` (the behavior tests poll for it).

Against a debug build on a device or emulator (the debug app serves the protocol on an abstract socket):

```bash
adb forward tcp:7171 localabstract:androidshowcase.reflex.introspection
cli/build/install/reflex/bin/reflex ls
cli/build/install/reflex/bin/reflex do toggleFavorite
```

The views update live while the CLI drives the app, so an agent or a script can operate the real UI.

## Behavior tests

`cli/src/test/.../behavior` holds grey-box journeys written against the protocol:

```kotlin
app.precondition("Pokedex", """{"error":null,"nextOffset":20}""")
app.act("open", """{"id":25}""", expect = "PokemonDetail", state = """{"isLoading":false}""")
app.act("toggleFavorite", state = """{"isFavorite":true}""")
app.refused("selectTab", """{"tab":"Settings"}""")
session.relaunch()
```

State is matched as a JSON subset, polled until it holds (3 s timeout). A failure prints the step, the expected
subset, the observed screen and state, the available actions, and the journey so far. Tests run against real
Room and DataStore files, with zero network.

## Starting a new project

```bash
scripts/rename-package.sh com.example.myapp
./gradlew clean test assembleDebug
```

The script moves every source tree, the Room schema folder, and rewrites the package in Kotlin, Gradle, XML,
ProGuard, properties, Markdown and Compose config files. Rename `rootProject.name`, `app_name`, `Theme.Reflex`
and the launcher icons by hand. AGENTS.md describes how to add a screen.

## Baseline Profile

```bash
./gradlew :app:generateReleaseBaselineProfile                 # needs a connected device or emulator
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest     # cold startup benchmark
```
