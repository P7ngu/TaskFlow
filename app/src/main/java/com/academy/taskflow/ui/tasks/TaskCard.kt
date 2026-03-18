package com.academy.taskflow.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.academy.taskflow.model.Task

/**TaskCard è una card per un singolo task.
 * Stateless: riceve tutto come parametro.
 * Non gestisce nessuno stato interno*/
@Composable
fun TaskCard(
    /**task è il task da mostrare*/
    task: Task,
    /**onCardClick questo è un evento: l'utente clicca sulla card*/
    onCardClick: () -> Unit,
    /**onToggleDone anche questo è un evento: l'utente clicca sulla chackbox*/
    onToggleDone: () -> Unit
) {
    Card(
        onClick = { onCardClick() },
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
        //.clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.Companion.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Column(modifier = Modifier.Companion.weight(1f)) {
                /**Titolo barrato se il task è stato completato*/
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.Companion.LineThrough else null
                )
                /**Qui definiamo l'etichetta priorità con colore*/
                Text(
                    text = task.priority.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(task.priority.color)
                )
            }
            /**Icona check circle se il task è stato completato,
             * altrimenti Clear se è il contrario*/
            Icon(
                imageVector = if (task.isDone) Icons.Default.CheckCircle
                else Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.Companion.size(28.dp).clickable { onToggleDone() },
                tint = if (task.isDone) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
        }
    }
}