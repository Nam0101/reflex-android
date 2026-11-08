package androidshowcase.reflex.data.remote

import androidshowcase.reflex.domain.model.Page
import androidshowcase.reflex.domain.model.PokemonDetail
import androidshowcase.reflex.domain.model.PokemonStat
import androidshowcase.reflex.domain.model.PokemonSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

internal suspend fun HttpClient.pokemonList(offset: Int, limit: Int): PokemonListDto = get("pokemon") {
  parameter("offset", offset)
  parameter("limit", limit)
}.body()

internal suspend fun HttpClient.pokemon(id: Int): PokemonDto = get("pokemon/$id").body()

@Serializable
internal data class PokemonListDto(val next: String? = null, val results: List<NamedResourceDto>)

@Serializable
internal data class NamedResourceDto(val name: String, val url: String)

@Serializable
internal data class PokemonDto(
  val id: Int,
  val name: String,
  val height: Int,
  val weight: Int,
  val types: List<TypeSlotDto>,
  val stats: List<StatDto>,
)

@Serializable
internal data class TypeSlotDto(val slot: Int, val type: NamedResourceDto)

@Serializable
internal data class StatDto(@SerialName("base_stat") val baseStat: Int, val stat: NamedResourceDto)

internal fun PokemonListDto.toPage(offset: Int): Page<PokemonSummary> = Page(
  items = results.map {
    val id = it.pokemonId()
    PokemonSummary(id = id, name = it.name, spriteUrl = spriteUrl(id), artworkUrl = artworkUrl(id))
  },
  nextOffset = if (next == null) null else offset + results.size,
)

internal fun PokemonDto.toDomain(): PokemonDetail = PokemonDetail(
  id = id,
  name = name,
  spriteUrl = spriteUrl(id),
  artworkUrl = artworkUrl(id),
  heightDecimetres = height,
  weightHectograms = weight,
  types = types.sortedBy { it.slot }.map { it.type.name },
  stats = stats.map { PokemonStat(name = it.stat.name, value = it.baseStat) },
)

/** `https://pokeapi.co/api/v2/pokemon/25/` → 25. */
internal fun NamedResourceDto.pokemonId(): Int = url.trimEnd('/').substringAfterLast('/').toInt()

private fun spriteUrl(id: Int) = "$SPRITES/$id.png"

/** Large official artwork, derived from the id (favorites store only the sprite). */
internal fun artworkUrl(id: Int) = "$SPRITES/other/official-artwork/$id.png"
