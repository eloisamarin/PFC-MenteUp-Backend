package com.umc.menteup.dto;

import com.umc.menteup.model.PerguntaDesafio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PerguntaDesafioRequest(
        @NotBlank(message = "O enunciado é obrigatório")
        @Size(max = 255, message = "O enunciado deve ter no máximo 255 caracteres")
        String enunciado,

        @NotNull(message = "O tempo da pergunta é obrigatório")
        @Min(value = PerguntaDesafio.TEMPO_MINIMO_SEGUNDOS, message = "O tempo mínimo é de 5 segundos")
        @Max(value = PerguntaDesafio.TEMPO_MAXIMO_SEGUNDOS, message = "O tempo máximo é de 300 segundos")
        Integer tempoSegundos,

        @NotNull(message = "As alternativas são obrigatórias")
        @Size(min = 2, max = 4, message = "A pergunta deve ter de 2 a 4 alternativas")
        List<@Valid @NotNull AlternativaRequest> alternativas
) {
}
