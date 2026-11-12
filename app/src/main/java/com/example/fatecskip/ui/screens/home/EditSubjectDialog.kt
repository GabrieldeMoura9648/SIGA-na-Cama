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
    val dayAbbreviations = mapOf(
        1 to "Seg",
        2 to "Ter",
        3 to "Qua",
        4 to "Qui",
        5 to "Sex",
        6 to "Sáb"
    )
    val initialSchedule = remember(subject.id, subject.classTime) {
        parseSubjectSchedule(subject.classTime, selectedDays)
    }
    var timesByDay by remember(subject.id) { mutableStateOf(initialSchedule) }

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
                                        if (selectedDays.contains(dInt)) {
                                            if (selectedDays.size > 1) selectedDays = selectedDays - dInt
                                        } else {
                                            selectedDays = selectedDays + dInt
                                            if (timesByDay[dInt] == null) {
                                                timesByDay = timesByDay + (dInt to (timesByDay.values.firstOrNull() ?: "07:40 - 11:20"))
                                            }
                                        }
                                    },
                                    label = { Text(dName.take(3)) }
                                )
                            }
                        }
                    }
                }

                Text(
                    "Horários por dia:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                allDays.filter { it.first in selectedDays }.forEach { (day, dayName) ->
                    OutlinedTextField(
                        value = timesByDay[day].orEmpty(),
                        onValueChange = { timesByDay = timesByDay + (day to it) },
                        label = { Text(dayName) },
                        placeholder = { Text("07:40 - 11:20") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    val finalDays = selectedDays.sorted().joinToString(",")
                    val schedule = selectedDays.sorted().joinToString(" | ") { day ->
                        "${dayAbbreviations[day]} ${timesByDay[day].orEmpty().trim()}"
                    }
                    onSave(subject.id, professor, schedule, finalDays, absences)
                },
                enabled = selectedDays.isNotEmpty() &&
                    selectedDays.all { isValidTimeRange(timesByDay[it].orEmpty()) },
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

private fun isValidTimeRange(value: String): Boolean {
    val match = Regex("""^\s*(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})\s*$""").matchEntire(value)
        ?: return false
    val startHour = match.groupValues[1].toIntOrNull() ?: return false
    val startMinute = match.groupValues[2].toIntOrNull() ?: return false
    val endHour = match.groupValues[3].toIntOrNull() ?: return false
    val endMinute = match.groupValues[4].toIntOrNull() ?: return false
    val start = startHour * 60 + startMinute
    val end = endHour * 60 + endMinute
    return startHour in 0..23 && endHour in 0..23 &&
        startMinute in 0..59 && endMinute in 0..59 && start < end
}

private fun parseSubjectSchedule(
    classTime: String,
    selectedDays: Set<Int>
): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    val dayPatterns = mapOf(
        1 to Regex("""\bseg(?:unda)?\b""", RegexOption.IGNORE_CASE),
        2 to Regex("""\bter(?:ça|ca)?\b""", RegexOption.IGNORE_CASE),
        3 to Regex("""\bqua(?:rta)?\b""", RegexOption.IGNORE_CASE),
        4 to Regex("""\bqui(?:nta)?\b""", RegexOption.IGNORE_CASE),
        5 to Regex("""\bsex(?:ta)?\b""", RegexOption.IGNORE_CASE),
        6 to Regex("""\bs(?:á|a)b(?:ado)?\b""", RegexOption.IGNORE_CASE)
    )
    classTime.split(Regex("""[|/;\n]+""")).forEach { segment ->
        val day = dayPatterns.entries.firstOrNull { (_, pattern) ->
            pattern.containsMatchIn(segment)
        }?.key
        val times = Regex("""\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2}""")
            .find(segment)?.value
        if (day != null && times != null) result[day] = times
    }

    val unlabelledRanges = Regex("""\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2}""")
        .findAll(classTime)
        .toList()
    if (result.isEmpty() && unlabelledRanges.size == selectedDays.size) {
        selectedDays.sorted().forEachIndexed { index, day ->
            result[day] = unlabelledRanges[index].value
        }
    }

    val fallbackTime = Regex("""\d{1,2}:\d{2}\s*-\s*\d{1,2}:\d{2}""")
        .find(classTime)?.value ?: "07:40 - 11:20"
    selectedDays.forEach { day ->
        if (result[day] == null) result[day] = fallbackTime
    }
    return result
}
