package com.umc.menteup.config;

import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.ConquistaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

///Verifica as conquistas de todos os alunos quando a aplicação sobe, depois do ConquistaSeeder
///Assim o ranking e os níveis já começam certos sem esperar cada aluno fazer login
///Ligar somente quando adiconar mais conquistas no app, senão mantenha como false no application.properties
@Component
@Order(2)
public class ConquistaVerificacaoInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConquistaVerificacaoInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final ConquistaService conquistaService;
    private final boolean ativa;

    public ConquistaVerificacaoInicial(
            UsuarioRepository usuarioRepository,
            ConquistaService conquistaService,
            @Value("${menteup.conquistas.verificar-ao-iniciar:true}") boolean ativa
    ) {
        this.usuarioRepository = usuarioRepository;
        this.conquistaService = conquistaService;
        this.ativa = ativa;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ativa) {
            return;
        }

        int conquistasLiberadas = 0;
        for (Usuario aluno : usuarioRepository.findAllByTipoUsuarioIgnoreCase(TipoUsuario.ALUNO)) {
            try {
                conquistasLiberadas += conquistaService.avaliarEmNovaTransacao(aluno.getId()).size();
            } catch (Exception e) {
                log.warn("Falha ao verificar conquistas do aluno {}: {}", aluno.getId(), e.getMessage());
            }
        }

        if (conquistasLiberadas > 0) {
            log.info("Verificação inicial: {} conquista(s) liberada(s).", conquistasLiberadas);
        }
    }
}
