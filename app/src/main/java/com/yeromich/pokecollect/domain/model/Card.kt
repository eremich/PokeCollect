package com.yeromich.pokecollect.domain.model

data class Card(
    val id: String,
    val name: String,
    val number: String,
    val setName: String,
    val rarity: String?,
    val imageSmall: String,
    val imageLarge: String
)
