package com.academy.taskflow.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/*
 *
 * DispatcherModule — fornisce i CoroutineDispatcher tramite Hilt.
 *
 * Separato da Qualifiers.kt per garantire il corretto ordine
 * di elaborazione da parte di KSP a compile-time.
 *
 * Dispatchers.IO: thread pool condiviso per operazioni bloccanti.
 * Iniettato tramite @IoDispatcher per distinguerlo da Main e Default.
 *
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
