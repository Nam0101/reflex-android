package androidshowcase.reflex.cli.behavior

import androidshowcase.reflex.introspection.introspectionRequest
import androidshowcase.reflex.introspection.introspectionWaitRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.opentest4j.AssertionFailedError

/**
 * Test SDK over the introspection protocol: it sees only what the UI sees (screen name, state,
 * actions). [precondition] and [act] wait for a few seconds (the protocol's `wait`), then fail with the last
 * observed screen and the journey so far. No sleeps, no "wait for element".
 *
 * Expected state is a JSON subset: objects match when every expected key matches, arrays when they
 * have the same size and every element matches.
 */
class ReflexDriver(private val send: (String) -> String, private val timeout: Duration = 3.seconds) {
  private val journey = mutableListOf<String>()

  fun ls(): JsonObject = request(introspectionRequest("ls"))

  /** Waits until the focused screen is [screen] and its state contains [state]. */
  fun precondition(screen: String, state: String = "{}") {
    journey += "precondition $screen $state"
    await(screen, state)
  }

  /** Does [action] like a user would, then waits for [expect] (default: the same screen) with [state]. */
  fun act(action: String, input: String? = null, expect: String? = null, state: String = "{}") {
    journey += "act $action ${input.orEmpty()}".trim()
    val screen = expect ?: ls().name
    val response = perform(action, input)
    if (!response.flag("ok")) fail("action refused: $response", ls())
    if (response.flag("ignored")) fail("the app ignored '$action': use ignored() if that is the behavior", ls())
    await(screen, state)
  }

  /** Expects the app to accept [action] and do nothing: the wrong moment for it (loading, nothing selected). */
  fun ignored(action: String, input: String? = null) {
    journey += "ignored $action ${input.orEmpty()}".trim()
    val response = perform(action, input)
    if (!response.flag("ok")) fail("action refused: $response", ls())
    if (!response.flag("ignored")) fail("expected the app to ignore '$action'", ls())
  }

  /** Expects [action] to be refused by the app (unavailable, or invalid input). */
  fun refused(action: String, input: String? = null) {
    journey += "refused $action ${input.orEmpty()}".trim()
    if (perform(action, input).flag("ok")) fail("expected '$action' to be refused", ls())
  }

  private fun perform(action: String, input: String?) =
    request(introspectionRequest("do", action, input?.let(Json::parseToJsonElement)))

  private fun await(screen: String, state: String) {
    val observed = request(introspectionWaitRequest(screen, Json.parseToJsonElement(state), timeout))
    if (!observed.flag("ok")) fail("expected screen $screen with state ⊇ $state", observed)
  }

  private fun request(line: String): JsonObject = Json.parseToJsonElement(send(line)).jsonObject

  private fun fail(reason: String, observed: JsonObject): Nothing = throw AssertionFailedError(
    buildString {
      appendLine("Step ${journey.size} failed: ${journey.last()}")
      appendLine("  $reason")
      appendLine("  observed: ${observed.name} ${observed["state"]}")
      val actions = (observed["actions"] as? JsonArray)?.map { (it as? JsonObject)?.get("name") ?: it }
      appendLine("  actions:  $actions")
      appendLine("  journey:")
      journey.forEachIndexed { i, step -> appendLine("    ${i + 1}. $step") }
    },
  )

  private val JsonObject.name get() = getValue("name").jsonPrimitive.content

  private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.boolean == true
}
