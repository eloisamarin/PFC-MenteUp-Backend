package com.umc.menteup.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.io.IOException;

@Configuration
// Desativado nos testes (firebase.enabled=false em src/test/resources/application.properties)
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

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

            log.info("Firebase Admin SDK inicializado");

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erro ao inicializar o Firebase Admin SDK.",
                    e
            );
        }
    }

}
