package com.umc.menteup.config;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.AceiteDocumentoRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.DocumentoLegalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// As contas criadas antes do versionamento só tinham a data do aceite (termo_aceito_em)
// Para cada uma, registra o aceite da primeira versão do Termo e da Política com essa mesma data
// Roda depois do seeder dos documentos e não repete: quem já tem algum aceite registrado fica de fora
@Component
@Order(3)
public class MigracaoAceitesIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoAceitesIniciais.class);

    private final UsuarioRepository usuarioRepository;
    private final AceiteDocumentoRepository aceiteRepository;
    private final DocumentoLegalService documentoLegalService;

    public MigracaoAceitesIniciais(UsuarioRepository usuarioRepository,
                                   AceiteDocumentoRepository aceiteRepository,
                                   DocumentoLegalService documentoLegalService) {
        this.usuarioRepository = usuarioRepository;
        this.aceiteRepository = aceiteRepository;
        this.documentoLegalService = documentoLegalService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<DocumentoLegal> termo = documentoLegalService.primeiraVersao(TipoDocumento.TERMO);
        Optional<DocumentoLegal> politica = documentoLegalService.primeiraVersao(TipoDocumento.POLITICA);
        if (termo.isEmpty() && politica.isEmpty()) {
            return;
        }

        List<Usuario> semRegistro = usuarioRepository.findAll().stream()
                .filter(u -> u.getTermoAceitoEm() != null)
                .filter(u -> !aceiteRepository.existsByUsuario_Id(u.getId()))
                .toList();

        for (Usuario usuario : semRegistro) {
            termo.ifPresent(d -> documentoLegalService.registrar(usuario, d, usuario.getTermoAceitoEm()));
            politica.ifPresent(d -> documentoLegalService.registrar(usuario, d, usuario.getTermoAceitoEm()));
        }

        if (!semRegistro.isEmpty()) {
            log.info("Documentos: aceite da primeira versão registrado para {} conta(s) antiga(s).",
                    semRegistro.size());
        }
    }
}
