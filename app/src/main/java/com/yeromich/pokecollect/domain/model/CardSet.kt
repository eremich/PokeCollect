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

/** TCGdex card ids are "<set id>-<number>", e.g. "me03-098" belongs to set "me03". */
fun setIdOfCard(cardId: String): String = cardId.substringBeforeLast('-')
