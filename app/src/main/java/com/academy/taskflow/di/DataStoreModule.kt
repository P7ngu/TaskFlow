package com.academy.taskflow.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
 * Estensione su Context — crea il DataStore con il nome "user_prefs".
 * preferencesDataStore: crea un singleton del DataStore a livello di Context.
 * Va dichiarata a livello di file (fuori dalla classe), una sola volta.
 */
private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_prefs")

/*
 * DataStoreModule — fornisce il DataStore tramite Hilt.
 *
 * @Singleton: una sola istanza — il DataStore non va creato più volte.
 * @ApplicationContext: usa il Context dell'app, non di una Activity.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
}
