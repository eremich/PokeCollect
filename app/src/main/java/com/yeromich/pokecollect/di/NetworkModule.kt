package com.yeromich.pokecollect.di

import com.yeromich.pokecollect.data.remote.PokemonTcgApi
import com.yeromich.pokecollect.data.remote.TcgdexApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.pokemontcg.io/v2/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("tcgdex")
    fun provideTcgdexRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.tcgdex.net/v2/de/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providePokemonTcgApi(retrofit: Retrofit): PokemonTcgApi =
        retrofit.create(PokemonTcgApi::class.java)

    @Provides
    @Singleton
    fun provideTcgdexApi(@Named("tcgdex") retrofit: Retrofit): TcgdexApi =
        retrofit.create(TcgdexApi::class.java)
}
