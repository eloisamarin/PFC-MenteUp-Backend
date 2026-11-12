package com.umc.menteup.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

///Verifica as conquistas depois que a atividade ou o desafio já foi salvo
///Um erro aqui não desfaz o que o aluno fez e é corrigido na próxima verificação
@Component
public class ConquistaListener {

    private static final Logger log = LoggerFactory.getLogger(ConquistaListener.class);

    private final ConquistaService conquistaService;

    public ConquistaListener(ConquistaService conquistaService) {
        this.conquistaService = conquistaService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoSolicitarAvaliacao(AvaliarConquistasEvento evento) {
        for (Long usuarioId : evento.usuarioIds()) {
            try {
                conquistaService.avaliarEmNovaTransacao(usuarioId);
            } catch (Exception e) {
                log.warn("Falha ao avaliar conquistas do usuário {}: {}", usuarioId, e.getMessage());
            }
        }
    }
}
