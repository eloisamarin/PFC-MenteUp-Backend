package com.umc.menteup.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/*
 * Envio dos e-mails do MenteUp (código de login, redefinição de senha e convite).
 *
 * Duas formas de envio, escolhidas pela configuração:
 * - Com BREVO_API_KEY: envia pela API HTTPS do Brevo. É a forma usada no Render, porque o plano
 *   gratuito do Render bloqueia as portas de SMTP (o Gmail na porta 587 não conecta).
 * - Sem BREVO_API_KEY: envia pelo SMTP do Gmail (spring.mail.*), como no computador de desenvolvimento.
 * O remetente é sempre o EMAIL_PROJETO; no Brevo ele precisa estar cadastrado como "sender" verificado.
 */
@Service
public class EmailService {

    static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";
    private static final String NOME_REMETENTE = "MenteUp";

    private final JavaMailSender mailSender;
    private final RestClient brevo;
    private final String chaveBrevo;
    private final String remetente;

    @Autowired
    public EmailService(
            JavaMailSender mailSender,
            @Value("${menteup.email.brevo-api-key:}") String chaveBrevo,
            @Value("${spring.mail.username:}") String remetente) {
        this(mailSender, RestClient.builder(), chaveBrevo, remetente);
    }

    // Usado nos testes, para trocar o servidor do Brevo por um simulado
    EmailService(JavaMailSender mailSender, RestClient.Builder builder, String chaveBrevo, String remetente) {
        this.mailSender = mailSender;
        this.brevo = builder.build();
        this.chaveBrevo = chaveBrevo == null ? "" : chaveBrevo.trim();
        this.remetente = remetente;
    }

    // Código de verificação

    public void enviarCodigo2FA(
            String destinatario,
            String codigo
    ) {
        enviar(destinatario,
                "MenteUp - Código de verificação",
                "Olá!\n\n"
                        + "Seu código de verificação do MenteUp é:\n\n"
                        + codigo
                        + "\n\n"
                        + "Esse código é válido por 5 minutos.\n\n");
    }

    // Link para criar uma nova senha (também desbloqueia a conta)
    public void enviarLinkRedefinicaoSenha(String destinatario, String link) {
        enviar(destinatario,
                "MenteUp - Redefinição de senha",
                "Olá!\n\n"
                        + "Recebemos um pedido para redefinir a senha da sua conta no MenteUp.\n"
                        + "Para criar uma nova senha, acesse o link abaixo:\n\n"
                        + link
                        + "\n\nO link vale por 30 minutos e só pode ser usado uma vez.\n"
                        + "Se a sua senha estava bloqueada, ela será desbloqueada com a nova senha.\n\n"
                        + "Se não foi você, ignore este e-mail: ninguém consegue trocar sua senha sem esse link.\n");
    }

    // Convite de quem foi cadastrado pelo administrador: criar a senha e aceitar o termo
    public void enviarConvitePrimeiroAcesso(String destinatario, String nome, String link) {
        enviar(destinatario,
                "MenteUp - Bem-vindo(a)! Crie sua senha",
                "Olá, " + nome + "!\n\n"
                        + "Um administrador criou a sua conta no MenteUp.\n"
                        + "Para começar, acesse o link abaixo, leia e aceite o Termo de Aceite e a "
                        + "Política de Privacidade e crie a sua senha:\n\n"
                        + link
                        + "\n\nO link vale por 3 dias e só pode ser usado uma vez.\n"
                        + "Se ele expirar, use \"Esqueci minha senha\" na tela de login para receber um novo.\n\n"
                        + "Se você não esperava este e-mail, pode ignorá-lo.\n");
    }

    // Escolhe a forma de envio. Se der erro, a exceção sobe: quem chama decide o que fazer
    // (no login, por exemplo, o código não enviado conta como falha).
    private void enviar(String destinatario, String assunto, String texto) {
        if (chaveBrevo.isEmpty()) {
            enviarPorSmtp(destinatario, assunto, texto);
        } else {
            enviarPeloBrevo(destinatario, assunto, texto);
        }
    }

    private void enviarPorSmtp(String destinatario, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mailSender.send(mensagem);
    }

    // API de e-mail transacional do Brevo (HTTPS, porta 443): https://developers.brevo.com
    private void enviarPeloBrevo(String destinatario, String assunto, String texto) {
        Map<String, Object> corpo = Map.of(
                "sender", Map.of("name", NOME_REMETENTE, "email", remetente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", assunto,
                "textContent", texto);

        brevo.post()
                .uri(URL_BREVO)
                .header("api-key", chaveBrevo)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
    }
}
