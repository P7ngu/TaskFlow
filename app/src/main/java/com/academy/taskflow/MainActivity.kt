package com.academy.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.academy.taskflow.ui.theme.TaskFlowTheme

/**MainActivity è il contenitore dell'app.
 * In Android ogni schermata è un'Activity.
 * Con Compose l'Activity fa una sola cosa: avvia il
 * contenuto con "setCompose"*/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                /**Tutto quello che mettiamo qui dentro,
                 * apparirà sullo schermo dell'emulatore.*/
                TaskFlowApp()
                /**Il nostro primo composable è un testo sullo schermo*/
            }
        }
    }
}

/**TaskFlowApp è il composable root dell'app.
 * @Composable è un'annotazione obbligatoria per ogni funzione
 * UI in Compose*/
@Composable
fun TaskFlowApp() {
    Text("Ciao TaskFlow!")
}

@Preview(showBackground = true)
@Composable
fun TaskFlowAppPreview() {
    MaterialTheme {
        TaskFlowApp()
    }
}