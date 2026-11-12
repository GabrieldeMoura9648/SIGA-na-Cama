package com.example.fatecskip.data.repository

import com.example.fatecskip.data.local.DefaultFatecData
import com.example.fatecskip.data.local.EventDao
import com.example.fatecskip.data.local.ExcuseDao
import com.example.fatecskip.data.local.SubjectDao
import com.example.fatecskip.data.local.entities.EventEntity
import com.example.fatecskip.data.local.entities.ExcuseEntity
import com.example.fatecskip.data.local.entities.SubjectEntity
import com.example.fatecskip.data.preferences.UserPreferencesRepository
import com.example.fatecskip.data.remote.FatecApiService
import com.example.fatecskip.domain.model.SkipVerdict
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class AcademicRepository(
    private val subjectDao: SubjectDao,
    private val eventDao: EventDao,
    private val excuseDao: ExcuseDao,
    private val apiService: FatecApiService,
    private val preferencesRepository: UserPreferencesRepository,
    private val holidayRepository: HolidayRepository
) {

    fun getSubjectsForSemester(semester: Int): Flow<List<SubjectEntity>> {
        return subjectDao.getSubjectsBySemester(semester)
    }
    
    suspend fun getSubjectsBySemesterList(semester: Int): List<SubjectEntity> {
        return subjectDao.getSubjectsBySemesterList(semester)
    }

    fun getCatalogSubjectsBySemester(semester: Int): List<SubjectEntity> {
        val allDefaults = DefaultFatecData.getDefaultSubjects()
        return allDefaults.filter { it.semester == semester }
    }

    fun getAllEvents(): Flow<List<EventEntity>> {
        return eventDao.getAllEvents()
    }

    fun getAllExcuses(): Flow<List<ExcuseEntity>> {
        return excuseDao.getAllExcuses()
    }

    suspend fun isAcademicHoliday(date: LocalDate): Boolean {
        return eventDao.getHolidaysByDate(date.toString()).isNotEmpty() ||
            holidayRepository.isHoliday(date)
    }

    suspend fun incrementAbsence(subject: SubjectEntity, count: Int = 1) {
        val updated = subject.copy(
            currentAbsences = (subject.currentAbsences + count).coerceAtMost(subject.maxAbsences + 20)
        )
        subjectDao.updateSubject(updated)
    }

    suspend fun decrementAbsence(subject: SubjectEntity, count: Int = 1) {
        val updated = subject.copy(
            currentAbsences = (subject.currentAbsences - count).coerceAtLeast(0)
        )
        subjectDao.updateSubject(updated)
    }

    suspend fun registerAbsenceForToday(
        semester: Int,
        date: LocalDate = LocalDate.now()
    ): Int {
        if (isAcademicHoliday(date)) return 0

        val dayOfWeek = date.dayOfWeek.value
        val subjectsToday = subjectDao.getSubjectsBySemesterList(semester)
            .filter { it.isClassOnDay(dayOfWeek) }

        subjectsToday.forEach { subject ->
            val updated = subject.copy(
                currentAbsences = (subject.currentAbsences + subject.classesPerDay)
                    .coerceAtMost(subject.maxAbsences + 20)
            )
            subjectDao.updateSubject(updated)
        }

        return subjectsToday.size
    }

    suspend fun addEvent(title: String, date: String, type: String, isCritical: Boolean): Long {
        val event = EventEntity(
            title = title.trim(),
            date = date.trim(),
            type = type,
            isCritical = isCritical
        )
        return eventDao.insertEvent(event)
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    suspend fun deleteEventById(id: Long) {
        eventDao.deleteEventById(id)
    }

    suspend fun addCustomSubject(
        code: String,
        name: String,
        semester: Int,
        dayOfWeek: Int,
        classesPerDay: Int,
        maxAbsences: Int
    ): Long {
        val extensionCodes = setOf("ES101", "ES201", "GAG01", "LDW01", "LDM01", "LDM02")
        val subject = SubjectEntity(
            code = code.uppercase().trim(),
            name = name.trim(),
            semester = semester,
            dayOfWeek = dayOfWeek,
            classesPerDay = classesPerDay,
            currentAbsences = 0,
            maxAbsences = maxAbsences,
            isCustomized = true,
            isExtension = extensionCodes.contains(code.uppercase().trim())
        )
        return subjectDao.insertSubject(subject)
    }

    suspend fun updateSubjectDetails(
        subjectId: Long,
        professor: String,
        classTime: String,
        daysOfWeek: String,
        currentAbsences: Int
    ) {
        val subject = subjectDao.getSubjectById(subjectId)
        if (subject != null) {
            val updated = subject.copy(
                professor = professor.trim(),
                classTime = classTime.trim(),
                daysOfWeek = daysOfWeek.trim(),
                currentAbsences = currentAbsences,
                isCustomized = true
            )
            subjectDao.updateSubject(updated)
        }
    }

    suspend fun removeSubjectFromGrade(subjectId: Long) {
        subjectDao.deleteSubjectById(subjectId)
    }

    /**
     * Adiciona matéria de outro semestre ("Grade Flexível/Fora de fase") na grade atual
     */
    suspend fun addExtraSubject(
        code: String,
        name: String,
        targetSemester: Int,
        dayOfWeek: Int,
        daysOfWeekStr: String,
        classTime: String,
        classesPerDay: Int,
        maxAbsences: Int,
        isExtension: Boolean
    ): Long {
        // Verifica se a matéria já existe naquele semestre e dia para não duplicar
        val existingAll = subjectDao.getSubjectsBySemesterList(targetSemester)
        val isDuplicate = existingAll.any { it.code.equals(code, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
        
        if (isDuplicate) return -1L

        val targetDays = if (daysOfWeekStr.isNotBlank()) {
            daysOfWeekStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            listOf(dayOfWeek)
        }

        existingAll.forEach { existing ->
            val existingDays = if (existing.daysOfWeek.isNotBlank()) {
                existing.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else {
                listOf(existing.dayOfWeek)
            }

            val dayConflict = existingDays.intersect(targetDays.toSet()).isNotEmpty()
            val timeConflict = existing.classTime.equals(classTime, ignoreCase = true)

            if (dayConflict && timeConflict) {
                subjectDao.deleteSubjectById(existing.id) // Substitui a matéria conflitante
            }
        }

        val subject = SubjectEntity(
            code = code.uppercase().trim(),
            name = name.trim(),
            semester = targetSemester,
            dayOfWeek = dayOfWeek,
            daysOfWeek = daysOfWeekStr,
            classesPerDay = classesPerDay,
            currentAbsences = 0,
            maxAbsences = maxAbsences,
            isCustomized = true,
            isExtension = isExtension,
            classTime = classTime
        )
        return subjectDao.insertSubject(subject)
    }

    suspend fun getRandomExcuse(): String {
        val dbExcuse = excuseDao.getRandomExcuse()?.text
        if (!dbExcuse.isNullOrBlank()) return dbExcuse
        val defaults = DefaultFatecData.getDefaultExcuses()
        return defaults.random().text
    }

    /**
     * Algoritmo central do "Posso Faltar Hoje?".
     * Avalia o dia atual contra feriados, eventos críticos e limites de faltas no SIGA.
     */
    suspend fun evaluateSkipVerdict(
        date: LocalDate = LocalDate.now(),
        semester: Int
    ): SkipVerdict {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD
        val dayOfWeek = date.dayOfWeek.value // 1 (Segunda) .. 7 (Domingo)

        // 1. Verificação de Domingo ou Feriado
        if (isAcademicHoliday(date)) {
            val holidayTitle = eventDao.getHolidaysByDate(dateString).firstOrNull()?.title ?: "Feriado"
            val messages = listOf(
                "Levanta pra quê? Hoje é feriado ($holidayTitle) na Fatec! O campus tá trancado e o cobertor te abraçou.",
                "Glória a Deux! Hoje é $holidayTitle. Desliga o despertador e volta a dormir com a bênção da coordenação!",
                "Nem o diretor pisou na Fatec hoje por causa de $holidayTitle. Fique de boas na sua cama!"
            )
            return SkipVerdict.NoClassOrHoliday(holidayTitle, messages.random())
        }

        if (dayOfWeek == 7) { // Domingo
            return SkipVerdict.NoClassOrHoliday(
                "Domingo",
                "Hoje é domingo, seu emocionado! Quem vai pra faculdade de domingo? Volta a dormir!"
            )
        }

        // Matérias do dia (suporta matérias de múltiplos dias como Terça e Quinta)
        val allSemesterSubjects = subjectDao.getSubjectsBySemesterList(semester)
        val subjectsToday = allSemesterSubjects.filter { it.isClassOnDay(dayOfWeek) }
        if (subjectsToday.isEmpty()) {
            val messages = listOf(
                "Hoje não tem nenhuma aula cadastrada para o seu semestre (${semester}º). Pode maratonar série em paz!",
                "Grade zerada hoje! Seu único compromisso é descansar a mente.",
                "Sem aulas hoje no SIGA para o ${semester}º semestre. Aproveite o dia sem estresse!"
            )
            return SkipVerdict.NoClassOrHoliday("Sem aulas hoje", messages.random())
        }

        // 2. Verificação de Evento Crítico (Provas P1/P2, entregas de Projeto Integrador, etc.)
        val criticalEventsToday = eventDao.getCriticalEventsByDate(dateString)
        if (criticalEventsToday.isNotEmpty()) {
            val eventTitle = criticalEventsToday.first().title
            val messages = listOf(
                "LEVANTA AGORA! Hoje tem '$eventTitle'! Se faltar, já pode ir pesquisando o horário da DP no semestre que vem!",
                "PROIBIDO FALTAR! Hoje tem evento com peso na alma: '$eventTitle'. Nem inventa de meter o louco!",
                "🚨 ALERTA VERMELHO! '$eventTitle' hoje. Se você faltar, nem o coordenador vai conseguir te defender!"
            )
            return SkipVerdict.ForbiddenCriticalEvent(eventTitle, messages.random())
        }

        // 3. Verificação de Limite de Faltas (Reprovação por Falta no SIGA - Regra 25% + Perda de Exame Final)
        for (subject in subjectsToday) {
            val simulatedAbsences = subject.currentAbsences + subject.classesPerDay
            if (simulatedAbsences > subject.maxAbsences) {
                val extensionWarning = if (subject.isExtension) {
                    " ⚠️ Atenção: Matéria de Extensão não aceita Regime Domiciliar!"
                } else {
                    ""
                }
                val customMessage = listOf(
                    "Você foi reprovado por falta!",
                    "Continue? (0 créditos)",
                    "Missão Falhada: O sono venceu o semestre"
                ).random() + " Limite ultrapassado em '${subject.name}' ($simulatedAbsences/${subject.maxAbsences} faltas). A reprovação por frequência bloqueia o Exame Final.$extensionWarning"
                return SkipVerdict.ForbiddenAbsenceLimit(
                    subject.name,
                    subject.currentAbsences,
                    subject.maxAbsences,
                    customMessage
                )
            }
        }

        // 4. Verificação da margem fixa de segurança (75% do limite de faltas)
        val excuse = getRandomExcuse()
        for (subject in subjectsToday) {
            val dangerThreshold = (subject.maxAbsences * 0.75f).toInt()
            val simulatedAbsences = subject.currentAbsences + subject.classesPerDay
            if (simulatedAbsences > dangerThreshold) {
                val messages = if (subject.isExtension) {
                    listOf(
                        "ZONA DE RISCO EXTENSIONISTA! '${subject.name}' exige 10% da carga do curso (PPC R-06) e NÃO ACEITA REGIME DOMICILIAR. Se faltar hoje, o sufoco será duplo!",
                        "CUIDADO EXTENSIONISTA! '${subject.name}' é matéria de extensão. Se estourar as faltas, terá que refazer todas as 60h práticas no semestre que vem!",
                        "ATENÇÃO! '${subject.name}' está perto do limite (${subject.currentAbsences}/${subject.maxAbsences}). Disciplinas extensionistas não têm regime abonado!"
                    )
                } else {
                    listOf(
                        "Por sua conta e risco! Sem provas hoje, mas faltar em '${subject.name}' vai te deixar a 1 passo de perder o Exame Final (${subject.currentAbsences}/${subject.maxAbsences}). Lembre-se: atestados devem ser protocolados em até 5 dias corridos!",
                        "CUIDADO, GUERREIRO! A corda tá esticando em '${subject.name}'. Se reprovar por falta, a Portaria da Fatec proíbe fazer Exame Final!",
                        "ZONA DE RISCO! Se faltar hoje em '${subject.name}', nas próximas semanas até dor de dente vai precisar de atestado formal em até 5 dias úteis!"
                    )
                }
                return SkipVerdict.WarningSafetyMargin(
                    subject.name,
                    subject.currentAbsences,
                    subject.maxAbsences,
                    "${messages.random()} Lembre-se: atestado médico curto só reagenda prova, mas a falta continua contando no SIGA!",
                    excuse
                )
            }
        }

        // 5. Veredito Verde (Liberado!)
        val allowedMessages = listOf(
            "Dia favorável! Sem provas críticas e faltas sob total controle. A cama te chama, guerreiro!",
            "LIBERADO COM LOUVOR! Faltar hoje é 100% taticamente seguro. O SIGA nem vai notar sua ausência!",
            "Pode meter o louco hoje! Seu saldo de faltas tá limpo e não tem nada crítico no radar. Bom descanso!",
            "Hoje a aula pode esperar e o cobertor venceu por nocaute técnico."
        )
        return SkipVerdict.Allowed(
            customMessage = allowedMessages.random(),
            excuse = excuse
        )
    }

    /**
     * Sincroniza a grade da Fatec, feriados e desculpas a partir do endpoint JSON.
     * Caso falhe por conexão ou URL inválida, garante fallback usando os dados padrão.
     */
    suspend fun syncFromApi(targetSemester: Int, customUrl: String? = null): Result<String> {
        return try {
            var endpoint = customUrl?.ifBlank { null } ?: UserPreferencesRepository.DEFAULT_API_URL
            // Sanitiza caso o usuário cole a URL da página do Gist em vez da Raw
            if (endpoint.contains("gist.github.com/") && !endpoint.contains("/raw")) {
                endpoint = endpoint.replace("gist.github.com/", "gist.githubusercontent.com/").trimEnd('/') + "/raw/fatec_api.json"
            }

            val apiData = try {
                apiService.getFatecData(endpoint)
            } catch (e: Exception) {
                null
            }

            if (apiData != null) {
                // Atualiza / insere feriados
                val holidays = apiData.semesterInfo?.holidays?.map {
                    EventEntity(
                        title = it.name,
                        date = it.date,
                        type = "HOLIDAY",
                        isCritical = false
                    )
                }
                if (!holidays.isNullOrEmpty()) {
                    eventDao.clearHolidays()
                    eventDao.insertEvents(holidays)
                }

                // Atualiza / insere desculpas
                val excuses = apiData.excuses?.map { ExcuseEntity(text = it) }
                if (!excuses.isNullOrEmpty()) {
                    excuseDao.insertExcuses(excuses)
                }

                // Atualiza matérias do semestre alvo ou de todos os semestres
                apiData.curriculums?.forEach { (semesterKey, subjectDtos) ->
                    val semInt = semesterKey.toIntOrNull() ?: 1
                    val existing = subjectDao.getSubjectsBySemesterList(semInt)
                    val existingMap = existing.associateBy { it.code }

                    val newCodes = subjectDtos.map { it.code }.toSet()
                    val toDelete = existing.filter { !it.isCustomized && it.code !in newCodes }
                    toDelete.forEach { subjectDao.deleteSubjectById(it.id) }

                    val subjectEntities = subjectDtos.map { dto ->
                        val prev = existingMap[dto.code]
                        
                        // Limpeza automática de nome e extração de professor
                        var cleanName = dto.name.trim()
                        var extractedProf = ""
                        val parensIndex = cleanName.lastIndexOf("(")
                        if (parensIndex != -1 && cleanName.endsWith(")")) {
                            extractedProf = cleanName.substring(parensIndex + 1, cleanName.length - 1).trim()
                            cleanName = cleanName.substring(0, parensIndex).trim()
                        }
                        
                        // Mapeia string "Segunda, Terça" para inteiros
                        val mapDias = mapOf(
                            "segunda" to 1, "terça" to 2, "terca" to 2,
                            "quarta" to 3, "quinta" to 4, "sexta" to 5, "sábado" to 6, "sabado" to 6
                        )
                        val daysParsed = dto.dayOfWeek.split(",")
                            .map { it.trim().lowercase() }
                            .mapNotNull { mapDias[it] }

                        SubjectEntity(
                            id = prev?.id ?: 0,
                            code = dto.code,
                            name = cleanName,
                            semester = semInt,
                            dayOfWeek = daysParsed.firstOrNull() ?: 1,
                            daysOfWeek = daysParsed.joinToString(","),
                            classesPerDay = dto.classesPerDay,
                            currentAbsences = prev?.currentAbsences ?: 0,
                            maxAbsences = dto.maxAbsences,
                            professor = if (prev == null || prev.professor.isBlank() || prev.professor == "Docente não definido") extractedProf else prev.professor,
                            classTime = dto.schedule.ifBlank { "07:40 - 11:20" },
                            isCustomized = prev?.isCustomized ?: false,
                            isExtension = dto.isExtension
                        )
                    }
                    subjectDao.insertSubjects(subjectEntities)
                }

                Result.success("Sincronização com a API concluída com sucesso! Grade e eventos atualizados.")
            } else {
                // Fallback offline seguro com a grade oficial exata do PDF (preserva matérias de DP customizadas)
                subjectDao.clearAllNonCustomizedSubjects()
                subjectDao.insertSubjects(DefaultFatecData.getDefaultSubjects())
                eventDao.clearHolidays()
                eventDao.insertEvents(DefaultFatecData.getDefaultHolidays())
                excuseDao.insertExcuses(DefaultFatecData.getDefaultExcuses())
                Result.success("Modo offline: Grade oficial de DSM de 2026 recarregada com sucesso!")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
