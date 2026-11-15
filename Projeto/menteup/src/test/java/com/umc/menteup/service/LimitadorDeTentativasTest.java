package com.umc.menteup.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LimitadorDeTentativasTest {

    // Relógio que o teste adianta quando quiser
    private static final class RelogioDeTeste extends Clock {
        private Instant agora = Instant.parse("2026-11-15T12:00:00Z");

        void adiantar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    @Test
    void recusaDepoisDoLimiteELiberaQuandoAJanelaPassa() {
        RelogioDeTeste relogio = new RelogioDeTeste();
        LimitadorDeTentativas limitador = new LimitadorDeTentativas(2, Duration.ofMinutes(15), relogio);

        assertThat(limitador.tentar("1.1.1.1")).isTrue();
        assertThat(limitador.tentar("1.1.1.1")).isTrue();
        assertThat(limitador.tentar("1.1.1.1")).isFalse();

        relogio.adiantar(Duration.ofMinutes(16));
        assertThat(limitador.tentar("1.1.1.1")).isTrue();
    }

    @Test
    void cadaChaveTemSuaPropriaContagem() {
        LimitadorDeTentativas limitador = new LimitadorDeTentativas(1, Duration.ofHours(1), new RelogioDeTeste());

        assertThat(limitador.tentar("a@teste.com")).isTrue();
        assertThat(limitador.tentar("a@teste.com")).isFalse();
        assertThat(limitador.tentar("b@teste.com")).isTrue();
    }
}
