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
}
