package com.umc.menteup.dto;

import com.umc.menteup.model.StatusAtividade;
import com.umc.menteup.model.TipoAtividade;

import java.time.LocalDateTime;
import java.util.List;

public record AtividadeResponse(
        Long id,
        String titulo,
        String descricao,
        TurmaResponse turma,
        List<TurmaResponse> turmas,
        Integer pontuacao,
        StatusAtividade status,
        UsuarioResponse usuario,
        LocalDateTime dataCriacao,
        TipoAtividade tipo
){
}
