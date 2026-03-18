package com.academy.taskflow.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.academy.taskflow.model.Task

/*
 * TaskDetailScreen — schermata dettaglio task.
 *
 * ACCESSIBILITÀ — heading e struttura semantica:
 * WCAG 1.3.1 Info and Relationships: la struttura logica del contenuto
 * deve essere determinabile programmaticamente.
 * In Compose: heading = true marca un elemento come intestazione
 * per TalkBack — permette navigazione per intestazioni (gesto specifico).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task         : Task?,
    onBack       : () -> Unit,
    onToggleDone : () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: "Dettaglio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        /*
                         * ArrowBack: contentDescription obbligatoria.
                         * Descrive l'AZIONE, non l'icona:
                         * "Torna alla lista" è più utile di "Freccia indietro"
                         * per un utente TalkBack che non vede l'icona.
                         */
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Torna alla lista dei task"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (task == null) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { Text("Task non trovato") }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /*
             * Titolo con heading = true.
             * TalkBack annuncia "Intestazione: <titolo>" —
             * l'utente può navigare tra le intestazioni della schermata.
             * WCAG 1.3.1: struttura semantica del contenuto.
             */
            Text(
                text     = task.title,
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )

            /*
             * Priorità: testo con colore — WCAG 1.4.1 Use of Color.
             * Il colore NON deve essere l'unico mezzo per trasmettere
             * informazione. Qui mostriamo anche il testo "Priorità: Alta"
             * — il colore è un rinforzo visivo, non l'unica informazione.
             */
            Text(
                text  = "Priorità: ${task.priority.label}",
                color = Color(task.priority.color),
                style = MaterialTheme.typography.bodyLarge
            )

            if (task.description.isNotEmpty()) {
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /*
             * Bottone toggle con stateDescription.
             * stateDescription: descrive lo STATO corrente del controllo.
             * TalkBack annuncia: "Completato, Bottone" o "Da completare, Bottone"
             * prima di descrivere l'azione disponibile.
             * WCAG 4.1.2 Name, Role, Value: i componenti UI devono
             * esporre nome, ruolo e valore corrente agli screen reader.
             */
            Button(
                onClick  = onToggleDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        stateDescription = if (task.isDone)
                            "Completato" else "Da completare"
                    }
            ) {
                Icon(
                    imageVector = if (task.isDone)
                        Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                    /*
                     * contentDescription = null sull'icona dentro il Button:
                     * il Button ha già la sua semantica completa.
                     * Aggiungere una descrizione anche all'icona causerebbe
                     * duplicazione — TalkBack leggerebbe due volte.
                     */
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    if (task.isDone) "Segna come da fare"
                    else             "Segna come completato"
                )
            }
        }
    }
}
