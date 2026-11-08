package androidshowcase.reflex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonSummary(val id: Int, val name: String, val spriteUrl: String)

@Serializable
data class PokemonDetail(
  val id: Int,
  val name: String,
  val spriteUrl: String,
  val artworkUrl: String,
  val heightDecimetres: Int,
  val weightHectograms: Int,
  val types: List<String>,
  val stats: List<PokemonStat>,
) {
  val summary: PokemonSummary get() = PokemonSummary(id = id, name = name, spriteUrl = spriteUrl)
}

@Serializable
data class PokemonStat(val name: String, val value: Int)

/** One page of an offset-paginated list. [nextOffset] is null on the last page. */
data class Page<T>(val items: List<T>, val nextOffset: Int?)
