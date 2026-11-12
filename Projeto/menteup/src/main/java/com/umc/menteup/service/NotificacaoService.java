package com.umc.menteup.service;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.umc.menteup.model.Atividade;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    public void enviarNotificacao(
            String token,
            String titulo,
            String mensagem,
            String atividadeId) {

        if (token == null || token.isBlank()) {
            System.out.println(
                    "Notificação não enviada: token Firebase vazio."
            );
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

            String resposta =
                    FirebaseMessaging.getInstance().send(message);

            System.out.println(
                    "Notificação Firebase enviada com sucesso: "
                            + resposta
            );

        } catch (FirebaseMessagingException e) {

            System.err.println(
                    "Erro ao enviar notificação Firebase: "
                            + e.getMessage()
            );
        }
    }

    public void notificarNovaAtividade(
            Atividade atividade,
            String token) {

        if (atividade == null) {
            System.out.println(
                    "Não foi possível enviar a notificação: atividade nula."
            );
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