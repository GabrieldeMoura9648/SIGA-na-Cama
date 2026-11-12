package com.example.fatecskip.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExtraSubjectDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val currentGrade by viewModel.subjects.collectAsState()
    var searchSemester by remember { mutableIntStateOf(1) }

    // Obter matérias disponíveis via HomeViewModel
    val availableSubjects = remember(searchSemester) {
        viewModel.getCatalogSubjectsBySemester(searchSemester)
    }

    var selectedSubjectCode by remember { mutableStateOf<String?>(null) }
    var conflictWarning by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var codeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var selectedDays by remember { mutableStateOf(setOf(1)) }
    var timesByDay by remember { mutableStateOf(mapOf(1 to "07:40 - 11:20")) }
    var is80Hours by remember { mutableStateOf(true) }
    var isManual by remember { mutableStateOf(false) }

    val days = listOf(
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
                text = "Incluir Matéria Extra 📚",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Semestre em que a disciplina pertence:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { sem ->
                        FilterChip(
                            selected = searchSemester == sem,
                            onClick = { searchSemester = sem; selectedSubjectCode = null; isManual = false; codeInput = ""; nameInput = "" },
                            label = { Text("${sem}º") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (4..6).forEach { sem ->
                        FilterChip(
                            selected = searchSemester == sem,
                            onClick = { searchSemester = sem; selectedSubjectCode = null; isManual = false; codeInput = ""; nameInput = "" },
                            label = { Text("${sem}º") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Disciplina do Catálogo:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val displayCode = if (isManual) "OUTRO - Digitar manualmente" else selectedSubjectCode ?: "Selecione uma matéria..."
                    OutlinedTextField(
                        value = displayCode,
                        onValueChange = {},
                        label = { Text("Código") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableSubjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.code} - ${sub.name}") },
                                onClick = {
                                    selectedSubjectCode = sub.code
                                    codeInput = sub.code
                                    nameInput = sub.name
                                    dayOfWeek = sub.dayOfWeek
                                    selectedDays = if (sub.daysOfWeek.isNotBlank()) {
                                        sub.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                                    } else {
                                        setOf(sub.dayOfWeek)
                                    }
                                    is80Hours = sub.classesPerDay == 4
                                    isManual = false
                                    expanded = false

                                    val targetDays = if (sub.daysOfWeek.isNotBlank()) {
                                        sub.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                                    } else listOf(sub.dayOfWeek)
                                    val conflicting = currentGrade.find { existing ->
                                        val existingDays = if (existing.daysOfWeek.isNotBlank()) {
                                            existing.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                                        } else listOf(existing.dayOfWeek)
                                        existingDays.intersect(targetDays.toSet()).any { day ->
                                            hasTimeOverlapForDay(existing.classTime, sub.classTime, day)
                                        }
                                    }
                                    conflictWarning = if (conflicting != null) {
                                        "⚠️ Conflito de horário com ${conflicting.name} (Substituirá na grade)"
                                    } else null
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("OUTRO - Digitar manualmente", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedSubjectCode = null
                                codeInput = ""
                                nameInput = ""
                                isManual = true
                                expanded = false
                                conflictWarning = null
                            }
                        )
                    }
                }

                conflictWarning?.let { warningMsg ->
                    Text(
                        text = warningMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nome da Matéria (Ex: Banco de Dados)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = !isManual && selectedSubjectCode != null
                )

                Text(
                    text = if (isManual) "Dias da Semana da Aula:" else "Dia da Semana da Aula:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                val daysChunked = days.chunked(3)
                daysChunked.forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowDays.forEach { (dInt, dName) ->
                            FilterChip(
                                selected = selectedDays.contains(dInt),
                                onClick = {
                                    if (isManual) {
                                        selectedDays = if (selectedDays.contains(dInt)) {
                                            if (selectedDays.size > 1) selectedDays - dInt else selectedDays
                                        } else {
                                            selectedDays + dInt
                                        }
                                        if (dInt in selectedDays && timesByDay[dInt] == null) {
                                            timesByDay = timesByDay + (dInt to (timesByDay.values.firstOrNull() ?: "07:40 - 11:20"))
                                        }
                                        dayOfWeek = selectedDays.minOrNull() ?: dayOfWeek
                                    } else {
                                        dayOfWeek = dInt
                                        selectedDays = setOf(dInt)
                                    }
                                },
                                label = { Text(dName.take(3)) },
                                modifier = Modifier.weight(1f),
                                enabled = isManual || selectedSubjectCode == null
                            )
                        }
                    }
                }

                if (isManual) {
                    Text(
                        "Horários por dia:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    days.filter { it.first in selectedDays }.forEach { (day, dayName) ->
                        OutlinedTextField(
                            value = timesByDay[day].orEmpty(),
                            onValueChange = { timesByDay = timesByDay + (day to it) },
                            label = { Text(dayName) },
                            placeholder = { Text("07:40 - 11:20") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "Carga Horária / Aulas por Dia:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = is80Hours,
                        onClick = { is80Hours = true },
                        label = { Text("80h (4 aulas/dia • Máx 20 faltas)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isManual || selectedSubjectCode == null
                    )
                    FilterChip(
                        selected = !is80Hours,
                        onClick = { is80Hours = false },
                        label = { Text("40h (2 aulas/dia • Máx 10 faltas)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isManual || selectedSubjectCode == null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        if (isManual) {
                            val classes = if (is80Hours) 4 else 2
                            val maxAbs = if (is80Hours) 20 else 10
                            val cd = if (codeInput.isBlank()) "DP${(10..99).random()}" else codeInput
                            
                            viewModel.addCustomSubject(
                                code = cd,
                                name = nameInput,
                                semester = searchSemester,
                                dayOfWeek = dayOfWeek,
                                classesPerDay = classes,
                                maxAbsences = maxAbs,
                                daysOfWeek = selectedDays.sorted().joinToString(","),
                                classTime = selectedDays.sorted().joinToString(" | ") { day ->
                                    val label = days.first { it.first == day }.second.take(3)
                                    "$label ${timesByDay[day].orEmpty().trim()}"
                                }
                            )
                        } else {
                            val sub = availableSubjects.find { it.code == selectedSubjectCode }
                            if (sub != null) {
                                viewModel.addSubjectFromOtherSemester(sub)
                            }
                        }
                        onDismiss()
                    }
                },
                enabled = nameInput.isNotBlank() &&
                    (selectedSubjectCode != null || isManual) &&
                    (!isManual || (selectedDays.isNotEmpty() &&
                        selectedDays.all { isValidTimeRange(timesByDay[it].orEmpty()) })),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Adicionar")
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

private fun hasTimeOverlapForDay(firstSchedule: String, secondSchedule: String, dayOfWeek: Int): Boolean {
    val dayPatterns = mapOf(
        1 to Regex("""\bseg(?:unda)?\b""", RegexOption.IGNORE_CASE),
        2 to Regex("""\bter(?:ça|ca)?\b""", RegexOption.IGNORE_CASE),
        3 to Regex("""\bqua(?:rta)?\b""", RegexOption.IGNORE_CASE),
        4 to Regex("""\bqui(?:nta)?\b""", RegexOption.IGNORE_CASE),
        5 to Regex("""\bsex(?:ta)?\b""", RegexOption.IGNORE_CASE),
        6 to Regex("""\bs(?:á|a)b(?:ado)?\b""", RegexOption.IGNORE_CASE)
    )
    fun ranges(schedule: String): List<Pair<Int, Int>> =
        schedule.split(Regex("""[|/;\n]+""")).flatMap { segment ->
            val labels = dayPatterns.filterValues { it.containsMatchIn(segment) }.keys
            if (labels.isNotEmpty() && dayOfWeek !in labels) return@flatMap emptyList()

            val minutes = Regex("""(\d{1,2}):(\d{2})""")
                .findAll(segment)
                .mapNotNull { match ->
                    val hour = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val minute = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    if (hour !in 0..23 || minute !in 0..59) return@mapNotNull null
                    hour * 60 + minute
                }
                .toList()
            minutes.chunked(2).mapNotNull { pair ->
                if (pair.size == 2 && pair[0] < pair[1]) pair[0] to pair[1] else null
            }
        }

    return ranges(firstSchedule).any { (startA, endA) ->
        ranges(secondSchedule).any { (startB, endB) ->
            startA < endB && startB < endA
        }
    }
}
