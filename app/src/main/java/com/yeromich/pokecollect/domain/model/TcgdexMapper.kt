package com.yeromich.pokecollect.domain.model

import com.yeromich.pokecollect.data.remote.TcgdexCardBriefDto
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.remote.TcgdexSetDto

fun TcgdexCardBriefDto.toDomain(setName: String = "", setId: String = ""): Card {
    val imageBase = image ?: ""
    return Card(
        id = id,
        name = name,
        number = localId,
        setName = setName,
        rarity = null,
        imageSmall = if (imageBase.isNotEmpty()) "$imageBase/low.webp" else "",
        imageLarge = if (imageBase.isNotEmpty()) "$imageBase/high.webp" else ""
    )
}

fun TcgdexCardDto.toDomain(): Card {
    val imageBase = image ?: ""
    return Card(
        id = id,
        name = name,
        number = localId,
        setName = set?.name ?: "",
        rarity = rarity,
        imageSmall = if (imageBase.isNotEmpty()) "$imageBase/low.webp" else "",
        imageLarge = if (imageBase.isNotEmpty()) "$imageBase/high.webp" else ""
    )
}

fun TcgdexSetDto.toDomain() = CardSet(
    id = id,
    name = name,
    series = serie?.name ?: "Другое",
    releaseDate = releaseDate ?: "",
    total = cardCount?.official ?: cardCount?.total ?: 0,
    logoUrl = if (logo != null) "$logo/logo.webp" else "",
    symbolUrl = if (symbol != null) "$symbol/symbol.webp" else ""
)
