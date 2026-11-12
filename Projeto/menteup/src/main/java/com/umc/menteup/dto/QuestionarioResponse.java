package com.umc.menteup.dto;

import com.umc.menteup.model.ProgressoAtividade;

import java.util.List;

public record QuestionarioResponse (
        AtividadeResponse atividade,
        List<PerguntaResponse> perguntas,
        ResultadoAtividadeResponse meuResultado,
        ProgressoAtividadeResponse meuProgresso
){
}
