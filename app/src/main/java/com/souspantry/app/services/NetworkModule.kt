package com.souspantry.app.services

import com.souspantry.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = if (RegionHolder.code.isNotBlank())
                    chain.request().newBuilder().addHeader("X-SP-Region", RegionHolder.code).build()
                else chain.request()
                chain.proceed(req)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)   // Claude can take up to 3 min
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    // ── AI proxy (Cloudflare Worker) ─────────────────────────────────────────
    //
    // ALL model calls go through the shared Worker: provider keys live in the
    // Worker, never in the APK. Auth is a rotatable app secret, and
    // X-SP-Region lets the Worker route by region (MY → ILMU local model).
    // Mirrors iOS v2.2.1 / v2.2.3.

    @Provides @Singleton @Named("aiProxy")
    fun provideAiProxyOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.SP_PROXY_SECRET}")
                if (RegionHolder.code.isNotBlank()) builder.addHeader("X-SP-Region", RegionHolder.code)
                chain.proceed(builder.build())
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("aiProxy")
    fun provideAiProxyRetrofit(@Named("aiProxy") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(AI_PROXY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAnthropicService(@Named("aiProxy") retrofit: Retrofit): AnthropicService =
        retrofit.create(AnthropicService::class.java)
}

/** Shared Cloudflare Worker that fronts every model provider. */
const val AI_PROXY_BASE_URL = "https://souspantry-images.souspantry.workers.dev/"
