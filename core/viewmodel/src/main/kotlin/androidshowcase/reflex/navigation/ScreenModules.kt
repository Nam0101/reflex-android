package androidshowcase.reflex.navigation

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.serializer
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
 * route being opened and resolves everything else with `get()`. The route's serializer is registered with it,
 * so the screen can be reopened after the process is killed.
 *
 * Declare those modules as functions (`fun pokedexModule() = module { }`), not `val`s: a Koin module holds the
 * instances of its singles, so two apps loading the same `val` would share them.
 */
inline fun <reified R : Route> Module.screen(noinline create: Scope.(R) -> ScreenModel) {
  // Resolved now, not on first use: a route that is not @Serializable fails at startup.
  val routeType = RouteType(R::class, serializer<R>())
  factory<ScreenModel>(TypeQualifier(R::class)) { params -> create(params.get()) }
  single(TypeQualifier(R::class)) { routeType }
}

/** A route type registered with [screen], with the serializer that carries its routes across process death. */
class RouteType<R : Route>(private val type: KClass<R>, private val serializer: KSerializer<R>) {
  fun registerIn(routes: PolymorphicModuleBuilder<Route>) = routes.subclass(type, serializer)
}

/** Every route type registered with [screen]. */
fun Koin.routeTypes(): List<RouteType<*>> = getAll()

/** Creates the screen registered for [route] with [screen]. */
fun Koin.createScreen(route: Route): ScreenModel = get(TypeQualifier(route::class)) { parametersOf(route) }
