package androidshowcase.reflex.introspection

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The protocol every driver speaks: one JSON request per line, one JSON response per line.
 *
 * - `{"cmd":"ls"}` → `{"ok":true,"name":…,"state":{…},"actions":[{"name":…,"title":…,"input":{schema}}]}`
 * - `{"cmd":"do","action":"selectTab","input":{"tab":"Favorites"}}` → `{"ok":true}`, with `"ignored":true` when
 *   the app accepted the action and did nothing (wrong moment: already loading, nothing selected yet).
 * - `{"cmd":"wait"}` blocks until no effect is running, `{"cmd":"wait","screen":"Pokedex","state":{…}}` until
 *   that screen is focused and its state contains `state` (see [containsSubset]). Both answer like `ls`, or with
 *   `"ok":false` and what was observed after `timeoutMs` (default 3000).
 *
 * `ls` describes the focused (deepest) node; its actions include those lent by its ancestors, the
 * nearest container first. An unavailable action is refused with the list of available ones.
 *
 * [isIdle] says whether the app has effects in flight; without it `wait` never has to wait for them.
 */
class IntrospectionSession(private val isIdle: () -> Boolean = { true }, private val root: () -> IntrospectionNode) {
  fun ls(): JsonObject {
    val (focused, available) = focus()
    return buildJsonObject {
      put("ok", true)
      put("name", focused.name)
      put("state", focused.state)
      putJsonArray("actions") {
        available.forEach { action ->
          addJsonObject {
            put("name", action.name)
            put("title", action.title)
            action.inputSchema?.let { put("input", it) }
          }
        }
      }
    }
  }

  fun perform(actionName: String, input: JsonElement?): JsonObject {
    val (_, available) = focus()
    val action = available.firstOrNull { it.name == actionName }
      ?: return failure("Action '$actionName' is not available here", available)
    return try {
      val handled = action(input)
      buildJsonObject {
        put("ok", true)
        if (!handled) put("ignored", true)
      }
    } catch (e: IntrospectionException) {
      failure(e.message.orEmpty(), available)
    }
  }

  /**
   * With a [screen], waits for it to be focused with a state containing [state]. Without one, waits until the
   * app has been idle for a short moment: effects are counted, but a value a stream delivers right after a
   * write is not, so expect a screen and state wherever the outcome matters.
   */
  fun await(screen: String?, state: JsonElement?, timeout: Duration = DEFAULT_TIMEOUT): JsonObject {
    val deadline = TimeSource.Monotonic.markNow() + timeout
    var idleSince: TimeSource.Monotonic.ValueTimeMark? = null
    while (true) {
      val observed = ls()
      val reached = if (screen == null) {
        idleSince = if (isIdle()) idleSince ?: TimeSource.Monotonic.markNow() else null
        idleSince != null && idleSince.elapsedNow() >= QUIET
      } else {
        observed["name"] == JsonPrimitive(screen) && (state == null || observed.getValue("state").containsSubset(state))
      }
      if (reached) return observed
      if (deadline.hasPassedNow()) {
        val expected = if (screen == null) "the app to be idle" else "screen $screen with state ⊇ ${state ?: "{}"}"
        return buildJsonObject {
          put("ok", false)
          put("error", "Timed out after $timeout waiting for $expected")
          put("name", observed.getValue("name"))
          put("state", observed.getValue("state"))
          put("actions", JsonArray(focus().second.map { JsonPrimitive(it.name) }))
        }
      }
      Thread.sleep(POLL.inWholeMilliseconds)
    }
  }

  /** Handles one NDJSON request line and returns the response line. Never throws for bad input. */
  fun handle(line: String): String {
    val response = try {
      val request = Json.parseToJsonElement(line).jsonObject
      when (val cmd = request["cmd"]?.jsonPrimitive?.contentOrNull) {
        "ls" -> ls()

        "do" -> {
          val name = request["action"]?.jsonPrimitive?.contentOrNull
          if (name == null) failure("'do' needs an \"action\"", focus().second) else perform(name, request["input"])
        }

        "wait" -> await(
          screen = request["screen"]?.jsonPrimitive?.contentOrNull,
          state = request["state"],
          timeout = request["timeoutMs"]?.jsonPrimitive?.longOrNull?.milliseconds ?: DEFAULT_TIMEOUT,
        )

        else -> failure("Unknown cmd '$cmd'. Use \"ls\", \"do\" or \"wait\"", focus().second)
      }
    } catch (e: IllegalArgumentException) {
      failure("Malformed request: ${e.message?.lineSequence()?.first()}", focus().second)
    }
    return response.toString()
  }

  private fun focus(): Pair<IntrospectionNode, List<IntrospectionAction>> {
    var node = root()
    val lent = ArrayList<IntrospectionAction>()
    while (true) {
      val child = node.child ?: break
      lent.addAll(0, node.lentActions) // nearer containers go first
      node = child
    }
    return node to (node.actions + lent).distinctBy { it.name }
  }

  private fun failure(message: String, available: List<IntrospectionAction>) = buildJsonObject {
    put("ok", false)
    put("error", message)
    put("actions", JsonArray(available.map { JsonPrimitive(it.name) }))
  }

  private companion object {
    val DEFAULT_TIMEOUT = 3000.milliseconds
    val POLL = 5.milliseconds
    val QUIET = 50.milliseconds
  }
}

/** Builds a request line, for drivers written in Kotlin. */
fun introspectionRequest(cmd: String, action: String? = null, input: JsonElement? = null): String = buildJsonObject {
  put("cmd", cmd)
  action?.let { put("action", it) }
  input?.let { put("input", it) }
}.toString()

/** Builds a `wait` request line: for [screen] with [state], or for the app to be idle when [screen] is null. */
fun introspectionWaitRequest(screen: String? = null, state: JsonElement? = null, timeout: Duration? = null): String =
  buildJsonObject {
    put("cmd", "wait")
    screen?.let { put("screen", it) }
    state?.let { put("state", it) }
    timeout?.let { put("timeoutMs", it.inWholeMilliseconds) }
  }.toString()
