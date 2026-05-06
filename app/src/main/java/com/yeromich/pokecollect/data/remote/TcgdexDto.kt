package com.yeromich.pokecollect.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TcgdexCardBriefDto(
    val id: String,
    val localId: String,
    val name: String,
    val image: String? = null
)

@Serializable
data class TcgdexCardDto(
    val id: String,
    val localId: String,
    val name: String,
    val image: String? = null,
    val hp: Int? = null,
    val types: List<String>? = null,
    val illustrator: String? = null,
    val rarity: String? = null,
    val set: TcgdexSetBriefDto? = null,
    val attacks: List<TcgdexAttackDto>? = null,
    val pricing: TcgdexPricingDto? = null
)

@Serializable
data class TcgdexPricingDto(
    val cardmarket: TcgdexCardmarketDto? = null
)

@Serializable
data class TcgdexCardmarketDto(
    val updated: String? = null,
    val unit: String? = null,
    val avg: Double? = null,
    val low: Double? = null,
    val trend: Double? = null,
    val avg7: Double? = null,
    val avg30: Double? = null,
    @SerialName("avg-holo") val avgHolo: Double? = null,
    @SerialName("low-holo") val lowHolo: Double? = null,
    @SerialName("trend-holo") val trendHolo: Double? = null
)

@Serializable
data class TcgdexSetBriefDto(
    val id: String,
    val name: String,
    val cardCount: TcgdexCardCountDto? = null,
    val logo: String? = null,
    val symbol: String? = null,
    val releaseDate: String? = null,
    val serie: TcgdexSerieDto? = null
)

@Serializable
data class TcgdexSerieDto(
    val id: String,
    val name: String
)

@Serializable
data class TcgdexCardCountDto(
    val total: Int? = null,
    val official: Int? = null
)

@Serializable
data class TcgdexAttackDto(
    val name: String,
    val damage: String? = null,
    val effect: String? = null
)

@Serializable
data class TcgdexSetDetailDto(
    val id: String,
    val name: String,
    val cards: List<TcgdexCardBriefDto> = emptyList(),
    val logo: String? = null,
    val symbol: String? = null,
    val releaseDate: String? = null,
    val serie: TcgdexSerieDto? = null
)

@Serializable
data class TcgdexSetDto(
    val id: String,
    val name: String,
    val releaseDate: String? = null,
    val cardCount: TcgdexCardCountDto? = null,
    val logo: String? = null,
    val symbol: String? = null,
    val serie: TcgdexSerieDto? = null
)
