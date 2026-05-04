package com.yeromich.pokecollect.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collected_cards")
data class CollectedCardEntity(
    @PrimaryKey val id: String,
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
