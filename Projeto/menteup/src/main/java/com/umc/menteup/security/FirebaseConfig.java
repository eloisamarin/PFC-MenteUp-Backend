package com.umc.menteup.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.io.IOException;

@Configuration
// Desativado nos testes (firebase.enabled=false em src/test/resources/application.properties)
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @PostConstruct
    public void inicializarFirebase() {

        try {

            // Evita inicializar o Firebase mais de uma vez
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.getApplicationDefault()
                    )
                    .build();

            FirebaseApp.initializeApp(options);

            System.out.println(
                    "Firebase Admin SDK inicializado com sucesso!"
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erro ao inicializar o Firebase Admin SDK.",
                    e
            );
        }
    }

}
