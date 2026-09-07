package com.umc.menteup.dto;

import java.util.List;

public record AlternativaResponse(
        Long id,
        String texto,
        Boolean correta
) {
}
