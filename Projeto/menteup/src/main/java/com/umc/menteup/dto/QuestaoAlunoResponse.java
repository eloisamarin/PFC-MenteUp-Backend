package com.umc.menteup.dto;

public record QuestaoAlunoResponse(
        String enunciado,
        Boolean correta,
        String respostaAluno,
        String respostaCorreta
) {
}
