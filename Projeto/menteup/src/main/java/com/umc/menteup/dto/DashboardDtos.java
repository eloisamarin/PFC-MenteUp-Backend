package com.umc.menteup.dto;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    // Big Numbers. Os percentuais vêm null quando ainda não há dados
    public record Resumo(
            long alunos,
            long professores,
            long turmas,
            long atividadesCriadas,
            long atividadesConcluidas,
            Double acertoAtividades,
            long desafiosRealizados,
            Double acertoDesafios
    ) {
    }

    // Um nome e uma quantidade (gráficos de pizza e de barras)
    public record Item(String nome, long quantidade) {
    }

    public record AcertoTurma(String turma, Double acerto) {
    }
    public record ConclusoesDia(String dia, long concluidas) {
    }

    public record DesafiosDia(String dia, long acertos, long erros) {
    }

    public record AcoesDia(String dia, long professores, long alunos) {
    }

    // ------------------------------------------------------------- Painel do sistema (GET /painel)
    // Cada perfil vê só o seu recorte: aluno, os próprios dados; professor, as turmas dele e os
    // alunos dessas turmas; administrador, tudo. Filtros: período, turma e (professor/admin) aluno.

    // Uma opção de filtro (turma ou aluno)
    public record Opcao(Long id, String nome) {
    }

    // Números do recorte. Os percentuais vêm null quando ainda não há dados
    public record ResumoPainel(
            long turmas,
            long alunos,
            long atividadesPublicadas,
            long atividadesConcluidas,
            Double acertoAtividades,
            long pontosAtividades,
            long respostasDesafios,
            Double acertoDesafios,
            long pontosDesafios
    ) {
    }

    // Uma linha da tabela de alunos (professor e administrador)
    public record AlunoPainel(
            Long id,
            String nome,
            long atividadesConcluidas,
            Double acerto,
            long pontosAtividades,
            long respostasDesafios,
            long pontosDesafios
    ) {
    }

    public record Painel(
            String perfil,
            int dias,
            Long turmaId,
            Long alunoId,
            java.util.List<Opcao> turmas,
            java.util.List<Opcao> alunos,
            ResumoPainel resumo,
            java.util.List<ConclusoesDia> conclusoesPorDia,
            java.util.List<AcertoTurma> acertoPorTurma,
            java.util.List<AlunoPainel> desempenhoAlunos
    ) {
    }
}
