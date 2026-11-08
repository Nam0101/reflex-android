package androidshowcase.reflex

import android.app.Application
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import androidshowcase.reflex.introspection.IntrospectionSession
import androidshowcase.reflex.shell.ReflexApp
import androidshowcase.reflex.shell.introspectionSession
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Debug builds only (release has a no-op twin): serves the introspection protocol on an abstract
 * local socket, so `reflex ls` / `reflex do` drive the running app while views update live.
 *   adb forward tcp:7171 localabstract:<applicationId>.introspection
 * Any app on the device can reach an abstract socket: this must never ship in a release build.
 */
internal object DebugTools {
  private const val TAG = "ReflexIntrospection"

  fun install(application: Application, app: ReflexApp) {
    val session = app.introspectionSession()
    val socketName = "${application.packageName}.introspection"
    thread(name = "reflex-introspection", isDaemon = true) {
      val server = try {
        LocalServerSocket(socketName)
      } catch (e: IOException) {
        Log.w(TAG, "Cannot open $socketName", e)
        return@thread
      }
      Log.i(TAG, "Listening on localabstract:$socketName")
      while (true) {
        val client = try {
          server.accept()
        } catch (e: IOException) {
          Log.w(TAG, "Accept failed, stopping", e)
          break
        }
        thread(name = "reflex-introspection-client", isDaemon = true) { serve(client, session) }
      }
    }
  }

  private fun serve(client: LocalSocket, session: IntrospectionSession) = client.use {
    try {
      val reader = it.inputStream.bufferedReader()
      val writer = it.outputStream.bufferedWriter()
      generateSequence(reader::readLine).forEach { line ->
        writer.write(session.handle(line))
        writer.newLine()
        writer.flush()
      }
    } catch (e: IOException) {
      Log.d(TAG, "Client disconnected: ${e.message}")
    }
  }
}
