package com.academy.taskflow.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.academy.taskflow.model.Task

/*
 * TaskCard — card visuale per un singolo task.
 *
 * ACCESSIBILITÀ (WCAG 2.1 — Web Content Accessibility Guidelines):
 * Le linee guida WCAG, adottate da Google per Android, definiscono
 * quattro principi: Perceivable, Operable, Understandable, Robust (POUR).
 * TaskCard implementa:
 *   - Perceivable: ogni elemento interattivo ha una descrizione testuale
 *   - Operable: touch target minimo 48x48dp (Material Design guideline)
 *   - Robust: semantics esplicite per screen reader TalkBack
 */
@Composable
fun TaskCard(
    task         : Task,
    onCardClick  : () -> Unit,
    onToggleDone : () -> Unit
) {
    Card(
        onClick  = { onCardClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            /*
             * semantics: espone informazioni alla Accessibility API di Android.
             * TalkBack legge questi valori agli utenti con disabilità visive.
             *
             * contentDescription: descrizione completa della card.
             * Include titolo, priorità e stato — TalkBack legge tutto
             * in un unico gesto di swipe, senza richiedere navigazione interna.
             *
             * Principio WCAG 1.1.1 Non-text Content:
             * "Tutto il contenuto non testuale ha un'alternativa testuale."
             */
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Task: ${task.title}. ")
                    append("Priorità: ${task.priority.label}. ")
                    append(if (task.isDone) "Completato." else "Da completare.")
                }
            }
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = task.title,
                    style          = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted)
                        TextDecoration.LineThrough else null
                )
                Text(
                    text  = task.priority.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(task.priority.color)
                )
            }
            /*
             * Icona di completamento con semantics esplicite.
             *
             * contentDescription su Icon: OBBLIGATORIO per le icone
             * che comunicano informazioni — WCAG 1.1.1.
             * contentDescription = null è corretto SOLO per icone puramente
             * decorative che non aggiungono informazione non già presente.
             *
             * Qui l'icona comunica lo stato — la descrizione è obbligatoria.
             *
             * semantics { role = Role.Button }: dice a TalkBack che
             * questo elemento è un bottone — verrà annunciato come tale
             * e l'utente saprà che può attivarlo con doppio tap.
             */
            Icon(
                imageVector = if (task.isDone)
                    Icons.Default.CheckCircle else Icons.Default.Clear,
                contentDescription = if (task.isDone)
                    "Segna ${task.title} come da completare"
                    else "Segna ${task.title} come completato",
                modifier = Modifier
                    .size(48.dp)   /* touch target minimo 48dp — Material guideline */
                    .clickable(
                        onClickLabel = if (task.isDone)
                            "Rimuovi completamento" else "Completa task"
                    ) { onToggleDone() }
                    .semantics { role = Role.Button },
                tint = if (task.isDone)
                    MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
            )
        }
    }
}
