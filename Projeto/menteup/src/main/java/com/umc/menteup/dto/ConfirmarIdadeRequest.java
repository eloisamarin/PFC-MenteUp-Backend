package com.umc.menteup.dto;

import java.time.LocalDate;

// POST /usuarios/me/idade: aluno com conta antiga informa a data de nascimento (não é guardada)
public record ConfirmarIdadeRequest(LocalDate dataNascimento) {
}
