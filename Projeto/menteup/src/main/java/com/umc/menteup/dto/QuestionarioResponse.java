package com.umc.menteup.dto;

import java.util.List;

public record QuestionarioResponse (
        AtividadeResponse atividade,
        List<PerguntaResponse> perguntas,
        ResultadoAtividadeResponse meuResultado,
        ProgressoAtividadeResponse meuProgresso
){
}
