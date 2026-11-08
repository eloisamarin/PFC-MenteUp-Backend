package com.umc.menteup.service;



import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Código de verificação

    public void enviarCodigo2FA(
            String destinatario,
            String codigo
    ) {

        SimpleMailMessage mensagem =
                new SimpleMailMessage();

        mensagem.setTo(destinatario);

        mensagem.setSubject(
                "MenteUp - Código de verificação"
        );

        mensagem.setText(
                "Olá!\n\n" +

                        "Seu código de verificação do MenteUp é:\n\n" +

                        codigo +

                        "\n\n" +

                        "Esse código é válido por 5 minutos.\n\n"

        );

        mailSender.send(mensagem);
    }

    // Link para criar uma nova senha (também desbloqueia a conta)
    public void enviarLinkRedefinicaoSenha(String destinatario, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject("MenteUp - Redefinição de senha");
        mensagem.setText(
                "Olá!\n\n"
                        + "Recebemos um pedido para redefinir a senha da sua conta no MenteUp.\n"
                        + "Para criar uma nova senha, acesse o link abaixo:\n\n"
                        + link
                        + "\n\nO link vale por 30 minutos e só pode ser usado uma vez.\n"
                        + "Se a sua senha estava bloqueada, ela será desbloqueada com a nova senha.\n\n"
                        + "Se não foi você, ignore este e-mail: ninguém consegue trocar sua senha sem esse link.\n");

        mailSender.send(mensagem);
    }


}
