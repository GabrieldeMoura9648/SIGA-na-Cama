package com.example.fatecskip.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fatecskip.data.local.entities.SubjectEntity

import androidx.compose.material3.ButtonDefaults

@Composable
fun EditSubjectDialog(
    subject: SubjectEntity,
    onDismiss: () -> Unit,
    onSave: (id: Long, professor: String, classTime: String, daysOfWeek: String, absences: Int) -> Unit,
    onDeleteSubject: (Long) -> Unit = {}
) {
    var professor by remember { mutableStateOf(if (subject.professor == "Docente não definido") "" else subject.professor) }
    var classTime by remember { mutableStateOf(subject.classTime) }
    var absences by remember { mutableIntStateOf(subject.currentAbsences) }

    // Parse selected days
    val selectedDaysList = subject.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
    if (selectedDaysList.isEmpty()) {
        selectedDaysList.add(subject.dayOfWeek)
    }

    var selectedDays by remember { mutableStateOf(selectedDaysList.toSet()) }

    val allDays = listOf(
        1 to "Segunda",
        2 to "Terça",
        3 to "Quarta",
        4 to "Quinta",
        5 to "Sexta",
        6 to "Sábado"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar ${subject.name}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Nome do Professor") },
                    placeholder = { Text("Digite o nome do docente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = classTime,
                    onValueChange = { classTime = it },
                    label = { Text("Horário") },
                    placeholder = { Text("Ex: 19:00 - 22:30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Dias da Semana:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                // Usando FlowRow seria melhor, mas para compatibilidade simples com M3 faremos Wrap:
                // Mas como não podemos usar experimental do FlowRow sem complicação, vamos agrupar em Rows:
                val daysChunks = allDays.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    daysChunks.forEach { rowDays ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowDays.forEach { (dInt, dName) ->
                                FilterChip(
                                    selected = selectedDays.contains(dInt),
                                    onClick = {
                                        selectedDays = if (selectedDays.contains(dInt)) {
                                            selectedDays - dInt
                                        } else {
                                            selectedDays + dInt
                                        }
                                    },
                                    label = { Text(dName.take(3)) }
                                )
                            }
                        }
                    }
                }

                Text("Ajuste Fino de Faltas:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (absences > 0) absences-- },
                        enabled = absences > 0
                    ) { Text("-1") }

                    Text(
                        text = "$absences",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedButton(
                        onClick = { if (absences < subject.maxAbsences + 10) absences++ }
                    ) { Text("+1") }
                }

                Button(
                    onClick = {
                        onDeleteSubject(subject.id)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🗑️ Retirar da Grade")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDays = selectedDays.joinToString(",")
                    onSave(subject.id, professor, classTime, finalDays, absences)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
