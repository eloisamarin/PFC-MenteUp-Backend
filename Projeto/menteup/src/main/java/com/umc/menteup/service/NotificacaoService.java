package com.umc.menteup.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.umc.menteup.model.Atividade;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    public void enviarNotificacao(
            String token,
            String titulo,
            String mensagem,
            String atividadeId) {

        if (token == null || token.isBlank()) {
            log.debug("Notificação não enviada: token do celular vazio");
            return;
        }

        Message message = Message.builder()
                .setToken(token)

                .setNotification(
                        Notification.builder()
                                .setTitle(titulo)
                                .setBody(mensagem)
                                .build()
                )

                .putData("tela", "atividades")
                .putData(
                        "atividadeId",
                        atividadeId
                )

                .build();

        try {

            FirebaseMessaging.getInstance().send(message);
            log.debug("Notificação da atividade {} enviada", atividadeId);

        } catch (FirebaseMessagingException e) {

            // Só o código do erro: a mensagem do Firebase pode repetir o token do celular
            log.warn("Erro ao enviar notificação da atividade {}: {}", atividadeId, e.getMessagingErrorCode());
        }
    }

    public void notificarNovaAtividade(
            Atividade atividade,
            String token) {

        if (atividade == null) {
            log.warn("Não foi possível enviar a notificação: atividade nula");
            return;
        }

        String titulo =
                "🔔 Nova atividade no MenteUp";

        String mensagem =
                "Uma nova atividade foi criada: "
                        + atividade.getTitulo()
                        + " • "
                        + atividade.getPontuacao()
                        + " pontos";

        enviarNotificacao(
                token,
                titulo,
                mensagem,
                String.valueOf(atividade.getId())
        );
    }
}