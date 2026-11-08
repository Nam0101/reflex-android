package androidshowcase.reflex.introspection

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
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * The protocol every driver speaks: one JSON request per line, one JSON response per line.
 *
 * - `{"cmd":"ls"}` → `{"ok":true,"name":…,"state":{…},"actions":[{"name":…,"title":…,"input":{schema}}]}`
 * - `{"cmd":"do","action":"selectTab","input":{"tab":"Favorites"}}` → `{"ok":true}`
 *
 * `ls` describes the focused (deepest) node; its actions include those lent by its ancestors, the
 * nearest container first. An unavailable action is refused with the list of available ones.
 */
class IntrospectionSession(private val root: () -> IntrospectionNode) {
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
      action(input)
      buildJsonObject { put("ok", true) }
    } catch (e: IntrospectionException) {
      failure(e.message.orEmpty(), available)
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

        else -> failure("Unknown cmd '$cmd'. Use \"ls\" or \"do\"", focus().second)
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
}

/** Builds a request line, for drivers written in Kotlin. */
fun introspectionRequest(cmd: String, action: String? = null, input: JsonElement? = null): String = buildJsonObject {
  put("cmd", cmd)
  action?.let { put("action", it) }
  input?.let { put("input", it) }
}.toString()
