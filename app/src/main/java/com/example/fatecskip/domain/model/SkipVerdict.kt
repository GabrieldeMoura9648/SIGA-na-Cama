package com.example.fatecskip.domain.model

import androidx.compose.ui.graphics.Color

sealed class SkipVerdict(
    val title: String,
    val description: String,
    val badgeText: String,
    val containerColor: Color,
    val contentColor: Color,
    val emoji: String,
    val suggestedExcuse: String? = null
) {
    /**
     * Veredito Azul: Domingo, dia sem matérias cadastradas para o aluno ou Feriado oficial.
     */
    data class NoClassOrHoliday(
        val reason: String,
        val customMessage: String
    ) : SkipVerdict(
        title = "PODE VOLTAR A DORMIR!",
        description = customMessage,
        badgeText = "FOLGA MERECIDA",
        containerColor = Color(0xFF1E88E5), // Azul vibrante
        contentColor = Color.White,
        emoji = "🛌"
    )

    /**
     * Veredito Vermelho: Evento crítico no dia (P1, P2, Projeto Integrador, etc.)
     */
    data class ForbiddenCriticalEvent(
        val eventTitle: String,
        val customMessage: String
    ) : SkipVerdict(
        title = "LEVANTA AGORA!",
        description = customMessage,
        badgeText = "EVENTO CRÍTICO",
        containerColor = Color(0xFFD32F2F), // Vermelho alerta
        contentColor = Color.White,
        emoji = "🚨"
    )

    /**
     * Veredito Vermelho: Faltar hoje estoura o limite de 25% de faltas no SIGA.
     */
    data class ForbiddenAbsenceLimit(
        val subjectName: String,
        val currentAbsences: Int,
        val maxAbsences: Int,
        val customMessage: String
    ) : SkipVerdict(
        title = "GAME OVER",
        description = customMessage,
        badgeText = "REPROVADO POR FREQUÊNCIA",
        containerColor = Color(0xFFC2185B), // Vermelho / Vinho de perigo
        contentColor = Color.White,
        emoji = "⚠️"
    )

    /**
     * Veredito Amarelo: Faltar hoje ultrapassa a margem fixa de segurança.
     */
    data class WarningSafetyMargin(
        val subjectName: String,
        val currentAbsences: Int,
        val maxAbsences: Int,
        val customMessage: String,
        val excuse: String
    ) : SkipVerdict(
        title = "CUIDADO, GUERREIRO!",
        description = customMessage,
        badgeText = "ZONA DE PERIGO",
        containerColor = Color(0xFFFFA000), // Âmbar / Amarelo forte
        contentColor = Color.Black,
        emoji = "⚠️",
        suggestedExcuse = excuse
    )

    /**
     * Veredito Verde: Sem provas críticas e saldo de faltas seguro.
     */
    data class Allowed(
        val customMessage: String,
        val excuse: String
    ) : SkipVerdict(
        title = "DIA FAVORÁVEL!",
        description = customMessage,
        badgeText = "LIBERADO PARA DORMIR",
        containerColor = Color(0xFF2E7D32), // Verde aprovação
        contentColor = Color.White,
        emoji = "🎉",
        suggestedExcuse = excuse
    )
}
