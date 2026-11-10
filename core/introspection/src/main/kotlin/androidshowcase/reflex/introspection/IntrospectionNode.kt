package androidshowcase.reflex.introspection

import androidshowcase.reflex.viewmodel.ReflexViewModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * A ViewModel (or navigator) as seen from outside the app. Three rules:
 * 1. If a user can see it, it is in [state]. If a user can do it, it is in [actions].
 * 2. No UI logic or conditionals here: business rules live in the ViewModel's `update`.
 * 3. Actions only send a ViewModel `Action`: a driver can do nothing a View could not.
 */
interface IntrospectionNode {
  /** Stable screen identifier; tests use it to check where they are. */
  val name: String

  val state: JsonElement

  val actions: List<IntrospectionAction>

  /** Actions a container lends to its focused descendant (a stack lends goBack, tabs lend selectTab). */
  val lentActions: List<IntrospectionAction> get() = emptyList()

  /** The next node towards the focused (deepest) screen. */
  val child: IntrospectionNode? get() = null
}

class IntrospectionAction internal constructor(
  val name: String,
  val title: String,
  /** JSON schema of the input, or null for an action without input. */
  val inputSchema: JsonObject?,
  private val perform: (JsonElement?) -> Boolean,
) {
  /**
   * Validates [input] first: a malformed payload throws [IntrospectionException] and never reaches the ViewModel.
   * Returns false when the app ignored the action (the ViewModel's `update` changed nothing and ran nothing).
   */
  operator fun invoke(input: JsonElement?): Boolean = perform(input)
}

class IntrospectionException(message: String) : RuntimeException(message)

/** Encodes state with every field present (defaults included), so tests see all user-visible values. */
val IntrospectionJson: Json = Json {
  encodeDefaults = true
  explicitNulls = true
}

/** An action of a container (a navigator, the app root). ViewModel actions come from [ViewModelIntrospectionNode]. */
fun action(name: String, title: String, perform: () -> Unit): IntrospectionAction =
  IntrospectionAction(name, title, inputSchema = null) { input ->
    requireNoInput(name, input)
    perform()
    true
  }

fun <I> action(name: String, title: String, input: KSerializer<I>, perform: (I) -> Unit): IntrospectionAction =
  IntrospectionAction(name, title, input.descriptor.toJsonSchema()) { json ->
    perform(decodeInput(name, input, json))
    true
  }

private fun requireNoInput(name: String, input: JsonElement?) {
  if (input != null && input != JsonNull && input != JsonObject(emptyMap())) {
    throw IntrospectionException("'$name' takes no input")
  }
}

private fun <I> decodeInput(name: String, input: KSerializer<I>, json: JsonElement?): I = try {
  // The default Json is strict: unknown keys, missing required fields and wrong types are rejected.
  Json.decodeFromJsonElement(input, json ?: JsonObject(emptyMap()))
} catch (e: IllegalArgumentException) {
  throw IntrospectionException("Invalid input for '$name': ${e.message?.lineSequence()?.first()}")
}

/**
 * Base for the node of each ViewModel (`FooViewModel` → `FooViewModelIntrospectionNode`), generated from
 * [Introspected]. It can read the state and send actions; nothing else of the ViewModel is reachable from here.
 */
abstract class ViewModelIntrospectionNode<State : Any, Action : Any>(
  private val viewModel: ReflexViewModel<State, Action, *>,
  private val stateSerializer: KSerializer<State>,
) : IntrospectionNode {
  final override val state: JsonElement
    get() = IntrospectionJson.encodeToJsonElement(stateSerializer, viewModel.state.value)

  /** An action without input: sends [toSend]. */
  protected fun action(name: String, title: String, toSend: Action): IntrospectionAction =
    IntrospectionAction(name, title, inputSchema = null) { input ->
      requireNoInput(name, input)
      viewModel.send(toSend)
    }

  /** An action with input: the decoded input is the action that is sent. */
  protected fun <A : Action> action(name: String, title: String, input: KSerializer<A>): IntrospectionAction =
    IntrospectionAction(name, title, input.descriptor.toJsonSchema()) { json ->
      viewModel.send(decodeInput(name, input, json))
    }
}
