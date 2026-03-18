package com.academy.taskflow.di

import com.academy.taskflow.data.DefaultTaskRepository
import com.academy.taskflow.domain.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/*
 * RepositoryModule — collega l'interfaccia all'implementazione concreta.
 *
 * Quando qualcuno chiede TaskRepository (l'interfaccia),
 * Hilt fornisce DefaultTaskRepository (l'implementazione).
 *
 * @Binds: più efficiente di @Provides — non crea una nuova istanza,
 * collega semplicemente interfaccia e implementazione.
 * L'implementazione deve avere @Inject constructor per funzionare con @Binds.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: DefaultTaskRepository
    ): TaskRepository
}
