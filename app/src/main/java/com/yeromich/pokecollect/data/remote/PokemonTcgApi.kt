package com.yeromich.pokecollect.data.remote

import com.yeromich.pokecollect.data.remote.dto.ApiResponse
import com.yeromich.pokecollect.data.remote.dto.CardDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PokemonTcgApi {

    @GET("cards")
    suspend fun getCards(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): ApiResponse<CardDto>
}
