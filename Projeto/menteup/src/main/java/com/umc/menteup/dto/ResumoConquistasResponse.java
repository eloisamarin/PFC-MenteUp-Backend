package com.umc.menteup.dto;

import java.util.List;

///Resumo da tela de conquistas de um aluno
public record ResumoConquistasResponse(
        Long usuarioId,
        long xpTotal,                 ///atividades + desafios + conquistas
        int conquistasDesbloqueadas,
        int totalConquistas,
        int diasConsecutivos,         ///Intensivão
        int maiorSequencia,
        List<ConquistaAlunoResponse> conquistas
) {
}
