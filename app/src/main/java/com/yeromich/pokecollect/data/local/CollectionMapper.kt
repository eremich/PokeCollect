package com.yeromich.pokecollect.data.local

import com.yeromich.pokecollect.domain.model.Card

fun CollectedCardEntity.toDomain() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName,
    rarity = rarity,
    imageSmall = imageSmall,
    imageLarge = imageLarge
)

fun Card.toEntity(addedAt: Long = System.currentTimeMillis()) = CollectedCardEntity(
    id = id,
    name = name,
    number = number,
    setName = setName,
    rarity = rarity,
    imageSmall = imageSmall,
    imageLarge = imageLarge,
    addedAt = addedAt
)
