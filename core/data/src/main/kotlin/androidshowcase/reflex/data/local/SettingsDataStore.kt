package androidshowcase.reflex.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

const val SETTINGS_FILE_NAME = "settings.preferences_pb"

/** A corrupt file is replaced instead of crashing the app. Cancelling [scope] releases the file. */
internal fun createSettingsDataStore(absolutePath: String, scope: CoroutineScope): DataStore<Preferences> =
  PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
    produceFile = { absolutePath.toPath() },
  )
