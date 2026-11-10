package androidshowcase.reflex.introspection

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * True when this element contains [expected]: objects match when every expected key matches, arrays when they
 * have the same size and every element matches, primitives when they are equal.
 */
fun JsonElement.containsSubset(expected: JsonElement): Boolean = when (expected) {
  is JsonObject -> this is JsonObject && expected.all { (key, value) -> this[key]?.containsSubset(value) == true }

  is JsonArray ->
    this is JsonArray &&
      size == expected.size &&
      zip(expected).all { (actual, wanted) -> actual.containsSubset(wanted) }

  is JsonPrimitive -> this == expected
}
