package com.yeromich.pokecollect.domain.model

data class CardSet(
    val id: String,
    val name: String,
    val series: String,
    val releaseDate: String,
    val total: Int,
    val logoUrl: String,
    val symbolUrl: String
)
