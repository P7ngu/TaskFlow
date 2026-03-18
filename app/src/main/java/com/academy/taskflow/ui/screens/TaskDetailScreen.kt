package com.academy.taskflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academy.taskflow.model.Task

@Composable
fun TaskDetailScreen(
    task: Task?,
    onBack: () -> Unit,
    onToggleDone: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task?.title ?: "Task non trovato",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = onBack) { Text("Indietro") }
            }

            if (task == null) {
                Text(
                    text = "Non riesco a caricare i dettagli del task.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Priorita: ${task.priority.label}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onToggleDone) {
                    Text(if (task.isDone) "Segna come NON completato" else "Segna come completato")
                }
            }
        }
    }
}
