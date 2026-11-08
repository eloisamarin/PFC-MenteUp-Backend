package com.umc.menteup.dto;

import com.umc.menteup.model.AvatarAnimal;
import jakarta.validation.constraints.NotNull;

public record AlterarAvatarRequest(
        @NotNull(message = "Escolha um avatar")
        AvatarAnimal avatar
) {
}
