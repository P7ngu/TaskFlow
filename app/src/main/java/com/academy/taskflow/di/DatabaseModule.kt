package com.academy.taskflow.di

import android.content.Context
import androidx.room.Room
import com.academy.taskflow.data.local.TaskDao
import com.academy.taskflow.data.local.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
 * DatabaseModule — fornisce il database Room e il DAO tramite Hilt.
 *
 * @Module: questo oggetto contiene le istruzioni per creare le dipendenze.
 * @InstallIn(SingletonComponent): una sola istanza per tutta l'app.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /*
     * provideDatabase: crea il database Room.
     *
     * @Provides: dice a Hilt come creare TaskDatabase.
     * @Singleton: crea una sola istanza — il database non si ricrea ad ogni inject.
     * @ApplicationContext: Hilt inietta il Context dell'app (non dell'Activity).
     *
     * Room.databaseBuilder: crea il database su disco con il nome specificato.
     * fallbackToDestructiveMigration: in sviluppo cancella e ricrea il DB
     * se lo schema cambia. MAI usare in produzione — usare Migration.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase = Room.databaseBuilder(
        context,
        TaskDatabase::class.java,
        "taskflow.db"
    ).fallbackToDestructiveMigration().build()

    /*
     * provideTaskDao: fornisce il DAO estratto dal database.
     * Hilt crea prima TaskDatabase (grazie a provideDatabase),
     * poi lo passa qui per estrarre il DAO.
     */
    @Provides
    @Singleton
    fun provideTaskDao(database: TaskDatabase): TaskDao =
        database.taskDao()
}
