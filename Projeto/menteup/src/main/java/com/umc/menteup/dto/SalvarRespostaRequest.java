package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SalvarRespostaRequest(
        @NotNull ( message = "perguntaId é obrigatório" )
        Long perguntaId,

        Long alternativaId,

        List<Long> alternativasIds
) {
    @JsonCreator
    public SalvarRespostaRequest {
    }

    public SalvarRespostaRequest(Long perguntaId, Long alternativaId) {
        this(perguntaId, alternativaId, null);
    }

    @JsonIgnore
    @AssertTrue(message = "Informe alternativaId ou alternativasIds")
    public boolean isRespostaInformada() {
        return alternativaId != null || (alternativasIds != null && !alternativasIds.isEmpty());
    }
}
