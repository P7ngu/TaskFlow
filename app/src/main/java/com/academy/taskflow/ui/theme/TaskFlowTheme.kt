package com.academy.taskflow.ui.theme

import android.R.attr.content
import android.R.id.content
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**TaskFlowTheme.kt è il tema Material3 dell'applicazione.
 * Tutti i componenti Material3 leggono AUTOMATICAMENTE questi colori.
 * Non serve passare i colori manualmente a ogni componente.*/
@Composable
fun TaskFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            /**primary: colore principale (bottoni, TopAppBar)*/
            primary = Color (0xFF263238L), //verde scuro
            secondary = Color(0xFF2E7D32L) //verde molto opaco
        )
    ) {
        /**content() è tutto l'albero dell'app che riceve il tema*/
        content()
    }
}