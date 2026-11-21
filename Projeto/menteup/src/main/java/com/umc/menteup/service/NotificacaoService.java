package com.umc.menteup.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.umc.menteup.model.Atividade;

/*
 * Notificações push (Firebase Cloud Messaging) enviadas aos alunos:
 * - nova atividade publicada;
 * - desafio ao vivo criado e desafio ao vivo iniciado;
 * - comentário do professor no resultado de uma atividade.
 * Cada notificação leva no campo "url" a tela que o front abre quando o aluno clica nela.
 * Com o Firebase desligado (FIREBASE_ENABLED=false), nada é enviado e o sistema segue normalmente.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    /// Endereço do front (APP_FRONTEND_URL): o clique na notificação abre a tela certa do MenteUp
    private final String frontendUrl;

    public NotificacaoService(@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    public void enviarNotificacao(
            String token,
            String titulo,
            String mensagem,
            String atividadeId) {

        enviar(token, titulo, mensagem, Map.of(
                "tela", "atividades",
                "atividadeId", atividadeId,
                "url", "/aluno/atividades"));
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

    /// Avisa os alunos da turma que o desafio começou.
    /// Roda em segundo plano: o cronômetro da primeira pergunta já está correndo e o professor não pode esperar.
    /// Recebe só textos e números (e não a entidade), porque a transação do desafio termina antes do envio.
    public void notificarDesafioIniciado(Long desafioId, String tituloDesafio, Collection<String> tokens) {
        enviarParaVarios(tokens,
                "🏁 Desafio ao vivo começou!",
                "O desafio \"" + tituloDesafio + "\" já começou. Entre agora para responder.",
                dadosDoDesafio(desafioId));
    }

    /// Avisa os alunos da turma que o professor criou um desafio (ainda aguardando o início)
    public void notificarDesafioCriado(Long desafioId, String tituloDesafio, Collection<String> tokens) {
        enviarParaVarios(tokens,
                "🎯 Novo desafio ao vivo",
                "O desafio \"" + tituloDesafio + "\" foi criado para a sua turma. Fique atento ao início!",
                dadosDoDesafio(desafioId));
    }

    private static Map<String, String> dadosDoDesafio(Long desafioId) {
        return Map.of(
                "tela", "desafio",
                "desafioId", String.valueOf(desafioId),
                "url", "/aluno/desafios/" + desafioId);
    }

    /// Envia a mesma notificação para vários alunos, em segundo plano
    private void enviarParaVarios(Collection<String> tokens, String titulo, String mensagem,
                                  Map<String, String> dados) {
        List<String> destinos = tokens.stream().filter(t -> t != null && !t.isBlank()).toList();
        if (destinos.isEmpty() || !firebaseLigado()) {
            return;
        }
        CompletableFuture.runAsync(() -> destinos.forEach(token -> enviar(token, titulo, mensagem, dados)));
    }

    /// Avisa o aluno que o professor comentou o resultado de uma atividade
    public void notificarComentario(Long atividadeId, String tituloAtividade, String token) {
        if (token == null || token.isBlank() || !firebaseLigado()) {
            return;
        }

        String titulo = "💬 Novo comentário do professor";
        String mensagem = "Seu professor comentou o resultado da atividade \"" + tituloAtividade + "\".";
        Map<String, String> dados = Map.of(
                "tela", "resultado",
                "atividadeId", String.valueOf(atividadeId),
                "url", "/aluno/atividades");

        CompletableFuture.runAsync(() -> enviar(token, titulo, mensagem, dados));
    }

    private void enviar(String token, String titulo, String mensagem, Map<String, String> dados) {
        if (token == null || token.isBlank()) {
            log.debug("Notificação não enviada: token do celular vazio");
            return;
        }
        if (!firebaseLigado()) {
            log.debug("Notificação não enviada: Firebase desligado");
            return;
        }

        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(
                        Notification.builder()
                                .setTitle(titulo)
                                .setBody(mensagem)
                                .build()
                )
                .putAllData(dados);

        // O Firebase só aceita link HTTPS: no computador (http://localhost) o front decide a tela pelo campo "url"
        String url = dados.get("url");
        if (url != null && frontendUrl.startsWith("https://")) {
            builder.setWebpushConfig(WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.withLink(frontendUrl + url))
                    .build());
        }
        Message message = builder.build();

        try {
            FirebaseMessaging.getInstance().send(message);
            log.debug("Notificação \"{}\" enviada", dados.get("tela"));
        } catch (FirebaseMessagingException e) {
            // Só o código do erro: a mensagem do Firebase pode repetir o token do celular
            log.warn("Erro ao enviar notificação \"{}\": {}", dados.get("tela"), e.getMessagingErrorCode());
        }
    }

    /// O Firebase só é configurado quando FIREBASE_ENABLED não é false (ver FirebaseConfig)
    private static boolean firebaseLigado() {
        return !FirebaseApp.getApps().isEmpty();
    }
}
