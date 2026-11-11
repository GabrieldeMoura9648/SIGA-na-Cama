package com.example.fatecskip.data.local

import com.example.fatecskip.data.local.entities.EventEntity
import com.example.fatecskip.data.local.entities.ExcuseEntity
import com.example.fatecskip.data.local.entities.SubjectEntity

/**
 * Grade Oficial Fatec - Horário de Aulas - 1º Semestre de 2026 - DSM (Matutino)
 * Extraído diretamente do PDF oficial da faculdade com os nomes exatos das matérias e professores.
 */
object DefaultFatecData {

    fun getSemesterLabel(semester: Int): String? = listOf(
        "1º Semestre (Ainda com esperança)",
        "2º Semestre (Descobrindo onde se meteu)",
        "3º Semestre (Amigo íntimo do café)",
        "4º Semestre (Milagreiro da nota 6)",
        "5º Semestre (Refém do Projeto Integrador)",
        "6º Semestre (Apenas sobrevivendo)"
    ).getOrNull(semester - 1)

    fun getDefaultSubjects(): List<SubjectEntity> {
        val list = mutableListOf<SubjectEntity>()

        fun extractProfAndCleanName(originalName: String): Pair<String, String> {
            var cleanName = originalName.trim()
            var extractedProf = ""
            val parensIndex = cleanName.lastIndexOf("(")
            if (parensIndex != -1 && cleanName.endsWith(")")) {
                extractedProf = cleanName.substring(parensIndex + 1, cleanName.length - 1).trim()
                cleanName = cleanName.substring(0, parensIndex).trim()
            }
            return Pair(cleanName, extractedProf)
        }

        // ==========================================
        // 1º SEMESTRE - MATUTINO DSM
        // ==========================================
        val dwi01 = extractProfAndCleanName("Desenvolvimento Web I (Hélio Cesar)")
        list.add(SubjectEntity(code = "DWI01", name = dwi01.first, professor = dwi01.second, semester = 1, dayOfWeek = 1, daysOfWeek = "1,2", classTime = "07:40 - 09:20", classesPerDay = 2, maxAbsences = 20))
        val alg01 = extractProfAndCleanName("Algoritmos e Lógica de Programação (Lúcio)")
        list.add(SubjectEntity(code = "ALG01", name = alg01.first, professor = alg01.second, semester = 1, dayOfWeek = 1, daysOfWeek = "1,3", classTime = "Seg 09:40-11:20 / Qua 11:20-13:00", classesPerDay = 2, maxAbsences = 20))
        val mbd01 = extractProfAndCleanName("Modelagem de Banco de Dados (Sueli Ap.)")
        list.add(SubjectEntity(code = "MBD01", name = mbd01.first, professor = mbd01.second, semester = 1, dayOfWeek = 2, daysOfWeek = "2", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val sop01 = extractProfAndCleanName("Sistemas Operacionais e Redes de Comp. (Silas / Ismael)")
        list.add(SubjectEntity(code = "SOP01", name = sop01.first, professor = sop01.second, semester = 1, dayOfWeek = 3, daysOfWeek = "3", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val es101 = extractProfAndCleanName("Engenharia de Software I (Rháleff)")
        list.add(SubjectEntity(code = "ES101", name = es101.first, professor = es101.second, semester = 1, dayOfWeek = 4, daysOfWeek = "4", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val ddg01 = extractProfAndCleanName("Design Digital (Patrícia)")
        list.add(SubjectEntity(code = "DDG01", name = ddg01.first, professor = ddg01.second, semester = 1, dayOfWeek = 5, daysOfWeek = "5", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))

        // ==========================================
        // 2º SEMESTRE - MATUTINO DSM
        // ==========================================
        val es201 = extractProfAndCleanName("Engenharia de Software II (Luiz Antonio / Marco A.)")
        list.add(SubjectEntity(code = "ES201", name = es201.first, professor = es201.second, semester = 2, dayOfWeek = 1, daysOfWeek = "1", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val edd01 = extractProfAndCleanName("Estrutura de Dados (Lúcio)")
        list.add(SubjectEntity(code = "EDD01", name = edd01.first, professor = edd01.second, semester = 2, dayOfWeek = 1, daysOfWeek = "1,3", classTime = "Seg 11:20-13:00 / Qua 07:40-09:20", classesPerDay = 2, maxAbsences = 20))
        val dw201 = extractProfAndCleanName("Desenvolvimento Web II (Bruno)")
        list.add(SubjectEntity(code = "DW201", name = dw201.first, professor = dw201.second, semester = 2, dayOfWeek = 2, daysOfWeek = "2", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val mpc01 = extractProfAndCleanName("Matemática para Computação (Rosângela)")
        list.add(SubjectEntity(code = "MPC01", name = mpc01.first, professor = mpc01.second, semester = 2, dayOfWeek = 3, daysOfWeek = "3", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val tpi01 = extractProfAndCleanName("Técnicas de Programação I (Marcos Vasconcelos)")
        list.add(SubjectEntity(code = "TPI01", name = tpi01.first, professor = tpi01.second, semester = 2, dayOfWeek = 4, daysOfWeek = "4", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val bdr01 = extractProfAndCleanName("Banco de Dados Relacional (Rafael Miranda / Renan)")
        list.add(SubjectEntity(code = "BDR01", name = bdr01.first, professor = bdr01.second, semester = 2, dayOfWeek = 5, daysOfWeek = "5", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))

        // ==========================================
        // 3º SEMESTRE - MATUTINO DSM
        // ==========================================
        val ihc01 = extractProfAndCleanName("Interação Humano Computador (Lúcio)")
        list.add(SubjectEntity(code = "IHC01", name = ihc01.first, professor = ihc01.second, semester = 3, dayOfWeek = 1, daysOfWeek = "1", classTime = "07:40 - 09:20", classesPerDay = 2, maxAbsences = 10))
        val gag01 = extractProfAndCleanName("Gestão Ágil de Projetos de Software (Bruno)")
        list.add(SubjectEntity(code = "GAG01", name = gag01.first, professor = gag01.second, semester = 3, dayOfWeek = 1, daysOfWeek = "1", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val bdn01 = extractProfAndCleanName("Banco de Dados Não Relacional (Marcos Vasconcelos)")
        list.add(SubjectEntity(code = "BDN01", name = bdn01.first, professor = bdn01.second, semester = 3, dayOfWeek = 2, daysOfWeek = "2", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val dw301 = extractProfAndCleanName("Desenvolvimento Web III (Vinicius Heltai)")
        list.add(SubjectEntity(code = "DW301", name = dw301.first, professor = dw301.second, semester = 3, dayOfWeek = 3, daysOfWeek = "3", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val alg03 = extractProfAndCleanName("Álgebra Linear (Rosângela)")
        list.add(SubjectEntity(code = "ALG03", name = alg03.first, professor = alg03.second, semester = 3, dayOfWeek = 4, daysOfWeek = "4", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val ing01 = extractProfAndCleanName("Inglês I (Luciano / Valteir)")
        list.add(SubjectEntity(code = "ING01", name = ing01.first, professor = ing01.second, semester = 3, dayOfWeek = 5, daysOfWeek = "5", classTime = "07:40 - 09:20", classesPerDay = 2, maxAbsences = 10))
        val tp201 = extractProfAndCleanName("Técnicas de Programação II (Vinicius)")
        list.add(SubjectEntity(code = "TP201", name = tp201.first, professor = tp201.second, semester = 3, dayOfWeek = 5, daysOfWeek = "5", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))

        // ==========================================
        // 4º SEMESTRE - MATUTINO DSM
        // ==========================================
        val iot01 = extractProfAndCleanName("Internet das Coisas e Aplicações (Rháleff / Ismael)")
        list.add(SubjectEntity(code = "IOT01", name = iot01.first, professor = iot01.second, semester = 4, dayOfWeek = 1, daysOfWeek = "1", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val ldw01 = extractProfAndCleanName("Laboratório de Desenv. Web (Edson)")
        list.add(SubjectEntity(code = "LDW01", name = ldw01.first, professor = ldw01.second, semester = 4, dayOfWeek = 2, daysOfWeek = "2", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val pdm01 = extractProfAndCleanName("Programação para Disp. Móveis I (Marcos Vasconcelos)")
        list.add(SubjectEntity(code = "PDM01", name = pdm01.first, professor = pdm01.second, semester = 4, dayOfWeek = 2, daysOfWeek = "2,4", classTime = "11:20 - 13:00", classesPerDay = 2, maxAbsences = 20))
        val iec01 = extractProfAndCleanName("Integração e Entrega Contínua (Rafael Lopes)")
        list.add(SubjectEntity(code = "IEC01", name = iec01.first, professor = iec01.second, semester = 4, dayOfWeek = 3, daysOfWeek = "3", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val est01 = extractProfAndCleanName("Estatística Aplicada (Diogo Soares)")
        list.add(SubjectEntity(code = "EST01", name = est01.first, professor = est01.second, semester = 4, dayOfWeek = 4, daysOfWeek = "4", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val pdm02 = extractProfAndCleanName("Programação para Disp. Móveis I (Marcos Vasconcelos)")
        list.add(SubjectEntity(code = "PDM02", name = pdm02.first, professor = pdm02.second, semester = 4, dayOfWeek = 4, daysOfWeek = "4", classTime = "11:20 - 13:00", classesPerDay = 2, maxAbsences = 20))
        val ing02 = extractProfAndCleanName("Inglês II (Luciano / Valteir)")
        list.add(SubjectEntity(code = "ING02", name = ing02.first, professor = ing02.second, semester = 4, dayOfWeek = 5, daysOfWeek = "5", classTime = "09:40 - 11:20", classesPerDay = 2, maxAbsences = 10))
        val ux001 = extractProfAndCleanName("Experiência Usuário (Carlo Ronny)")
        list.add(SubjectEntity(code = "UX001", name = ux001.first, professor = ux001.second, semester = 4, dayOfWeek = 5, daysOfWeek = "5", classTime = "11:20 - 13:00", classesPerDay = 2, maxAbsences = 10))

        // ==========================================
        // 5º SEMESTRE - MATUTINO DSM
        // ==========================================
        val frt01 = extractProfAndCleanName("Fundamentos da Redação Técnica (Thierri)")
        list.add(SubjectEntity(code = "FRT01", name = frt01.first, professor = frt01.second, semester = 5, dayOfWeek = 1, daysOfWeek = "1", classTime = "07:40 - 09:20", classesPerDay = 2, maxAbsences = 10))
        val ing03 = extractProfAndCleanName("Inglês III (Amaury)")
        list.add(SubjectEntity(code = "ING03", name = ing03.first, professor = ing03.second, semester = 5, dayOfWeek = 1, daysOfWeek = "1", classTime = "09:40 - 11:20", classesPerDay = 2, maxAbsences = 10))
        val cpn01 = extractProfAndCleanName("Computação em Nuvem I (Odair / Renan)")
        list.add(SubjectEntity(code = "CPN01", name = cpn01.first, professor = cpn01.second, semester = 5, dayOfWeek = 2, daysOfWeek = "2", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val ldm01 = extractProfAndCleanName("Lab. de Des. p/ Disp. Móveis (Carlo Ronny)")
        list.add(SubjectEntity(code = "LDM01", name = ldm01.first, professor = ldm01.second, semester = 5, dayOfWeek = 3, daysOfWeek = "3", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val pdm03 = extractProfAndCleanName("Programação para Disp. Móveis II (Vinicius)")
        list.add(SubjectEntity(code = "PDM03", name = pdm03.first, professor = pdm03.second, semester = 5, dayOfWeek = 3, daysOfWeek = "3,5", classTime = "Qua 11:20-13:00 / Sex 07:40-09:20", classesPerDay = 2, maxAbsences = 20))
        val sda01 = extractProfAndCleanName("Segurança no Desenv. de Aplicações (Evandro / Ismael)")
        list.add(SubjectEntity(code = "SDA01", name = sda01.first, professor = sda01.second, semester = 5, dayOfWeek = 4, daysOfWeek = "4", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val pdm04 = extractProfAndCleanName("Programação para Disp. Móveis II (Vinicius)")
        list.add(SubjectEntity(code = "PDM04", name = pdm04.first, professor = pdm04.second, semester = 5, dayOfWeek = 5, daysOfWeek = "5", classTime = "Qua 11:20-13:00 / Sex 07:40-09:20", classesPerDay = 2, maxAbsences = 20))
        val apm01 = extractProfAndCleanName("Aprendizagem de Máquina (Rháleff / Marcelo)")
        list.add(SubjectEntity(code = "APM01", name = apm01.first, professor = apm01.second, semester = 5, dayOfWeek = 5, daysOfWeek = "5", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))

        // ==========================================
        // 6º SEMESTRE - MATUTINO DSM
        // ==========================================
        val ldm02 = extractProfAndCleanName("Lab. de Desenv. Multiplataforma (Rafael Guem / Renan)")
        list.add(SubjectEntity(code = "LDM02", name = ldm02.first, professor = ldm02.second, semester = 6, dayOfWeek = 1, daysOfWeek = "1", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20, isExtension = true))
        val etc01 = extractProfAndCleanName("Ética Profissional e Patente (Janaina)")
        list.add(SubjectEntity(code = "ETC01", name = etc01.first, professor = etc01.second, semester = 6, dayOfWeek = 2, daysOfWeek = "2", classTime = "09:40 - 11:20", classesPerDay = 2, maxAbsences = 10))
        val mnd01 = extractProfAndCleanName("Mineração de Dados (Edson)")
        list.add(SubjectEntity(code = "MND01", name = mnd01.first, professor = mnd01.second, semester = 6, dayOfWeek = 2, daysOfWeek = "2,3", classTime = "Ter 11:20-13:00 / Qua 07:40-09:20", classesPerDay = 2, maxAbsences = 20))
        val mnd02 = extractProfAndCleanName("Mineração de Dados (Edson)")
        list.add(SubjectEntity(code = "MND02", name = mnd02.first, professor = mnd02.second, semester = 6, dayOfWeek = 3, daysOfWeek = "3", classTime = "Ter 11:20-13:00 / Qua 07:40-09:20", classesPerDay = 2, maxAbsences = 20))
        val qts01 = extractProfAndCleanName("Qualidade e Testes de Software (Edson)")
        list.add(SubjectEntity(code = "QTS01", name = qts01.first, professor = qts01.second, semester = 6, dayOfWeek = 3, daysOfWeek = "3", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val cn201 = extractProfAndCleanName("Computação em Nuvem II (Renan)")
        list.add(SubjectEntity(code = "CN201", name = cn201.first, professor = cn201.second, semester = 6, dayOfWeek = 4, daysOfWeek = "4", classTime = "09:40 - 13:00", classesPerDay = 4, maxAbsences = 20))
        val pln01 = extractProfAndCleanName("Processamento de Linguagem Natural (PLN) (Andrea)")
        list.add(SubjectEntity(code = "PLN01", name = pln01.first, professor = pln01.second, semester = 6, dayOfWeek = 5, daysOfWeek = "5", classTime = "07:40 - 11:20", classesPerDay = 4, maxAbsences = 20))
        val ing04 = extractProfAndCleanName("Inglês IV (Luciano / Valteir)")
        list.add(SubjectEntity(code = "ING04", name = ing04.first, professor = ing04.second, semester = 6, dayOfWeek = 5, daysOfWeek = "5", classTime = "11:20 - 13:00", classesPerDay = 2, maxAbsences = 10))

        return list
    }

    fun getDefaultHolidays(): List<EventEntity> {
        return listOf(
            EventEntity(title = "Independência do Brasil", date = "2026-09-07", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Nossa Sra. Aparecida (Padroeira)", date = "2026-10-12", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Dia do Professor (Folga Fatec!)", date = "2026-10-15", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Dia do Servidor Público", date = "2026-10-28", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Finados", date = "2026-11-02", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Proclamação da República", date = "2026-11-15", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Dia da Consciência Negra", date = "2026-11-20", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Recesso de Fim de Ano", date = "2026-12-24", type = "HOLIDAY", isCritical = false),
            EventEntity(title = "Natal", date = "2026-12-25", type = "HOLIDAY", isCritical = false)
        )
    }

    fun getDefaultExcuses(): List<ExcuseEntity> {
        return listOf(
            ExcuseEntity(text = "Meu cachorro comeu meu bilhete único."),
            ExcuseEntity(text = "Deu conflito de merge no meu despertador hoje."),
            ExcuseEntity(text = "Fiquei preso num loop infinito tentando sair da cama."),
            ExcuseEntity(text = "O ônibus passou direto e levou minha vontade de estudar junto."),
            ExcuseEntity(text = "Tive que ficar em casa pra atualizar o Windows aos 99%."),
            ExcuseEntity(text = "A linha do trem quebrou e tive que voltar pra dormir."),
            ExcuseEntity(text = "Minha bateria social descarregou e a Fatec não tem tomada bivolt."),
            ExcuseEntity(text = "Deu NullPointerException na minha motivação nesta manhã."),
            ExcuseEntity(text = "O SIGA avisou que hoje é melhor descansar e recuperar as energias."),
            ExcuseEntity(text = "Estou realizando testes de estresse no meu colchão."),
            ExcuseEntity(text = "Fui compilar o café da manhã e deu Timeout."),
            ExcuseEntity(text = "Fui dar git push sem revisar o código e deu merge conflict."),
            ExcuseEntity(text = "Atestado de um dia não é passe livre: no SIGA, a falta continua no placar!"),
            ExcuseEntity(text = "Perder o Exame Final por falta é o único final que nem uma recuperação de código conserta.")
        )
    }
}
