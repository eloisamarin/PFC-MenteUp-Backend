package com.umc.menteup.dto;

import com.umc.menteup.model.Conquista;
import com.umc.menteup.model.UsuarioConquista;

import java.time.LocalDateTime;

///Uma conquista e o progresso do aluno nela
public record ConquistaAlunoResponse(
        Long id,
        String codigo,
        String nome,
        String descricao,
        String categoria,
        String icone,
        String raridade,
        boolean secreta,
        int recompensaXp,          ///XP ganho ou a ganhar
        boolean desbloqueada,
        int progressoAtual,
        int meta,
        int percentual,            ///0 a 100
        LocalDateTime dataDesbloqueio
) {

    private static final String NOME_SECRETO = "Conquista secreta";
    private static final String DESCRICAO_SECRETA = "Continue jogando para descobrir.";

    ///uc pode ser null (aluno ainda sem progresso). ocultarSecreta esconde as secretas não ganhas
    public static ConquistaAlunoResponse de(Conquista c, UsuarioConquista uc, boolean ocultarSecreta) {
        boolean desbloqueada = uc != null && Boolean.TRUE.equals(uc.getDesbloqueada());
        int meta = uc != null ? uc.getMeta() : c.getMeta();
        int progresso = uc != null ? uc.getProgressoAtual() : 0;
        int percentual = desbloqueada ? 100 : (meta > 0 ? Math.min(100, progresso * 100 / meta) : 0);
        boolean oculta = ocultarSecreta && Boolean.TRUE.equals(c.getSecreta()) && !desbloqueada;

        return new ConquistaAlunoResponse(
                c.getId(),
                c.getCodigo(),
                oculta ? NOME_SECRETO : c.getNome(),
                oculta ? DESCRICAO_SECRETA : c.getDescricao(),
                c.getCategoria().name(),
                oculta ? null : c.getIcone(),
                c.getRaridade().name(),
                Boolean.TRUE.equals(c.getSecreta()),
                desbloqueada ? uc.getXpConcedido() : c.getRecompensaXp(),
                desbloqueada,
                desbloqueada ? meta : progresso,
                meta,
                percentual,
                desbloqueada ? uc.getDataDesbloqueio() : null
        );
    }
}
