package com.umc.menteup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.umc.menteup.controller.UsuarioController;

@SpringBootTest
class MenteupApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void deveSubirOContextoSemDependerDoFirebase() {
        // Assert
        assertNotNull(context.getBean(UsuarioController.class));
        assertFalse(context.containsBean("firebaseConfig"));
    }

}
