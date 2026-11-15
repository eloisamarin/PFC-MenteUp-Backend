package com.umc.menteup.config;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.repository.DocumentoLegalRepository;
import com.umc.menteup.service.DocumentoLegalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

// Publica as versões do Termo e da Política que estão em src/main/resources/documentos
// Cada arquivo é uma versão: documentos/termo/1.0.md, documentos/politica/1.1.md e assim por diante
// No começo do arquivo, entre linhas com ---, ficam a data de vigência, se exige novo aceite e o resumo
// Versão nova é gravada; versão que já existe nunca é alterada (se o texto mudou, só avisa no log)
@Component
@Order(2)
public class DocumentoLegalSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentoLegalSeeder.class);
    private static final String SEPARADOR = "---";

    private final DocumentoLegalRepository documentoRepository;
    private final DocumentoLegalService documentoLegalService;

    public DocumentoLegalSeeder(DocumentoLegalRepository documentoRepository,
                                DocumentoLegalService documentoLegalService) {
        this.documentoRepository = documentoRepository;
        this.documentoLegalService = documentoLegalService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        // Lê pelo classpath: no Render a aplicação roda de dentro do .jar, onde a pasta não existe
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (TipoDocumento tipo : TipoDocumento.values()) {
            for (Resource arquivo : resolver.getResources("classpath*:documentos/" + tipo.getPasta() + "/*.md")) {
                publicar(tipo, arquivo);
            }
        }

        documentoLegalService.recarregar();
    }

    private void publicar(TipoDocumento tipo, Resource arquivo) throws IOException {
        String nome = arquivo.getFilename();
        if (nome == null) {
            return;
        }
        String versao = nome.substring(0, nome.length() - ".md".length());

        Optional<VersaoLida> lida = ler(arquivo, nome);
        if (lida.isEmpty()) {
            return;
        }

        String hash = sha256(lida.get().conteudo());
        Optional<DocumentoLegal> existente = documentoRepository.findByTipoAndVersao(tipo.name(), versao);

        if (existente.isPresent()) {
            if (!existente.get().getHash().equals(hash)) {
                log.warn("Documentos: o texto de {} {} mudou depois de publicado. A versão gravada foi mantida; "
                        + "para mudar o texto, crie uma versão nova.", tipo, versao);
            }
            return;
        }

        documentoRepository.save(new DocumentoLegal(tipo, versao, lida.get().conteudo(), hash,
                lida.get().vigenteDesde(), lida.get().exigeNovoAceite(), lida.get().resumo()));
        log.info("Documentos: {} {} publicado (vigente desde {}).", tipo, versao, lida.get().vigenteDesde());
    }

    private record VersaoLida(LocalDate vigenteDesde, boolean exigeNovoAceite, String resumo, String conteudo) {
    }

    // Separa o cabeçalho (entre ---) do texto. Arquivo com cabeçalho errado é ignorado, com aviso no log
    private Optional<VersaoLida> ler(Resource arquivo, String nome) throws IOException {
        String texto;
        try (InputStream entrada = arquivo.getInputStream()) {
            texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Mesmo texto no Windows (CRLF) e no Render (LF): senão o hash mudaria de uma máquina para outra
        texto = texto.replace("﻿", "").replace("\r\n", "\n").replace('\r', '\n');

        if (!texto.startsWith(SEPARADOR + "\n")) {
            log.warn("Documentos: {} ignorado, falta o cabeçalho entre linhas ---.", nome);
            return Optional.empty();
        }
        int fim = texto.indexOf("\n" + SEPARADOR + "\n", SEPARADOR.length());
        if (fim < 0) {
            log.warn("Documentos: {} ignorado, o cabeçalho não foi fechado com ---.", nome);
            return Optional.empty();
        }

        Map<String, String> campos = new HashMap<>();
        for (String linha : texto.substring(SEPARADOR.length() + 1, fim).split("\n")) {
            int doisPontos = linha.indexOf(':');
            if (doisPontos > 0) {
                campos.put(linha.substring(0, doisPontos).trim(), linha.substring(doisPontos + 1).trim());
            }
        }

        LocalDate vigenteDesde;
        try {
            vigenteDesde = LocalDate.parse(campos.getOrDefault("vigenteDesde", ""));
        } catch (DateTimeParseException e) {
            log.warn("Documentos: {} ignorado, vigenteDesde precisa estar no formato AAAA-MM-DD.", nome);
            return Optional.empty();
        }

        String conteudo = texto.substring(fim + SEPARADOR.length() + 2).strip() + "\n";
        String resumo = campos.get("resumo");
        if (resumo != null && resumo.length() > 500) {
            resumo = resumo.substring(0, 500);
        }

        return Optional.of(new VersaoLida(vigenteDesde,
                Boolean.parseBoolean(campos.getOrDefault("exigeNovoAceite", "true")), resumo, conteudo));
    }

    private static String sha256(String texto) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
