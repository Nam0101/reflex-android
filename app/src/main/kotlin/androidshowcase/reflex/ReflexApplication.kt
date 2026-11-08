package androidshowcase.reflex

import android.app.Application
import android.content.Context
import android.util.Log
import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.local.SETTINGS_FILE_NAME
import androidshowcase.reflex.domain.platform.LanguageApplier
import androidshowcase.reflex.shell.Platform
import androidshowcase.reflex.shell.ReflexApp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.room3.Room
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app's object tree for the whole process, so navigation survives Activity recreation. */
class ReflexApplication : Application() {
  lateinit var reflex: ReflexApp
    private set

  override fun onCreate() {
    super.onCreate()
    reflex = ReflexApp(androidPlatform(this))
    DebugTools.install(this, reflex)
  }
}

val Context.reflexApp: ReflexApp get() = (applicationContext as ReflexApplication).reflex

/** The Android implementations of what Core asks for. Everything heavy is created lazily, off the main thread. */
private fun androidPlatform(context: Context) = Platform(
  uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
  databaseBuilder = { Room.databaseBuilder<ReflexDatabase>(context, ReflexDatabase.FILE_NAME) },
  settingsFile = File(context.filesDir, SETTINGS_FILE_NAME).absolutePath,
  languageApplier = AppCompatLanguageApplier,
  httpLogger = if (BuildConfig.DEBUG) { message -> Log.d("Http", message) } else null,
)

/** Per-app language through AppCompat: system setting on Android 13+, stored by AppCompat below. */
private object AppCompatLanguageApplier : LanguageApplier {
  override fun apply(language: androidshowcase.reflex.domain.model.AppLanguage) =
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
