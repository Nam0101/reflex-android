package androidshowcase.reflex.introspection

import androidshowcase.reflex.viewmodel.Next
import androidshowcase.reflex.viewmodel.ReflexViewModel
import androidshowcase.reflex.viewmodel.next
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntrospectionSessionTest {
  @Serializable
  data class RenameInput(val name: String, val times: Int = 1)

  private val calls = mutableListOf<String>()

  private val leaf = object : IntrospectionNode {
    override val name = "Leaf"
    override val state: JsonElement = buildJsonObject { put("count", 3) }
    override val actions = listOf(
      action("refresh", "Refresh") { calls += "refresh" },
      action("rename", "Rename", RenameInput.serializer()) { calls += "rename:${it.name}x${it.times}" },
    )
  }

  private val container = object : IntrospectionNode {
    override val name = "Container"
    override val state: JsonElement = JsonObject(emptyMap())
    override val actions = listOf(action("hidden", "Not lent") { calls += "hidden" })
    override val lentActions = listOf(action("goBack", "Go back") { calls += "back" })
    override val child = leaf
  }

  private val session = IntrospectionSession { container }

  private fun call(line: String) = Json.parseToJsonElement(session.handle(line)).jsonObject

  @Test
  fun `ls shows the focused node with its own and lent actions`() {
    val ls = call("""{"cmd":"ls"}""")
    assertEquals("Leaf", ls["name"]!!.jsonPrimitive.content)
    assertEquals(
      listOf("refresh", "rename", "goBack"),
      ls["actions"]!!.jsonArray.map {
        it.jsonObject["name"]!!.jsonPrimitive.content
      },
    )
    val schema = ls["actions"]!!.jsonArray[1].jsonObject["input"]!!.jsonObject
    assertEquals(listOf("name"), schema["required"]!!.jsonArray.map { it.jsonPrimitive.content })
  }

  @Test
  fun `typed input is decoded, malformed input never reaches the action`() {
    assertTrue(call("""{"cmd":"do","action":"rename","input":{"name":"pika"}}""")["ok"]!!.jsonPrimitive.boolean)
    assertFalse(call("""{"cmd":"do","action":"rename","input":{"name":7,"oops":1}}""")["ok"]!!.jsonPrimitive.boolean)
    assertFalse(call("""{"cmd":"do","action":"refresh","input":{"x":1}}""")["ok"]!!.jsonPrimitive.boolean)
    assertEquals(listOf("rename:pikax1"), calls)
  }

  @Test
  fun `actions of a non-focused node are refused, malformed lines do not throw`() {
    val refused = call("""{"cmd":"do","action":"hidden"}""")
    assertFalse(refused["ok"]!!.jsonPrimitive.boolean)
    assertEquals(3, refused["actions"]!!.jsonArray.size)
    assertFalse(call("not json")["ok"]!!.jsonPrimitive.boolean)
    assertTrue(call("""{"cmd":"do","action":"goBack"}""")["ok"]!!.jsonPrimitive.boolean)
    assertEquals(listOf("back"), calls)
  }

  private class DoorViewModel :
    ReflexViewModel<DoorViewModel.State, DoorViewModel.Action, DoorViewModel.Action>(
      State(),
      CoroutineScope(Dispatchers.Unconfined),
    ) {
    @Serializable
    data class State(val open: Boolean = false)

    sealed interface Action {
      data object Open : Action
    }

    override fun viewAction(action: Action) = action

    override fun update(state: State, message: Action): Next<State, Action> =
      if (state.open) state.next() else State(open = true).next()
  }

  private class DoorNode(viewModel: DoorViewModel) :
    ViewModelIntrospectionNode<DoorViewModel.State, DoorViewModel.Action>(viewModel, DoorViewModel.State.serializer()) {
    override val name = "Door"
    override val actions = listOf(action("open", "Open", DoorViewModel.Action.Open))
  }

  @Test
  fun `an action the ViewModel ignores is reported, and wait answers when the screen and state are reached`() {
    val door = DoorNode(DoorViewModel())
    var idle = false
    val session = IntrospectionSession(isIdle = { idle }) { door }
    fun call(line: String) = Json.parseToJsonElement(session.handle(line)).jsonObject

    val timedOut = call("""{"cmd":"wait","screen":"Door","state":{"open":true},"timeoutMs":20}""")
    assertFalse(timedOut["ok"]!!.jsonPrimitive.boolean)
    assertEquals("Door", timedOut["name"]!!.jsonPrimitive.content)
    assertFalse(call("""{"cmd":"wait","timeoutMs":20}""")["ok"]!!.jsonPrimitive.boolean) // never idle

    assertEquals(null, call("""{"cmd":"do","action":"open"}""")["ignored"])
    assertTrue(call("""{"cmd":"do","action":"open"}""")["ignored"]!!.jsonPrimitive.boolean)
    assertTrue(call("""{"cmd":"wait","screen":"Door","state":{"open":true}}""")["ok"]!!.jsonPrimitive.boolean)
    idle = true
    assertTrue(call("""{"cmd":"wait"}""")["ok"]!!.jsonPrimitive.boolean)
  }
}
