package androidshowcase.reflex.cli

import androidshowcase.reflex.introspection.introspectionRequest
import androidshowcase.reflex.introspection.introspectionWaitRequest
import java.io.File
import java.io.IOException
import java.net.Socket
import java.nio.file.Path
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val DEFAULT_PORT = 7171
private const val DEFAULT_PACKAGE = "androidshowcase.reflex"
private const val UNKNOWN_COMMAND =
  """{"ok":false,"error":"Unknown command. Use ls, do <action> [json], wait [screen [json]], advance <duration> or relaunch"}"""
private val SETTLE_TIMEOUT = 5.seconds
private val LAUNCH_TIMEOUT = 15.seconds

private val USAGE = """
  |reflex headless [--record <dir>]
  |    Run the app headless with fixtures. One request per line on stdin, one JSON response per line on stdout:
  |      ls                      focused screen: name, state, actions
  |      do <action> [json]      run an action as a user would; "ignored":true when the app did nothing
  |      wait                    until no effect is running
  |      wait <screen> [json]    until that screen is focused and its state contains the json
  |      advance <duration>      move the app's clock: 1d, 36h, 90m
  |      relaunch                restart the app on the same disk
  |    or the protocol JSON as is: {"cmd":"do","action":"open","input":{"id":25}}
  |    Every command answers once the app is idle again, so piped scripts need no sleep.
  |    Exits 1 when a request had no fixture. --record fetches such a request from the real server and
  |    saves it under <dir> (cli/src/main/resources/fixtures).
  |
  |reflex ls | do <action> [json] | wait [screen [json]]     [--package <applicationId>] [--port N]
  |    The same commands against a debug build on a device or emulator.
  |reflex screenshot [file.png]                              [--package <applicationId>]
  |    Save what the device shows (default: screenshot.png).
  |reflex reset                                              [--package <applicationId>] [--port N]
  |    Clear the app's data, start it again and wait for its first screen.
  |
  |Device commands run adb themselves (ANDROID_SERIAL picks the device) and forward tcp:<port> to the socket
  |localabstract:<applicationId>.introspection. --package defaults to $DEFAULT_PACKAGE: pass the id of the
  |variant you installed when a flavor or build type adds a suffix.
""".trimMargin()

fun main(args: Array<String>) {
  val options = HashMap<String, String>()
  val positional = ArrayList<String>()
  var i = 0
  while (i < args.size) {
    val arg = args[i]
    if (arg in setOf("--port", "--package", "--record")) {
      options[arg] = args.getOrNull(i + 1) ?: usage()
      i += 2
    } else {
      positional += arg
      i++
    }
  }
  val device = Device(
    packageName = options["--package"] ?: DEFAULT_PACKAGE,
    port = options["--port"]?.let { it.toIntOrNull() ?: usage() } ?: DEFAULT_PORT,
  )
  when (positional.firstOrNull()) {
    "headless" -> if (!headless(options["--record"]?.let(Path::of))) exitProcess(1)
    "ls", "do", "wait" -> println(device.send(toRequest(positional.joinToString(" ")) ?: usage()))
    "screenshot" -> println(device.screenshot(File(positional.getOrNull(1) ?: "screenshot.png")))
    "reset" -> println(device.reset())
    else -> usage()
  }
}

/** False when a request had no fixture: a headless run fails on it, like a behavior test does. */
private fun headless(recordTo: Path?): Boolean = HeadlessSession(FixtureHttp(recordTo)).use { session ->
  // The first screen is decided asynchronously; a command piped in right away would still see Splash.
  session.settle()
  generateSequence(::readlnOrNull).map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
    println(session.run(line))
    System.out.flush()
  }
  val violations = session.fixtures.violations
  if (violations.isNotEmpty()) System.err.println("Requests without a fixture: $violations")
  session.storageProblems.forEach { System.err.println("Unreadable store file: $it") }
  violations.isEmpty()
}

private fun HeadlessSession.run(line: String): String = when {
  line == "relaunch" -> {
    relaunch()
    settle()
    handle(introspectionRequest("ls"))
  }

  line.startsWith("advance ") -> {
    val duration = Duration.parseOrNull(line.removePrefix("advance ").trim())
    if (duration == null) {
      """{"ok":false,"error":"advance takes a duration: 1d, 36h, 90m"}"""
    } else {
      clock.advance(duration)
      buildJsonObject {
        put("ok", true)
        put("now", clock.now().toString())
      }.toString()
    }
  }

  else -> toRequest(line)?.let { request -> handle(request).also { settle() } } ?: UNKNOWN_COMMAND
}

private fun HeadlessSession.settle() {
  handle(introspectionWaitRequest(timeout = SETTLE_TIMEOUT))
}

/** Accepts protocol JSON as is, or `ls` / `do <action> [json]` / `wait [screen [json]]`. Null when it is neither. */
private fun toRequest(line: String): String? {
  val words = line.split(' ', limit = 3).filter { it.isNotBlank() }
  val json = runCatching { words.getOrNull(2)?.let(Json::parseToJsonElement) }.getOrElse { return null }
  return when {
    line.startsWith("{") -> line
    words == listOf("ls") -> introspectionRequest("ls")
    words.first() == "do" && words.size >= 2 -> introspectionRequest("do", words[1], json)
    words.first() == "wait" -> introspectionWaitRequest(words.getOrNull(1)?.takeUnless { it == "idle" }, json)
    else -> null
  }
}

/** A debug build on a device, reached with adb. */
private class Device(private val packageName: String, private val port: Int) {
  private val socketName = "$packageName.introspection"

  fun send(request: String): String {
    forward()
    return try {
      exchange(request)
    } catch (e: IOException) {
      fail("Cannot reach $packageName on port $port (${e.message}). Is a debug build of that package running?")
    }
  }

  fun screenshot(file: File): String {
    val png = adb("exec-out", "screencap", "-p")
    file.writeBytes(png)
    return buildJsonObject {
      put("ok", true)
      put("file", file.absolutePath)
    }.toString()
  }

  fun reset(): String {
    adb("shell", "pm", "clear", packageName)
    adb("shell", "monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1")
    forward()
    // The process is starting: the socket refuses connections until the app has opened it.
    val deadline = TimeSource.Monotonic.markNow() + LAUNCH_TIMEOUT
    while (true) {
      try {
        return exchange(introspectionWaitRequest(timeout = SETTLE_TIMEOUT))
      } catch (e: IOException) {
        if (deadline.hasPassedNow()) fail("$packageName did not open its socket after a reset (${e.message})")
        Thread.sleep(RETRY_MS)
      }
    }
  }

  private fun forward() {
    adb("forward", "tcp:$port", "localabstract:$socketName")
  }

  private fun exchange(request: String): String = Socket("127.0.0.1", port).use { socket ->
    socket.getOutputStream().bufferedWriter().apply {
      write(request)
      newLine()
      flush()
    }
    // adb accepts the forwarded connection even when nothing listens on the device, then closes it.
    socket.getInputStream().bufferedReader().readLine() ?: throw IOException("the app closed the connection")
  }

  private fun adb(vararg command: String): ByteArray {
    val process = try {
      ProcessBuilder("adb", *command).redirectError(ProcessBuilder.Redirect.INHERIT).start()
    } catch (e: IOException) {
      fail("Cannot run adb (${e.message}). Is it on the PATH?")
    }
    val output = process.inputStream.readBytes()
    if (process.waitFor() != 0) fail("adb ${command.joinToString(" ")} failed")
    return output
  }

  private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
  }

  private companion object {
    const val RETRY_MS = 200L
  }
}

private fun usage(): Nothing {
  System.err.println(USAGE)
  exitProcess(2)
}
