package androidshowcase.reflex.shell

import androidshowcase.reflex.navigation.Route
import androidshowcase.reflex.navigation.RouteType
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * A [HomeStack] as text the platform keeps across process death. Each instance stamps what it writes, so it
 * can tell its own snapshots (the screens are still alive) from those of a process that was killed.
 */
internal class NavigationSnapshots(routeTypes: () -> List<RouteType<*>>) {
  @Serializable
  private class Snapshot(val writer: String, val routes: List<Route>, val tab: Int?)

  private val id = UUID.randomUUID().toString()

  /** Reads and writes routes of every type registered with `screen<Route> { }`. */
  private val json by lazy {
    Json {
      serializersModule = SerializersModule {
        polymorphic(Route::class) { routeTypes().forEach { it.registerIn(this) } }
      }
    }
  }

  fun write(stack: HomeStack): String =
    json.encodeToString(Snapshot.serializer(), Snapshot(id, stack.routes, stack.tab))

  /** Null for a snapshot this instance wrote, and for one it cannot read (written by another app version). */
  fun read(saved: String): HomeStack? {
    val snapshot = try {
      json.decodeFromString(Snapshot.serializer(), saved)
    } catch (_: IllegalArgumentException) {
      return null
    }
    return if (snapshot.writer == id) null else HomeStack(snapshot.routes, snapshot.tab)
  }
}
