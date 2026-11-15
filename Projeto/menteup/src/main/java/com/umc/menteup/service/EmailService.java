package com.umc.menteup.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/// Envio dos e-mails do MenteUp (código de login, redefinição de senha, convite e cadastro de professor)
/// Tudo sai pela API do Brevo, porque o Render gratuito bloqueia o envio por SMTP
/// O remetente é o EMAIL_PROJETO e precisa estar verificado no Brevo
@Service
public class EmailService {

    static final String URL_BREVO = "https://api.brevo.com/v3/smtp/email";
    private static final String NOME_REMETENTE = "MenteUp";

    private final RestClient brevo;
    private final String chaveBrevo;
    private final String remetente;

    @Autowired
    public EmailService(
            @Value("${menteup.email.brevo-api-key}") String chaveBrevo,
            @Value("${menteup.email.remetente}") String remetente) {
        this(RestClient.builder(), chaveBrevo, remetente);
    }

    // Usado nos testes, para trocar o servidor do Brevo por um simulado
    EmailService(RestClient.Builder builder, String chaveBrevo, String remetente) {
        ///Sem chave ou sem remetente nenhum e-mail sai, então a aplicação nem sobe
        if (chaveBrevo == null || chaveBrevo.isBlank()) {
            throw new IllegalStateException("Configure a variável BREVO_API_KEY para enviar os e-mails");
        }
        if (remetente == null || remetente.isBlank()) {
            throw new IllegalStateException("Configure a variável EMAIL_PROJETO com o remetente dos e-mails");
        }
        this.brevo = builder.build();
        this.chaveBrevo = chaveBrevo.trim();
        this.remetente = remetente.trim();
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

    ///Avisa se o pedido para ser professor foi aprovado ou não
    public void enviarResultadoCadastroProfessor(String destinatario, String nome, boolean aprovado) {
        enviar(destinatario,
                aprovado
                        ? "MenteUp - Cadastro de professor aprovado"
                        : "MenteUp - Cadastro de professor não aprovado",
                "Olá, " + nome + "!\n\n"
                        + (aprovado
                        ? "Seu cadastro como professor no MenteUp foi aprovado.\n"
                                + "Você já pode entrar na plataforma com seu e-mail e senha.\n"
                        : "Seu cadastro como professor no MenteUp não foi aprovado.\n"
                                + "Se quiser, você pode enviar um novo pedido pela tela de cadastro.\n"));
    }

    // Se der erro, a exceção sobe: quem chama decide o que fazer
    // (no login, por exemplo, o código não enviado conta como falha).
    // API de e-mail transacional do Brevo (HTTPS, porta 443): https://developers.brevo.com
    private void enviar(String destinatario, String assunto, String texto) {
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
