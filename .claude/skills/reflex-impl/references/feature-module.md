# A new feature

A feature is two modules: `feature:<name>` (pure JVM: routes, navigation port, ViewModels, nodes, Koin module) and
`feature:<name>-ui` (Android: Compose views and strings). `<pkg>` is `androidshowcase.reflex`, `<pkgpath>` its path.

## New files

```text
feature/<name>/build.gradle.kts
feature/<name>/src/main/kotlin/<pkgpath>/feature/<name>/<Name>Routes.kt         routes + <Name>Navigation port
feature/<name>/src/main/kotlin/<pkgpath>/feature/<name>/<Name>Module.kt         fun <name>Module() = module { }
feature/<name>/src/main/kotlin/<pkgpath>/feature/<name>/<Screen>ViewModel.kt     one per screen
feature/<name>/src/test/kotlin/<pkgpath>/feature/<name>/<Screen>ViewModelTest.kt
feature/<name>-ui/build.gradle.kts
feature/<name>-ui/src/main/kotlin/<pkgpath>/feature/<name>/ui/<Screen>Screen.kt
feature/<name>-ui/src/main/res/values/strings.xml
feature/<name>-ui/src/main/res/values-vi/strings.xml
cli/src/test/kotlin/<pkgpath>/cli/behavior/<Name>BehaviorTest.kt
```

`feature/<name>/build.gradle.kts`:

```kotlin
// Core side of the <name> feature: ViewModels, routes, navigation port; KSP generates the introspection nodes. Pure JVM.
plugins {
  alias(libs.plugins.reflex.jvm.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

dependencies {
  api(projects.core.domain)
  api(projects.core.viewmodel)
  api(projects.core.introspection)
  ksp(projects.core.introspectionProcessor)
}
```

`feature/<name>-ui/build.gradle.kts`:

```kotlin
// Mobile side of <name>: Compose views bound to the Core ViewModels of :feature:<name>.
plugins {
  alias(libs.plugins.reflex.android.library)
  alias(libs.plugins.reflex.compose)
}

android {
  namespace = "androidshowcase.reflex.feature.<name>.ui"
}

dependencies {
  api(projects.feature.<name>)
  implementation(projects.core.designsystem)
  implementation(libs.androidx.lifecycle.runtime.compose)
}
```

Add Coil (`libs.bundles.coil`) or other libraries only when the views use them. A feature module never depends on
another feature module.

`<Name>Module.kt`:

```kotlin
/** The <name> screens. The shell loads this module and implements [<Name>Navigation]. */
fun <name>Module(): Module = module {
  screen<FooRoute> { FooViewModel(get(), get(), get(UiScope)) }
}
```

A function, never a `val` (AGENTS.md explains why).

## Edits to existing files

| File | Edit |
| --- | --- |
| `settings.gradle.kts` | `include(":feature:<name>")` in the Core block, `include(":feature:<name>-ui")` in the Mobile block |
| `shell/build.gradle.kts` | `api(projects.feature.<name>)` |
| `app/build.gradle.kts` | `implementation(projects.feature.<name>Ui)` (type-safe accessor camel-cases `-ui`) |
| `shell/.../ReflexApp.kt` | append `<name>Module()` to `modules(...)`; implement `<Name>Navigation` and bind it in `shellModule()` (navigation.md) |
| `app/.../ui/ReflexHost.kt` | one `ScreenContent` (or `HomeTabs`) branch per ViewModel |
| `shell/.../DataModule.kt` | bind any new repository or port (data.md) |

`config/compose/compose_compiler_config.conf` already marks `<pkg>.feature.**.State` and `<pkg>.domain.**` stable.
A state class outside those packages needs its own line, and must stay immutable.

## Checks

- `./gradlew :feature:<name>:test` compiles the Core module with no Android on the classpath.
- `./gradlew :app:assembleDebug` compiles the `-ui` module and the host.
- `./gradlew :cli:test`: `WiringTest` lists any `@Introspected` ViewModel missing from Koin or `ReflexHost`. Without
  the `ksp(...)` line the nodes are not generated and the first behavior test fails with "No introspection node".
