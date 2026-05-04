package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardDto(
    val id: String,
    val name: String,
    val number: String,
    val set: CardSetDto,
    val rarity: String? = null,
    val images: CardImagesDto
)
