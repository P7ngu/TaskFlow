package com.academy.taskflow.di

import javax.inject.Qualifier

/*
 *
 * Qualifiers — annotation di disambiguazione per Hilt.
 *
 * File separato da DispatcherModule per garantire che KSP
 * processi la definizione del tipo PRIMA del suo utilizzo.
 * Se @IoDispatcher fosse nello stesso file che la usa,
 * KSP genererebbe error.NonExistentClass a compile-time.
 *
 * @Qualifier: meta-annotation JSR-330 che dichiara questa
 * annotation come qualificatore di iniezione.
 * @Retention(BINARY): mantenuta nel bytecode — necessario
 * per Hilt per leggerla durante la generazione del grafo DI.
 *
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
