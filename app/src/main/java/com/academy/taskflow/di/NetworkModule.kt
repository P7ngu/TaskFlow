package com.academy.taskflow.di

import com.academy.taskflow.data.remote.TaskApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kotlin.jvm.java

/*
 *
 * NetworkModule — grafo di dipendenze per il layer di rete.
 *
 * PATTERN CHAIN OF RESPONSIBILITY (GoF) applicato agli interceptor:
 * ogni interceptor processa la richiesta/risposta in sequenza.
 * OkHttp permette di aggiungere logging, autenticazione, caching
 * come componenti intercambiabili e componibili.
 *
 * ORDINE DI COSTRUZIONE — Hilt risolve automaticamente:
 * 1. provideOkHttpClient()
 * 2. provideRetrofit(client: OkHttpClient)
 * 3. provideApiService(retrofit: Retrofit)
 *
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /*
     * OkHttpClient con HttpLoggingInterceptor.
     * Level.BODY: registra URL, headers e body completo in Logcat.
     * ATTENZIONE: mai usare Level.BODY in produzione —
     * espone token di autenticazione e dati sensibili nei log.
     * In produzione: Level.NONE o Level.BASIC con obfuscation.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

    /*
     * Retrofit con GsonConverterFactory.
     * GsonConverterFactory: deserializza automaticamente il JSON
     * nelle data class annotate — zero parsing manuale.
     * baseUrl DEVE terminare con "/" — requisito di Retrofit.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /*
     * retrofit.create(): genera il Proxy a runtime.
     * Legge le annotazioni @GET, @POST, @Path, @Query
     * e genera il codice di chiamata HTTP corrispondente.
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): TaskApiService =
        retrofit.create(TaskApiService::class.java)
}
