package androidshowcase.reflex.navigation

import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.TypeQualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

/** The scope ViewModels run their effects on. The shell binds it to `Platform.uiScope`. */
val UiScope: Qualifier = named("uiScope")

/**
 * Registers the screen for routes of type [R]. Features call it in their Koin module; [create] receives the
 * route being opened and resolves everything else with `get()`.
 *
 * Declare those modules as functions (`fun pokedexModule() = module { }`), not `val`s: a Koin module holds the
 * instances of its singles, so two apps loading the same `val` would share them.
 */
inline fun <reified R : Route> Module.screen(noinline create: Scope.(R) -> ScreenModel) {
  factory<ScreenModel>(TypeQualifier(R::class)) { params -> create(params.get()) }
}

/** Creates the screen registered for [route] with [screen]. */
fun Koin.createScreen(route: Route): ScreenModel = get(TypeQualifier(route::class)) { parametersOf(route) }
