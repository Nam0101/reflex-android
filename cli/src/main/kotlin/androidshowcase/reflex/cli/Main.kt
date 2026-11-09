package androidshowcase.reflex.cli

import androidshowcase.reflex.introspection.introspectionRequest
import java.io.IOException
import java.net.Socket
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json

private const val DEFAULT_PORT = 7171
private const val SOCKET_NAME = "androidshowcase.reflex.introspection"
private const val UNKNOWN_COMMAND = """{"ok":false,"error":"Unknown command. Use ls or do <action> [json]"}"""

private val USAGE = """
  |reflex headless                  Run the app headless with fixtures. One request per line on stdin,
  |                                 one JSON response per line on stdout. Lines are JSON
  |                                 ({"cmd":"ls"}, {"cmd":"do","action":"open","input":{"id":25}})
  |                                 or the short form: ls | do <action> [json]
  |                                 Exits 1 when a request had no fixture.
  |reflex ls [--port N]             Focused screen of a debug build on a device: name, state, actions.
  |reflex do <action> [json] [--port N]
  |                                 Run an action on the debug build, as a user would.
  |
  |Device commands need: adb forward tcp:$DEFAULT_PORT localabstract:$SOCKET_NAME
""".trimMargin()

fun main(args: Array<String>) {
  val port = args.indexOf("--port").takeIf { it >= 0 }?.let { args.getOrNull(it + 1)?.toIntOrNull() } ?: DEFAULT_PORT
  val positional = args.filterIndexed { i, arg -> arg != "--port" && args.getOrNull(i - 1) != "--port" }
  when (positional.firstOrNull()) {
    "headless" -> if (!headless()) exitProcess(1)
    "ls", "do" -> println(device(toRequest(positional.joinToString(" ")) ?: usage(), port))
    else -> usage()
  }
}

/** False when a request had no fixture: a headless run fails on it, like a behavior test does. */
private fun headless(): Boolean = HeadlessSession().use { session ->
  generateSequence(::readlnOrNull).filter { it.isNotBlank() }.forEach { line ->
    println(toRequest(line.trim())?.let(session::handle) ?: UNKNOWN_COMMAND)
    System.out.flush()
  }
  val violations = session.fixtures.violations
  if (violations.isNotEmpty()) System.err.println("Requests without a fixture: $violations")
  violations.isEmpty()
}

/** Accepts protocol JSON as is, or `ls` / `do <action> [json]`. */
private fun toRequest(line: String): String? = when {
  line.startsWith("{") -> line

  line == "ls" -> introspectionRequest("ls")

  line.startsWith("do ") -> {
    val rest = line.removePrefix("do ").trim()
    val action = rest.substringBefore(' ')
    val input = rest.substringAfter(' ', missingDelimiterValue = "").takeIf { it.isNotBlank() }
    runCatching { introspectionRequest("do", action, input?.let(Json::parseToJsonElement)) }.getOrNull()
  }

  else -> null
}

private fun device(request: String, port: Int): String = try {
  Socket("127.0.0.1", port).use { socket ->
    socket.getOutputStream().bufferedWriter().apply {
      write(request)
      newLine()
      flush()
    }
    // adb accepts the forwarded connection even when nothing listens on the device, then closes it.
    socket.getInputStream().bufferedReader().readLine() ?: throw IOException("the app closed the connection")
  }
} catch (e: IOException) {
  System.err.println("Cannot reach the app on port $port (${e.message}). Is a debug build running, and did you run")
  System.err.println("  adb forward tcp:$port localabstract:$SOCKET_NAME")
  exitProcess(1)
}

private fun usage(): Nothing {
  System.err.println(USAGE)
  exitProcess(2)
}
