package com.yeromich.pokecollect.di

import android.content.Context
import com.yeromich.pokecollect.BuildConfig
import com.yeromich.pokecollect.data.remote.TcgdexApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024
    private const val FRESH_MINUTES = 60
    private const val OFFLINE_STALE_DAYS = 7

    // TCGdex answers with "no-store", so OkHttp would never cache. Catalog data barely changes,
    // so we keep responses fresh for an hour and serve them without touching the network.
    private val cacheResponses = Interceptor { chain ->
        chain.proceed(chain.request()).newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", CacheControl.Builder().maxAge(FRESH_MINUTES, TimeUnit.MINUTES).build().toString())
            .build()
    }

    // Without a connection, fall back to whatever we cached in the last week.
    private val offlineFallback = Interceptor { chain ->
        try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            val cached = chain.request().newBuilder()
                .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(OFFLINE_STALE_DAYS, TimeUnit.DAYS).build())
                .build()
            chain.proceed(cached).takeIf { it.isSuccessful } ?: throw e
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http"), CACHE_SIZE_BYTES))
        .addInterceptor(offlineFallback)
        .addNetworkInterceptor(cacheResponses)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
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
    @Named("tcgdex")
    fun provideTcgdexApi(@Named("tcgdex") retrofit: Retrofit): TcgdexApi =
        retrofit.create(TcgdexApi::class.java)
}
