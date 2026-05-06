package com.yeromich.pokecollect.data.local

import androidx.room.Entity

@Entity(tableName = "collected_cards", primaryKeys = ["id", "variant"])
data class CollectedCardEntity(
    val id: String,
    val variant: String = "Normal",
    val name: String,
    val number: String,
    val setName: String,
    val rarity: String?,
    val imageSmall: String,
    val imageLarge: String,
    val addedAt: Long,
    val quantity: Int = 1,
    val condition: String? = null
)
