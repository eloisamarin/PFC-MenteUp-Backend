package com.umc.menteup.dto;

/*
 * Resposta de GET /turmas/{id}/exclusao: diz se a turma pode ser excluída e, se não puder,
 * o que a prende (alunos vinculados ou histórico de desafios e resultados) e por quê.
 * A tela usa antes de excluir, para avisar e oferecer o arquivamento.
 */
public record ExclusaoTurmaResponse(
        boolean podeExcluir,
        int alunos,
        long desafios,
        long resultados,
        long atividadesEmAndamento,
        String motivo
) {
}
