package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.domain.model.CardSet
import com.yeromich.pokecollect.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Named

class SetsRepositoryImpl @Inject constructor(
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi
) : SetsRepository {

    private var cache: List<CardSet>? = null

    override fun getAllSets(): Flow<Result<List<CardSet>>> = flow {
        cache?.let { emit(Result.success(it)); return@flow }
        try {
            val sets = tcgdexApi.getSets().map { it.toDomain() }
            cache = sets
            emit(Result.success(sets))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
