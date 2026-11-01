package com.umc.menteup.dto;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.ResultadoLog;

import java.time.LocalDateTime;

public record LogSistemaResponse (
        Long id,
        Long usuarioId,
        String nomeUsuario,
        AcaoLog acao,
        EntidadeLog entidade,
        Long entidadeId,
        LocalDateTime dataHora,
        ResultadoLog resultado,
        String descricao,
        String ip,
        String dados
) {
}
