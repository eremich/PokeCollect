package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardSetDto(
    val id: String,
    val name: String,
    val series: String
)
