package androidshowcase.reflex.introspection

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** A JSON schema for an action input, derived from its serializer (what Reflex's `@IntrospectionInput` generates). */
internal fun SerialDescriptor.toJsonSchema(): JsonObject = buildJsonObject {
  when (kind) {
    PrimitiveKind.STRING, PrimitiveKind.CHAR -> put("type", "string")

    PrimitiveKind.BOOLEAN -> put("type", "boolean")

    PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> put("type", "integer")

    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> put("type", "number")

    SerialKind.ENUM -> {
      put("type", "string")
      putJsonArray("enum") { elementNames.forEach { add(it) } }
    }

    StructureKind.LIST -> {
      put("type", "array")
      put("items", getElementDescriptor(0).toJsonSchema())
    }

    StructureKind.CLASS, StructureKind.OBJECT -> {
      put("type", "object")
      putJsonObject("properties") {
        repeat(elementsCount) { put(getElementName(it), getElementDescriptor(it).toJsonSchema()) }
      }
      putJsonArray("required") {
        repeat(elementsCount) { if (!isElementOptional(it)) add(getElementName(it)) }
      }
      put("additionalProperties", false)
    }

    else -> Unit // Maps, polymorphic and contextual inputs are left unconstrained.
  }
}
