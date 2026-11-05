package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlternativaResponse(
        Long id,
        String texto,
        ///apenas na visão do professor
        Boolean correta,
        Integer ordem
) {
    ///Visão do aluno sem gabarito
    public AlternativaResponse(Long id, String texto) {
        this(id, texto, null, null);
    }
}
