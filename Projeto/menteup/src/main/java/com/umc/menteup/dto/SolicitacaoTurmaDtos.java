package com.umc.menteup.dto;

import com.umc.menteup.model.SolicitacaoTurma;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/*
 * Formatos usados nas solicitações de entrada e saída de turma.
 */
public final class SolicitacaoTurmaDtos {

    private SolicitacaoTurmaDtos() {
    }

    // POST /alunos/me/solicitacoes
    public record NovaSolicitacao(
            @NotNull(message = "Informe a turma")
            Long turmaId,

            @NotNull(message = "Informe se é para entrar (ENTRADA) ou sair (SAIDA)")
            SolicitacaoTurma.Tipo tipo
    ) {
    }

    public record Solicitacao(
            Long id,
            Long turmaId,
            String turmaNome,
            Long alunoId,
            String alunoNome,
            SolicitacaoTurma.Tipo tipo,
            SolicitacaoTurma.Status status,
            LocalDateTime criadaEm,
            LocalDateTime respondidaEm
    ) {
        public static Solicitacao de(SolicitacaoTurma s) {
            return new Solicitacao(
                    s.getId(),
                    s.getTurma().getId(),
                    s.getTurma().getNome(),
                    s.getAluno().getId(),
                    s.getAluno().getNomeUsuario(),
                    s.getTipo(),
                    s.getStatus(),
                    s.getCriadaEm(),
                    s.getRespondidaEm());
        }
    }

    // Uma turma vista pelo aluno: se ele está nela e se há pedido aberto.
    // Não traz a lista de alunos da turma (privacidade).
    public record TurmaDoAluno(
            Long id,
            String nome,
            String professor,
            boolean matriculado,
            Long solicitacaoPendenteId,
            SolicitacaoTurma.Tipo solicitacaoPendenteTipo
    ) {
    }
}
