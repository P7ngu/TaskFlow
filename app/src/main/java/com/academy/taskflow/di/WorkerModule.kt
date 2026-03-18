package com.academy.taskflow.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
 * WorkerModule — fornisce il WorkManager tramite Hilt.
 *
 * WorkManager.getInstance(context): recupera l'istanza singleton di WorkManager.
 * Hilt la inietta dove serve (es. TaskViewModel).
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
