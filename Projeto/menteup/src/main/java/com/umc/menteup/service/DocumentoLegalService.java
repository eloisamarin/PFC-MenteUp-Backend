package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.AceiteDocumento;
import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.AceiteDocumentoRepository;
import com.umc.menteup.repository.DocumentoLegalRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

// Versões do Termo de Aceite e da Política de Privacidade e os aceites de cada pessoa
@Service
public class DocumentoLegalService {

    private static final Comparator<DocumentoLegal> POR_VERSAO =
            (a, b) -> DocumentoLegal.compararVersoes(a.getVersao(), b.getVersao());

    // Etiquetas HTML (<script>, <img>, <!-- -->...) não entram no texto: o front mostra só Markdown
    private static final Pattern ETIQUETA_HTML = Pattern.compile("<\\s*/?\\s*[a-zA-Z!][^>]*>");

    public static final String AGENDADA = "AGENDADA";
    public static final String VIGENTE = "VIGENTE";
    public static final String ANTERIOR = "ANTERIOR";

    private final DocumentoLegalRepository documentoRepository;
    private final AceiteDocumentoRepository aceiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaService logSistemaService;
    private final Clock clock;

    // As versões só mudam quando a aplicação sobe ou quando o administrador publica ou cancela uma,
    // então ficam guardadas aqui para o filtro de aceite não consultar o banco a cada requisição
    private volatile Map<TipoDocumento, List<DocumentoLegal>> versoesPorTipo;

    public DocumentoLegalService(DocumentoLegalRepository documentoRepository,
                                 AceiteDocumentoRepository aceiteRepository,
                                 UsuarioRepository usuarioRepository,
                                 LogSistemaService logSistemaService,
                                 Clock clock) {
        this.documentoRepository = documentoRepository;
        this.aceiteRepository = aceiteRepository;
        this.usuarioRepository = usuarioRepository;
        this.logSistemaService = logSistemaService;
        this.clock = clock;
    }

    //Consulta pública

    // Maior versão que já está valendo (data de vigência até hoje)
    public DocumentoLegal vigente(TipoDocumento tipo) {
        return buscarVigente(tipo).orElseThrow(() -> ApiException.naoEncontrado(
                "DOCUMENTO_NAO_ENCONTRADO", "Nenhuma versão publicada deste documento."));
    }

    // Histórico das versões publicadas, da mais nova para a mais antiga
    public List<DocumentoLegal> historico(TipoDocumento tipo) {
        return publicadas(tipo).stream().sorted(POR_VERSAO.reversed()).toList();
    }

    public DocumentoLegal versao(TipoDocumento tipo, String versao) {
        return publicadas(tipo).stream()
                .filter(d -> d.getVersao().equals(versao))
                .findFirst()
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "VERSAO_NAO_ENCONTRADA", "Esta versão do documento não existe."));
    }

    //  Aceites

    // No cadastro e no primeiro acesso: a versão que a pessoa leu precisa ser a que está valendo
    // Sem versão informada (front antigo), considera a vigente, até o front novo ser publicado
    public void conferirVersoesInformadas(String termoVersao, String politicaVersao) {
        conferirVersao(TipoDocumento.TERMO, termoVersao);
        conferirVersao(TipoDocumento.POLITICA, politicaVersao);
    }

    // Registra o aceite das versões vigentes que a pessoa ainda não aceitou
    @Transactional
    public void registrarAceiteDasVigentes(Usuario usuario, LocalDateTime aceitoEm) {
        for (TipoDocumento tipo : TipoDocumento.values()) {
            buscarVigente(tipo).ifPresent(documento -> registrar(usuario, documento, aceitoEm));
        }
    }

    // Documentos que mudaram e que a pessoa ainda precisa aceitar (vazio = tudo em dia)
    @Transactional(readOnly = true)
    public List<DocumentoLegal> pendentes(Long usuarioId) {
        List<AceiteDocumento> aceites = aceiteRepository.buscarDoUsuario(usuarioId);
        List<DocumentoLegal> pendentes = new ArrayList<>();

        for (TipoDocumento tipo : TipoDocumento.values()) {
            Optional<DocumentoLegal> exigida = versaoMinimaExigida(tipo);
            if (exigida.isEmpty()) {
                continue;
            }

            boolean emDia = aceites.stream()
                    .map(AceiteDocumento::getDocumento)
                    .filter(d -> d.getTipo() == tipo)
                    .anyMatch(d -> DocumentoLegal.compararVersoes(d.getVersao(), exigida.get().getVersao()) >= 0);

            if (!emDia) {
                pendentes.add(vigente(tipo));
            }
        }

        return pendentes;
    }

    @Transactional(readOnly = true)
    public List<DocumentoLegal> pendentesPorUsername(String username) {
        return usuarioRepository.findByUsuario(username)
                .map(usuario -> pendentes(usuario.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<AceiteDocumento> aceitesDo(Long usuarioId) {
        return aceiteRepository.buscarDoUsuario(usuarioId);
    }

    // POST /usuarios/me/aceites: registra as versões aceitas e devolve o que ainda falta
    @Transactional
    public List<DocumentoLegal> aceitar(String username, String termoVersao, String politicaVersao) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        Map<TipoDocumento, String> informadas = new LinkedHashMap<>();
        if (termoVersao != null && !termoVersao.isBlank()) {
            informadas.put(TipoDocumento.TERMO, termoVersao.trim());
        }
        if (politicaVersao != null && !politicaVersao.isBlank()) {
            informadas.put(TipoDocumento.POLITICA, politicaVersao.trim());
        }
        if (informadas.isEmpty()) {
            throw ApiException.requisicaoInvalida("VERSAO_NAO_INFORMADA",
                    "Informe a versão do Termo e/ou da Política que você aceitou.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        informadas.forEach((tipo, versao) -> {
            conferirVersao(tipo, versao);
            DocumentoLegal documento = vigente(tipo);
            if (registrar(usuario, documento, agora)) {
                logSistemaService.registrarSucesso(usuario, AcaoLog.ACEITAR_TERMO, EntidadeLog.USUARIO,
                        usuario.getId(),
                        "Usuário #" + usuario.getId() + " aceitou a versão " + documento.getVersao()
                                + " do documento " + tipo.name() + ".",
                        Map.of("tipo", tipo.name(), "versao", documento.getVersao()));
            }
        });

        return pendentes(usuario.getId());
    }

    // Gestão pelo administrador

    // Todas as versões, inclusive a agendada, da mais nova para a mais antiga
    @Transactional(readOnly = true)
    public List<DocumentoLegal> todasAsVersoes(TipoDocumento tipo) {
        return documentoRepository.findAllByTipo(tipo.name()).stream().sorted(POR_VERSAO.reversed()).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoLegal versaoParaGestao(TipoDocumento tipo, String versao) {
        return documentoRepository.findByTipoAndVersao(tipo.name(), versao)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "VERSAO_NAO_ENCONTRADA", "Esta versão do documento não existe."));
    }

    public String situacao(DocumentoLegal documento) {
        if (documento.getVigenteDesde().isAfter(LocalDate.now(clock))) {
            return AGENDADA;
        }
        return buscarVigente(documento.getTipo())
                .filter(vigente -> vigente.getVersao().equals(documento.getVersao()))
                .map(vigente -> VIGENTE)
                .orElse(ANTERIOR);
    }

    // Publica uma versão nova com o próximo número
    // Uma vez valendo, nunca é alterada: para mudar o texto, publica-se outra versão
    @Transactional
    public DocumentoLegal publicar(TipoDocumento tipo, String conteudo, String resumo, LocalDate vigenteDesde,
                                   Boolean exigeNovoAceite, String username) {
        Usuario autor = carregarAutor(username);
        LocalDate hoje = LocalDate.now(clock);

        if (vigenteDesde.isBefore(hoje)) {
            throw ApiException.requisicaoInvalida("VIGENCIA_INVALIDA",
                    "A data de vigência precisa ser hoje ou uma data futura.");
        }

        String texto = DocumentoLegal.normalizarConteudo(conteudo);
        if (ETIQUETA_HTML.matcher(texto).find()) {
            throw ApiException.requisicaoInvalida("CONTEUDO_COM_HTML",
                    "O texto não pode ter HTML. Use apenas Markdown (para links, use [texto](endereço)).");
        }

        List<DocumentoLegal> existentes = documentoRepository.findAllByTipo(tipo.name());

        // Uma versão agendada por vez: para trocar, o administrador cancela a que está esperando
        Optional<DocumentoLegal> agendada = existentes.stream()
                .filter(d -> d.getVigenteDesde().isAfter(hoje))
                .findFirst();
        if (agendada.isPresent()) {
            throw ApiException.conflito("VERSAO_AGENDADA_EXISTENTE",
                    "Já existe a versão " + agendada.get().getVersao() + " agendada para "
                            + agendada.get().getVigenteDesde() + ". Cancele-a antes de publicar outra.");
        }

        String hash = DocumentoLegal.calcularHash(texto);
        if (buscarVigente(tipo).map(DocumentoLegal::getHash).filter(hash::equals).isPresent()) {
            throw ApiException.requisicaoInvalida("CONTEUDO_SEM_MUDANCA",
                    "O texto é igual ao da versão que está valendo.");
        }

        String ultima = existentes.stream().max(POR_VERSAO).map(DocumentoLegal::getVersao).orElse(null);
        DocumentoLegal nova = documentoRepository.save(new DocumentoLegal(tipo, DocumentoLegal.proximaVersao(ultima),
                texto, hash, vigenteDesde, exigeNovoAceite == null || exigeNovoAceite, resumo.trim()));

        logSistemaService.registrarSucesso(autor, AcaoLog.PUBLICAR_DOCUMENTO, null, null,
                "Versão " + nova.getVersao() + " do documento " + tipo.name() + " publicada, vigente desde "
                        + nova.getVigenteDesde() + ".",
                Map.of("tipo", tipo.name(), "versao", nova.getVersao(),
                        "vigenteDesde", nova.getVigenteDesde().toString(),
                        "exigeNovoAceite", String.valueOf(nova.isExigeNovoAceite())));

        recarregarDepoisDeGravar();
        return nova;
    }

    // Cancela uma versão que ainda não começou a valer (ninguém aceitou)
    @Transactional
    public void cancelar(TipoDocumento tipo, String versao, String username) {
        Usuario autor = carregarAutor(username);
        DocumentoLegal documento = versaoParaGestao(tipo, versao);

        if (!documento.getVigenteDesde().isAfter(LocalDate.now(clock))
                || aceiteRepository.existsByDocumento_Id(documento.getId())) {
            throw ApiException.conflito("VERSAO_JA_VIGENTE",
                    "Esta versão já está valendo ou já foi aceita e não pode ser cancelada. "
                            + "Para mudar o texto, publique uma versão nova.");
        }

        documentoRepository.delete(documento);

        logSistemaService.registrarSucesso(autor, AcaoLog.CANCELAR_DOCUMENTO, null, null,
                "Versão " + versao + " do documento " + tipo.name() + " cancelada antes de valer.",
                Map.of("tipo", tipo.name(), "versao", versao));

        recarregarDepoisDeGravar();
    }

    private Usuario carregarAutor(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    // Atualiza a lista guardada em memória só depois que a gravação foi confirmada no banco
    private void recarregarDepoisDeGravar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recarregar();
                }
            });
        } else {
            recarregar();
        }
    }

    // Usado pelo seeder e pela migração

    public void recarregar() {
        Map<TipoDocumento, List<DocumentoLegal>> mapa = new EnumMap<>(TipoDocumento.class);
        for (TipoDocumento tipo : TipoDocumento.values()) {
            mapa.put(tipo, List.copyOf(documentoRepository.findAllByTipo(tipo.name())));
        }
        versoesPorTipo = mapa;
    }

    // Menor versão publicada (a que as contas antigas aceitaram)
    public Optional<DocumentoLegal> primeiraVersao(TipoDocumento tipo) {
        return publicadas(tipo).stream().min(POR_VERSAO);
    }

    // Grava o aceite se ainda não existe; devolve true quando gravou
    @Transactional
    public boolean registrar(Usuario usuario, DocumentoLegal documento, LocalDateTime aceitoEm) {
        if (aceiteRepository.existsByUsuario_IdAndDocumento_Id(usuario.getId(), documento.getId())) {
            return false;
        }
        aceiteRepository.save(new AceiteDocumento(
                usuario, documentoRepository.getReferenceById(documento.getId()), aceitoEm));
        return true;
    }

    // Regras internas

    private void conferirVersao(TipoDocumento tipo, String versaoInformada) {
        if (versaoInformada == null || versaoInformada.isBlank()) {
            return;
        }
        Optional<DocumentoLegal> vigente = buscarVigente(tipo);
        if (vigente.isPresent() && !vigente.get().getVersao().equals(versaoInformada.trim())) {
            throw ApiException.conflito("DOCUMENTO_ATUALIZADO",
                    (tipo == TipoDocumento.TERMO ? "O Termo de Aceite" : "A Política de Privacidade")
                            + " foi atualizado(a) para a versão " + vigente.get().getVersao()
                            + ". Leia a nova versão antes de aceitar.");
        }
    }

    // A pessoa precisa ter aceitado esta versão ou uma mais nova:
    // a última que pediu novo aceite (ou a primeira de todas, se nenhuma pediu)
    private Optional<DocumentoLegal> versaoMinimaExigida(TipoDocumento tipo) {
        List<DocumentoLegal> publicadas = publicadas(tipo);
        Optional<DocumentoLegal> ultimaQueExige = publicadas.stream()
                .filter(DocumentoLegal::isExigeNovoAceite)
                .max(POR_VERSAO);
        return ultimaQueExige.isPresent() ? ultimaQueExige : publicadas.stream().min(POR_VERSAO);
    }

    private Optional<DocumentoLegal> buscarVigente(TipoDocumento tipo) {
        return publicadas(tipo).stream().max(POR_VERSAO);
    }

    // Só as versões com data de vigência até hoje (uma versão com data futura fica guardada, sem valer)
    private List<DocumentoLegal> publicadas(TipoDocumento tipo) {
        if (versoesPorTipo == null) {
            recarregar();
        }
        LocalDate hoje = LocalDate.now(clock);
        return versoesPorTipo.getOrDefault(tipo, List.of()).stream()
                .filter(d -> !d.getVigenteDesde().isAfter(hoje))
                .toList();
    }
}
