package com.yeromich.pokecollect.domain.model

import com.yeromich.pokecollect.data.remote.dto.SetDto

fun SetDto.toDomain() = CardSet(
    id = id,
    name = name,
    series = series,
    releaseDate = releaseDate,
    total = total,
    logoUrl = images.logo,
    symbolUrl = images.symbol
)
