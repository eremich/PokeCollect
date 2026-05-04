package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardImagesDto(
    val small: String,
    val large: String
)
