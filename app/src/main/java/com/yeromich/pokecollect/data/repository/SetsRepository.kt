package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.domain.model.CardSet
import kotlinx.coroutines.flow.Flow

interface SetsRepository {
    fun getAllSets(): Flow<Result<List<CardSet>>>
}
