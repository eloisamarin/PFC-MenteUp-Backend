package com.umc.menteup.service;

import com.umc.menteup.model.GamificacaoUsuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.umc.menteup.model.TipoRegraConquista.*;
import static org.junit.jupiter.api.Assertions.*;

///Testes das regras do Intensivão e das conquistas, sem banco
class ConquistaRegrasTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    private static GamificacaoUsuario estado(int dias, int maior, LocalDate ultimo) {
        GamificacaoUsuario g = new GamificacaoUsuario();
        g.setDiasConsecutivos(dias);
        g.setMaiorSequencia(maior);
        g.setUltimoAcesso(ultimo);
        return g;
    }

    @Test
    void primeiroAcessoIniciaSequenciaEmUm() {
        GamificacaoUsuario g = estado(0, 0, null);
        assertTrue(IntensivaoService.atualizarSequencia(g, HOJE));
        assertEquals(1, g.getDiasConsecutivos());
        assertEquals(1, g.getMaiorSequencia());
        assertEquals(HOJE, g.getUltimoAcesso());
    }

    @Test
    void acessoNoMesmoDiaNaoAltera() {
        GamificacaoUsuario g = estado(4, 4, HOJE);
        assertFalse(IntensivaoService.atualizarSequencia(g, HOJE));
        assertEquals(4, g.getDiasConsecutivos());
    }

    @Test
    void acessoNoDiaSeguinteIncrementa() {
        GamificacaoUsuario g = estado(4, 4, HOJE.minusDays(1));
        IntensivaoService.atualizarSequencia(g, HOJE);
        assertEquals(5, g.getDiasConsecutivos());
        assertEquals(5, g.getMaiorSequencia());
    }

    @Test
    void doisOuMaisDiasSemAcessoReiniciaMasPreservaRecorde() {
        GamificacaoUsuario g = estado(12, 12, HOJE.minusDays(2));
        IntensivaoService.atualizarSequencia(g, HOJE);
        assertEquals(1, g.getDiasConsecutivos());
        assertEquals(12, g.getMaiorSequencia());
    }

    @Test
    void dataAnteriorAoUltimoAcessoEIgnorada() {
        GamificacaoUsuario g = estado(3, 3, HOJE);
        assertFalse(IntensivaoService.atualizarSequencia(g, HOJE.minusDays(1)));
        assertEquals(3, g.getDiasConsecutivos());
        assertEquals(HOJE, g.getUltimoAcesso());
    }

    @Test
    void valorAtualDeCadaTipoDeRegra() {
        var ind = new ConquistaService.Indicadores(12, 2, 1050, 6, 3, 5);

        assertEquals(1050, ConquistaService.valorAtual(XP_ACUMULADO, 1000, ind));
        assertEquals(12, ConquistaService.valorAtual(CONTAGEM_ATIVIDADES, 10, ind));
        assertEquals(2, ConquistaService.valorAtual(APROVEITAMENTO, 1, ind));
        assertEquals(6, ConquistaService.valorAtual(SEQUENCIA_DIAS, 5, ind));
        assertEquals(5, ConquistaService.valorAtual(EVOLUCAO_RANKING, 5, ind));
        assertEquals(0, ConquistaService.valorAtual(POSICAO_RANKING, 1, ind)); ///está em 3º
    }

    @Test
    void primeiroLugarAtingeAMeta() {
        var lider = new ConquistaService.Indicadores(1, 0, 10, 1, 1, 0);
        var foraDoRanking = new ConquistaService.Indicadores(0, 0, 0, 1, null, 0);

        assertEquals(1, ConquistaService.valorAtual(POSICAO_RANKING, 1, lider));
        assertEquals(0, ConquistaService.valorAtual(POSICAO_RANKING, 1, foraDoRanking));
    }
}
