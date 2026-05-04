package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SetDto(
    val id: String,
    val name: String,
    val series: String,
    val releaseDate: String,
    val total: Int,
    val printedTotal: Int,
    val images: SetImagesDto
)
