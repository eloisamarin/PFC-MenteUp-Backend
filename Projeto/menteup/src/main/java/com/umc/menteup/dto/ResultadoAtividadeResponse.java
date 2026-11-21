package com.umc.menteup.dto;

import java.time.LocalDateTime;

/// Resultado da atividade para o aluno, com o comentário do professor quando houver
public record ResultadoAtividadeResponse(
        Long atividadeId,
        String titulo,
        Integer totalQuestoes,
        Integer acertos,
        Integer pontuacao,
        String comentarioProfessor,
        String comentadoPor,
        LocalDateTime comentadoEm
) {
}
