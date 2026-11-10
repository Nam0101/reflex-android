package androidshowcase.reflex

import android.app.Application
import android.content.Context
import android.util.Log
import androidshowcase.reflex.data.local.ReflexDatabase
import androidshowcase.reflex.data.remote.createOkHttpClient
import androidshowcase.reflex.data.remote.okHttpEngine
import androidshowcase.reflex.domain.platform.LanguageApplier
import androidshowcase.reflex.shell.Platform
import androidshowcase.reflex.shell.ReflexApp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.room3.Room
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/** Holds the app's object tree for the whole process, so navigation survives Activity recreation. */
class ReflexApplication :
  Application(),
  SingletonImageLoader.Factory {
  lateinit var reflex: ReflexApp
    private set

  /** One HTTP stack for the API and the images: built on first use, off the main thread. */
  private val okHttp = lazy { createOkHttpClient(cacheDir = File(cacheDir, "http")) }

  override fun onCreate() {
    super.onCreate()
    reflex = ReflexApp(androidPlatform(this, okHttp))
    DebugTools.install(this, reflex)
  }

  /** Images share the API client's connection pool; Coil keeps its own disk cache, so they skip the HTTP one. */
  override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
    .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttp.value.newBuilder().cache(null).build() })) }
    .build()
}

val Context.reflexApp: ReflexApp get() = (applicationContext as ReflexApplication).reflex

/** The Android implementations of what Core asks for. Everything heavy is created lazily, off the main thread. */
private fun androidPlatform(context: Context, okHttp: Lazy<OkHttpClient>) = Platform(
  uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
  databaseBuilder = { Room.databaseBuilder<ReflexDatabase>(context, ReflexDatabase.FILE_NAME) },
  storageDir = context.filesDir.absolutePath,
  onStorageProblem = { Log.e("Storage", "Unreadable store file, starting from defaults: $it", it.cause) },
  languageApplier = AppCompatLanguageApplier,
  httpEngine = { okHttpEngine(okHttp.value) },
  httpLogger = if (BuildConfig.DEBUG) { message -> Log.d("Http", message) } else null,
)

/** Per-app language through AppCompat: system setting on Android 13+, stored by AppCompat below. */
private object AppCompatLanguageApplier : LanguageApplier {
  override fun apply(language: androidshowcase.reflex.domain.model.AppLanguage) =
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
