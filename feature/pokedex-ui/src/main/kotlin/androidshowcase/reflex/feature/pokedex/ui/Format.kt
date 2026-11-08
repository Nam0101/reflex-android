package androidshowcase.reflex.feature.pokedex.ui

import java.util.Locale

internal fun String.displayName(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

internal fun Int.dexNumber(): String = "#%03d".format(Locale.ROOT, this)
