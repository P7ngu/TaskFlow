package com.academy.taskflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academy.taskflow.ui.tasks.TaskCard
import com.academy.taskflow.viewModel.TaskUiState

@Composable
fun TaskListScreen(
    uiState: TaskUiState,
    onTaskClick: (String) -> Unit,
    onToggleDone: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            TaskUiState.Loading -> CenteredMessage("Caricamento...")
            TaskUiState.Empty -> CenteredMessage("Nessun task disponibile")
            is TaskUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onCardClick = { onTaskClick(task.id) },
                            onToggleDone = { onToggleDone(task.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
