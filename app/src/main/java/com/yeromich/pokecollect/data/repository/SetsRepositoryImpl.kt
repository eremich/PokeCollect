package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.remote.PokemonTcgApi
import com.yeromich.pokecollect.domain.model.CardSet
import com.yeromich.pokecollect.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SetsRepositoryImpl @Inject constructor(
    private val api: PokemonTcgApi
) : SetsRepository {

    private var cache: List<CardSet>? = null

    override fun getAllSets(): Flow<Result<List<CardSet>>> = flow {
        cache?.let { emit(Result.success(it)); return@flow }
        try {
            val sets = api.getSets().data.map { it.toDomain() }
            cache = sets
            emit(Result.success(sets))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
