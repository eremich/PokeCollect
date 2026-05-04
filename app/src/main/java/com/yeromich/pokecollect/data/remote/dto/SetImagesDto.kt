package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SetImagesDto(
    val logo: String,
    val symbol: String
)
