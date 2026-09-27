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
}
