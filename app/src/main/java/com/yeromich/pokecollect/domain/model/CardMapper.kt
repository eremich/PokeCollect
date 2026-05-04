package com.yeromich.pokecollect.domain.model

import com.yeromich.pokecollect.data.remote.dto.CardDto

fun CardDto.toDomain() = Card(
    id = id,
    name = name,
    number = number,
    setName = set.name,
    rarity = rarity,
    imageSmall = images.small,
    imageLarge = images.large
)
