package com.yeromich.pokecollect.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TcgdexApi {

    @GET("cards")
    suspend fun searchCards(
        @Query("name") name: String? = null,
        @Query("localId") localId: String? = null,
        @Query("set.id") setId: String? = null
    ): List<TcgdexCardBriefDto>

    @GET("cards/{id}")
    suspend fun getCard(@Path("id") id: String): TcgdexCardDto

    @GET("sets")
    suspend fun getSets(): List<TcgdexSetDto>

    @GET("sets/{setId}")
    suspend fun getSet(@Path("setId") setId: String): TcgdexSetDetailDto
}
