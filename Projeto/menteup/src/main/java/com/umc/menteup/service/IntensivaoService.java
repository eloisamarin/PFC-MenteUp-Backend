package com.umc.menteup.service;

import com.umc.menteup.model.GamificacaoUsuario;
import com.umc.menteup.model.UsuarioConquista;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

///Intensivão: dias seguidos de login (não necessariamente de estudo)
///A sequência zera no próximo login depois de um dia sem entrar
@Service
public class IntensivaoService {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ConquistaService conquistaService;
    private final Clock clock;

    public IntensivaoService(ConquistaService conquistaService, Clock clock) {
        this.conquistaService = conquistaService;
        this.clock = clock;
    }

    ///Conta o login de hoje e verifica as conquistas. Devolve as que o aluno acabou de ganhar
    @Transactional
    public List<UsuarioConquista> registrarAcesso(Long usuarioId) {
        ///Bloqueia o aluno: dois logins juntos não contam o dia duas vezes
        GamificacaoUsuario estado = conquistaService.travarEstado(usuarioId);
        atualizarSequencia(estado, LocalDate.now(clock.withZone(FUSO)));

        ///Professores e admins são ignorados
        return conquistaService.avaliar(usuarioId);
    }

    ///Primeiro login: 1. Mesmo dia: não muda. Dia seguinte: +1
    ///Dois dias ou mais depois: volta a 1. Data antiga: ignora
    ///Devolve true se o contador mudou
    static boolean atualizarSequencia(GamificacaoUsuario estado, LocalDate hoje) {
        LocalDate ultimo = estado.getUltimoAcesso();

        if (ultimo != null && !hoje.isAfter(ultimo)) {
            return false; ///mesmo dia ou data anterior
        }

        boolean diaSeguinte = ultimo != null && ChronoUnit.DAYS.between(ultimo, hoje) == 1;
        int sequencia = diaSeguinte ? estado.getDiasConsecutivos() + 1 : 1;

        estado.setDiasConsecutivos(sequencia);
        estado.setMaiorSequencia(Math.max(estado.getMaiorSequencia(), sequencia));
        estado.setUltimoAcesso(hoje);
        return true;
    }
}
