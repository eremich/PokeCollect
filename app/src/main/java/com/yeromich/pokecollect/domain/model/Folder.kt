package com.yeromich.pokecollect.domain.model

/** A user-made collection of owned cards, with what's needed to draw its tile. */
data class Folder(
    val id: Long,
    val name: String,
    val entries: Int,
    val totalEur: Double,
    val previewImages: List<String>
)
