package com.umc.menteup.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.menteup.dto.RankingResponse;

// Regra de privacidade do ranking que o aluno vê
class RankingServiceTest {

    private static List<RankingResponse> rankingCom(int quantidade) {
        List<RankingResponse> ranking = new ArrayList<>();
        for (long i = 1; i <= quantidade; i++) {
            ranking.add(new RankingResponse(i, "Aluno " + i, 1000 - i, 1L, "PANDA"));
        }
        return ranking;
    }

    @Test
    @DisplayName("os primeiros colocados aparecem com nome e os demais ficam ocultos")
    void ocultaQuemEstaForaDoTop() {
        List<RankingResponse> resultado = RankingService.ocultarNomes(rankingCom(12), 1L);

        assertThat(resultado).hasSize(12);
        assertThat(resultado.subList(0, RankingService.NOMES_VISIVEIS_PARA_ALUNO))
                .allSatisfy(r -> assertThat(r.usuarioId()).isNotNull());
        assertThat(resultado.get(10)).satisfies(r -> {
            assertThat(r.usuarioId()).isNull();
            assertThat(r.nomeUsuario()).isEqualTo(RankingService.NOME_OCULTO);
            assertThat(r.avatar()).isNull();
            assertThat(r.pontuacaoTotal()).isEqualTo(989L);
        });
    }

    @Test
    @DisplayName("o próprio aluno sempre aparece com nome, mesmo fora do top")
    void alunoSempreSeVe() {
        List<RankingResponse> resultado = RankingService.ocultarNomes(rankingCom(12), 12L);

        assertThat(resultado.get(11).usuarioId()).isEqualTo(12L);
        assertThat(resultado.get(11).nomeUsuario()).isEqualTo("Aluno 12");
        assertThat(resultado.get(10).usuarioId()).isNull();
    }

    @Test
    @DisplayName("ranking pequeno fica inteiro")
    void rankingPequenoNaoMuda() {
        List<RankingResponse> ranking = rankingCom(3);

        assertThat(RankingService.ocultarNomes(ranking, 2L)).isEqualTo(ranking);
    }
}
