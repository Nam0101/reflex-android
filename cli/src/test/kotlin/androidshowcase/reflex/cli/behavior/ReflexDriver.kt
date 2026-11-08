package androidshowcase.reflex.cli.behavior

import androidshowcase.reflex.introspection.introspectionRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.opentest4j.AssertionFailedError

/**
 * Test SDK over the introspection protocol: it sees only what the UI sees (screen name, state,
 * actions). [precondition] and [act] poll for a few seconds, then fail with the last observed screen
 * and the journey so far. No sleeps, no "wait for element".
 *
 * Expected state is a JSON subset: objects match when every expected key matches, arrays when they
 * have the same size and every element matches.
 */
class ReflexDriver(private val send: (String) -> String, private val timeout: Duration = 3.seconds) {
  private val journey = mutableListOf<String>()

  fun ls(): JsonObject = Json.parseToJsonElement(send(introspectionRequest("ls"))).jsonObject

  /** Waits until the focused screen is [screen] and its state contains [state]. */
  fun precondition(screen: String, state: String = "{}") {
    journey += "precondition $screen $state"
    await(screen, state)
  }

  /** Does [action] like a user would, then waits for [expect] (default: the same screen) with [state]. */
  fun act(action: String, input: String? = null, expect: String? = null, state: String = "{}") {
    journey += "act $action ${input.orEmpty()}".trim()
    val screen = expect ?: ls().name
    val response = Json.parseToJsonElement(
      send(introspectionRequest("do", action, input?.let(Json::parseToJsonElement))),
    ).jsonObject
    if (!response.getValue("ok").jsonPrimitive.boolean) fail("action refused: $response", ls())
    await(screen, state)
  }

  /** Expects [action] to be refused by the app (unavailable, or invalid input). */
  fun refused(action: String, input: String? = null) {
    journey += "refused $action ${input.orEmpty()}".trim()
    val response = Json.parseToJsonElement(
      send(introspectionRequest("do", action, input?.let(Json::parseToJsonElement))),
    ).jsonObject
    if (response.getValue("ok").jsonPrimitive.boolean) fail("expected '$action' to be refused", ls())
  }

  private fun await(screen: String, state: String) {
    val expected = Json.parseToJsonElement(state)
    val deadline = TimeSource.Monotonic.markNow() + timeout
    var observed = ls()
    while (!(observed.name == screen && observed.getValue("state").matches(expected))) {
      if (deadline.hasPassedNow()) fail("expected screen $screen with state ⊇ $state", observed)
      Thread.sleep(POLL_MS)
      observed = ls()
    }
  }

  private fun fail(reason: String, observed: JsonObject): Nothing = throw AssertionFailedError(
    buildString {
      appendLine("Step ${journey.size} failed: ${journey.last()}")
      appendLine("  $reason")
      appendLine("  observed: ${observed.name} ${observed["state"]}")
      val actions = (observed["actions"] as? JsonArray)?.map { it.jsonObject["name"]?.jsonPrimitive?.content }
      appendLine("  actions:  $actions")
      appendLine("  journey:")
      journey.forEachIndexed { i, step -> appendLine("    ${i + 1}. $step") }
    },
  )

  private val JsonObject.name get() = getValue("name").jsonPrimitive.content

  private companion object {
    const val POLL_MS = 5L
  }
}

internal fun JsonElement.matches(expected: JsonElement): Boolean = when (expected) {
  is JsonObject -> this is JsonObject && expected.all { (key, value) -> this[key]?.matches(value) == true }

  is JsonArray ->
    this is JsonArray &&
      size == expected.size &&
      zip(expected).all { (actual, wanted) -> actual.matches(wanted) }

  is JsonPrimitive -> this == expected
}
