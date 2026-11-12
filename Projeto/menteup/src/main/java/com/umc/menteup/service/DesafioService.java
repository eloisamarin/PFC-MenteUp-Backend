package com.umc.menteup.service;

import com.umc.menteup.dto.*;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.DesafioRepository;
import com.umc.menteup.repository.RespostaDesafioRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DesafioService {

    ///Cada acerto vale de 25 a 75 pontos: 25 fixos + até 50 pela rapidez
    private static final int PONTOS_BASE = 25;
    private static final int BONUS_MAXIMO_VELOCIDADE = 50;

    private final DesafioRepository desafioRepository;
    private final RespostaDesafioRepository respostaRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaService matriculaService;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;
    private final LogSistemaService logSistemaService;

    public DesafioService(
            DesafioRepository desafioRepository,
            RespostaDesafioRepository respostaRepository,
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            MatriculaService matriculaService,
            Clock clock,
            ApplicationEventPublisher eventos,
            LogSistemaService logSistemaService
    ) {
        this.eventos = eventos;
        this.logSistemaService = logSistemaService;
        this.desafioRepository = desafioRepository;
        this.respostaRepository = respostaRepository;
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.matriculaService = matriculaService;
        this.clock = clock;
    }

    @Transactional
    public DesafioResponse criar(CriarDesafioRequest request, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Turma turma = carregarTurma(request.turmaId());
        exigirPodeAlterar(turma, autor);

        Desafio desafio = new Desafio();
        desafio.setTitulo(request.titulo());
        desafio.setDescricao(request.descricao());
        desafio.setTurma(turma);
        desafio.setUsuario(autor);

        int ordem = 0;
        for (PerguntaDesafioRequest perguntaRequest : request.perguntas()) {
            desafio.getPerguntas().add(montarPergunta(perguntaRequest, desafio, ordem++));
        }

        Desafio salvo = desafioRepository.save(desafio);
        registrarLog(autor, AcaoLog.CRIAR_DESAFIO, salvo, "Desafio criado");

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<DesafioResponse> listar(String username) {
        Usuario usuario = carregarUsuario(username);

        List<Desafio> desafios = TipoUsuario.ehAluno(usuario)
                ? desafioRepository.findVisiveisParaAluno(usuario.getId())
                : desafioRepository.findAllByOrderByDataCriacaoDesc();

        return desafios.stream().map(DesafioService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DesafioEdicaoResponse buscarParaEdicao(Long desafioId, String username) {
        Usuario usuario = exigirProfessorOuAdministrador(username);

        Desafio desafio = carregarDesafio(desafioId);
        exigirPodeAlterar(desafio.getTurma(), usuario);

        exigirAguardando(desafio);

        return toEdicaoResponse(desafio);
    }

    @Transactional
    public DesafioResponse atualizar(Long desafioId, CriarDesafioRequest request, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Desafio desafio = carregarDesafio(desafioId);

        // Professor só edita desafio da turma dele e não pode mover para a turma de outro professor
        Turma novaTurma = carregarTurma(request.turmaId());
        exigirPodeAlterar(desafio.getTurma(), autor);
        exigirPodeAlterar(novaTurma, autor);

        exigirAguardando(desafio);

        desafio.setTitulo(request.titulo());
        desafio.setDescricao(request.descricao());
        desafio.setTurma(novaTurma);

        // Sem respostas enquanto aguarda, então as perguntas podem ser trocadas por inteiro
        desafio.getPerguntas().clear();

        int ordem = 0;
        for (PerguntaDesafioRequest perguntaRequest : request.perguntas()) {
            desafio.getPerguntas().add(montarPergunta(perguntaRequest, desafio, ordem++));
        }

        Desafio salvo = desafioRepository.save(desafio);
        registrarLog(autor, AcaoLog.EDITAR_DESAFIO, salvo, "Desafio editado");

        return toResponse(salvo);
    }

    @Transactional
    public void excluir(Long desafioId, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Desafio desafio = carregarDesafio(desafioId);
        exigirPodeAlterar(desafio.getTurma(), autor);

        if (desafio.getStatus() == StatusDesafio.EM_ANDAMENTO) {
            throw ApiException.conflito(
                    "DESAFIO_EM_ANDAMENTO", "Não é possível excluir um desafio em andamento.");
        }

        registrarLog(autor, AcaoLog.EXCLUIR_DESAFIO, desafio, "Desafio excluído");

        desafioRepository.delete(desafio);
    }

    @Transactional
    public EstadoDesafioResponse iniciar(Long desafioId, String username) {
        Usuario professor = exigirProfessorOuAdministrador(username);

        Desafio desafio = carregarDesafio(desafioId);
        exigirPodeAlterar(desafio.getTurma(), professor);

        if (desafio.getStatus() != StatusDesafio.AGUARDANDO) {
            throw ApiException.conflito("DESAFIO_JA_INICIADO", "Este desafio já foi iniciado.");
        }

        desafio.setStatus(StatusDesafio.EM_ANDAMENTO);
        abrirPergunta(desafio, 0);

        registrarLog(professor, AcaoLog.INICIAR_DESAFIO, desafio, "Desafio iniciado");

        return montarEstado(desafio, professor);
    }

    @Transactional
    public EstadoDesafioResponse avancar(Long desafioId, String username) {
        Usuario professor = exigirProfessorOuAdministrador(username);

        Desafio desafio = carregarDesafio(desafioId);
        exigirPodeAlterar(desafio.getTurma(), professor);

        if (desafio.getStatus() != StatusDesafio.EM_ANDAMENTO) {
            throw ApiException.conflito("DESAFIO_NAO_ESTA_EM_ANDAMENTO", "Este desafio não está em andamento.");
        }

        if (!perguntaEncerrada(desafio)) {
            throw ApiException.conflito(
                    "PERGUNTA_EM_ANDAMENTO", "Aguarde o tempo acabar ou todos os alunos responderem.");
        }

        int proxima = desafio.getIndicePerguntaAtual() + 1;

        if (proxima < desafio.getPerguntas().size()) {
            abrirPergunta(desafio, proxima);
        } else {
            desafio.setStatus(StatusDesafio.ENCERRADO);
            desafio.setIndicePerguntaAtual(null);
            desafio.setPerguntaExpiraEm(null);

            registrarLog(professor, AcaoLog.ENCERRAR_DESAFIO, desafio, "Desafio encerrado");

            ///Verifica as conquistas de todos os participantes no fim do desafio, não a cada resposta
            List<Long> participantes = respostaRepository.buscarTodasDoDesafio(desafioId).stream()
                    .map(r -> r.getUsuario().getId())
                    .distinct()
                    .toList();
            if (!participantes.isEmpty()) {
                eventos.publishEvent(AvaliarConquistasEvento.de(participantes));
            }
        }

        return montarEstado(desafio, professor);
    }

    @Transactional(readOnly = true)
    public EstadoDesafioResponse buscarEstado(Long desafioId, String username) {
        Usuario usuario = carregarUsuario(username);
        Desafio desafio = carregarDesafio(desafioId);

        exigirAcesso(desafio, usuario);

        return montarEstado(desafio, usuario);
    }

    @Transactional
    public RespostaProgressoResponse responder(Long desafioId, ResponderDesafioRequest request, String username) {
        Usuario aluno = carregarUsuario(username);

        if (!TipoUsuario.ehAluno(aluno)) {
            throw ApiException.proibido("SOMENTE_ALUNO", "Somente alunos podem responder o desafio.");
        }

        Desafio desafio = carregarDesafio(desafioId);

        exigirAcesso(desafio, aluno);

        if (desafio.getStatus() == StatusDesafio.AGUARDANDO) {
            throw ApiException.conflito("DESAFIO_NAO_INICIADO", "O professor ainda não iniciou este desafio.");
        }

        if (desafio.getStatus() == StatusDesafio.ENCERRADO) {
            throw ApiException.conflito("DESAFIO_ENCERRADO", "Este desafio já foi encerrado.");
        }

        PerguntaDesafio pergunta = perguntaAtual(desafio);

        if (!pergunta.getId().equals(request.perguntaId())) {
            throw ApiException.requisicaoInvalida(
                    "PERGUNTA_NAO_ATIVA", "Esta não é a pergunta em andamento.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);

        if (agora.isAfter(desafio.getPerguntaExpiraEm())) {
            throw ApiException.conflito("TEMPO_ESGOTADO", "O tempo para responder esta pergunta acabou.");
        }

        if (respostaRepository.existsByPergunta_IdAndUsuario_Id(pergunta.getId(), aluno.getId())) {
            throw perguntaJaRespondida();
        }

        AlternativaDesafio alternativa = pergunta.getAlternativas().stream()
                .filter(a -> a.getId().equals(request.alternativaId()))
                .findFirst()
                .orElseThrow(() -> ApiException.requisicaoInvalida(
                        "ALTERNATIVA_INVALIDA", "A alternativa não pertence a esta pergunta."));

        RespostaDesafio resposta = new RespostaDesafio();
        resposta.setPergunta(pergunta);
        resposta.setAlternativa(alternativa);
        resposta.setUsuario(aluno);
        resposta.setCorreta(alternativa.getCorreta());
        resposta.setPontos(calcularPontos(desafio, pergunta, alternativa, agora));
        resposta.setRespondidaEm(agora);

        try {
            respostaRepository.saveAndFlush(resposta);
        } catch (DataIntegrityViolationException e) {
            // Dois envios simultâneos do mesmo aluno: a constraint do banco barra o segundo
            throw perguntaJaRespondida();
        }

        registrarLog(aluno, AcaoLog.RESPONDER_DESAFIO, desafio, "Aluno respondeu uma pergunta do desafio");

        return new RespostaProgressoResponse(pergunta.getId(), alternativa.getId());
    }

    ///Atalho para registrar as ações do desafio
    private void registrarLog(Usuario autor, AcaoLog acao, Desafio desafio, String texto) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("titulo", desafio.getTitulo());
        dados.put("turmaId", desafio.getTurma() == null ? null : desafio.getTurma().getId());

        logSistemaService.registrarSucesso(
                autor,
                acao,
                EntidadeLog.DESAFIO,
                desafio.getId(),
                texto + ": \"" + desafio.getTitulo() + "\".",
                dados
        );
    }

    @Transactional(readOnly = true)
    public List<RankingDesafioResponse> ranking(Long desafioId, String username) {
        Usuario usuario = carregarUsuario(username);
        Desafio desafio = carregarDesafio(desafioId);

        exigirAcesso(desafio, usuario);

        // A pergunta aberta fica de fora: a pontuação mudando entregaria quem acertou
        Long perguntaAberta = desafio.getStatus() == StatusDesafio.EM_ANDAMENTO && !perguntaEncerrada(desafio)
                ? perguntaAtual(desafio).getId()
                : null;

        Map<Long, int[]> placar = new LinkedHashMap<>();
        Map<Long, String> nomes = new LinkedHashMap<>();

        for (RespostaDesafio resposta : respostaRepository.buscarTodasDoDesafio(desafioId)) {
            if (resposta.getPergunta().getId().equals(perguntaAberta)) {
                continue;
            }

            Long alunoId = resposta.getUsuario().getId();
            nomes.putIfAbsent(alunoId, resposta.getUsuario().getNomeUsuario());

            int[] pontosEAcertos = placar.computeIfAbsent(alunoId, id -> new int[2]);
            pontosEAcertos[0] += resposta.getPontos();
            pontosEAcertos[1] += Boolean.TRUE.equals(resposta.getCorreta()) ? 1 : 0;
        }

        List<Long> ordenados = placar.keySet().stream()
                .sorted(Comparator
                        .comparingInt((Long id) -> placar.get(id)[0]).reversed()
                        .thenComparing(Comparator.comparingInt((Long id) -> placar.get(id)[1]).reversed())
                        .thenComparing(nomes::get))
                .toList();

        List<RankingDesafioResponse> ranking = new ArrayList<>();
        for (Long alunoId : ordenados) {
            int[] pontosEAcertos = placar.get(alunoId);
            ranking.add(new RankingDesafioResponse(
                    ranking.size() + 1, alunoId, nomes.get(alunoId), pontosEAcertos[0], pontosEAcertos[1]));
        }

        return ranking;
    }

    private PerguntaDesafio montarPergunta(PerguntaDesafioRequest request, Desafio desafio, int ordem) {
        long corretas = request.alternativas().stream()
                .filter(alternativa -> Boolean.TRUE.equals(alternativa.correta()))
                .count();

        if (corretas != 1) {
            throw ApiException.requisicaoInvalida(
                    "ALTERNATIVA_CORRETA_INVALIDA", "Cada pergunta deve ter exatamente uma alternativa correta.");
        }

        PerguntaDesafio pergunta = new PerguntaDesafio();
        pergunta.setDesafio(desafio);
        pergunta.setEnunciado(request.enunciado());
        pergunta.setTempoSegundos(request.tempoSegundos());
        pergunta.setOrdem(ordem);

        for (AlternativaRequest alternativaRequest : request.alternativas()) {
            AlternativaDesafio alternativa = new AlternativaDesafio();
            alternativa.setPergunta(pergunta);
            alternativa.setTexto(alternativaRequest.texto());
            alternativa.setCorreta(alternativaRequest.correta());
            pergunta.getAlternativas().add(alternativa);
        }

        return pergunta;
    }

    private void abrirPergunta(Desafio desafio, int indice) {
        PerguntaDesafio pergunta = desafio.getPerguntas().get(indice);

        desafio.setIndicePerguntaAtual(indice);
        desafio.setPerguntaExpiraEm(LocalDateTime.now(clock).plusSeconds(pergunta.getTempoSegundos()));
    }

    // Quem responde mais rápido ganha mais: de 500 (no fim do tempo) a 1000 pontos (na hora)
    private int calcularPontos(
            Desafio desafio,
            PerguntaDesafio pergunta,
            AlternativaDesafio alternativa,
            LocalDateTime agora
    ) {
        if (!Boolean.TRUE.equals(alternativa.getCorreta())) {
            return 0;
        }

        long tempoTotal = Duration.ofSeconds(pergunta.getTempoSegundos()).toMillis();
        long tempoRestante = Duration.between(agora, desafio.getPerguntaExpiraEm()).toMillis();

        return PONTOS_BASE + (int) Math.round(BONUS_MAXIMO_VELOCIDADE * (double) tempoRestante / tempoTotal);
    }

    private boolean perguntaEncerrada(Desafio desafio) {
        if (LocalDateTime.now(clock).isAfter(desafio.getPerguntaExpiraEm())) {
            return true;
        }

        int alunosDaTurma = desafio.getTurma().getAlunos().size();

        return alunosDaTurma > 0
                && respostaRepository.countByPergunta_Id(perguntaAtual(desafio).getId()) >= alunosDaTurma;
    }

    private EstadoDesafioResponse montarEstado(Desafio desafio, Usuario usuario) {
        int totalPerguntas = desafio.getPerguntas().size();

        if (desafio.getStatus() != StatusDesafio.EM_ANDAMENTO) {
            return new EstadoDesafioResponse(
                    desafio.getId(), desafio.getTitulo(), desafio.getStatus(), null, totalPerguntas,
                    null, null, null, null, null, null, null);
        }

        PerguntaDesafio pergunta = perguntaAtual(desafio);
        boolean encerrada = perguntaEncerrada(desafio);

        RespostaDesafio minhaResposta = respostaRepository
                .findByPergunta_IdAndUsuario_Id(pergunta.getId(), usuario.getId())
                .orElse(null);

        Long alternativaCorreta = !encerrada ? null : pergunta.getAlternativas().stream()
                .filter(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                .map(AlternativaDesafio::getId)
                .findFirst()
                .orElse(null);

        return new EstadoDesafioResponse(
                desafio.getId(),
                desafio.getTitulo(),
                desafio.getStatus(),
                desafio.getIndicePerguntaAtual() + 1,
                totalPerguntas,
                toPerguntaResponse(pergunta),
                encerrada ? 0 : segundosRestantes(desafio),
                encerrada,
                respostaRepository.countByPergunta_Id(pergunta.getId()),
                minhaResposta == null ? null : minhaResposta.getAlternativa().getId(),
                alternativaCorreta,
                minhaResposta == null || !encerrada ? null : minhaResposta.getPontos()
        );
    }

    private int segundosRestantes(Desafio desafio) {
        long restantes = Duration.between(LocalDateTime.now(clock), desafio.getPerguntaExpiraEm()).toMillis();
        return (int) Math.ceil(restantes / 1000.0);
    }

    private static PerguntaDesafio perguntaAtual(Desafio desafio) {
        return desafio.getPerguntas().get(desafio.getIndicePerguntaAtual());
    }

    private void exigirAcesso(Desafio desafio, Usuario usuario) {
        if (TipoUsuario.ehAluno(usuario)
                && !matriculaService.estaMatriculado(desafio.getTurma().getId(), usuario.getId())) {
            throw ApiException.proibido(
                    "ALUNO_FORA_DA_TURMA", "Você não pertence à turma deste desafio.");
        }
    }

    private Desafio carregarDesafio(Long desafioId) {
        return desafioRepository.findById(desafioId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "DESAFIO_NAO_ENCONTRADO", "Desafio não encontrado."));
    }

    private Turma carregarTurma(Long turmaId) {
        return turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));
    }

    /*
     * Quem cria, edita, exclui e conduz (iniciar/avançar) um desafio:
     * - administrador: qualquer desafio;
     * - professor: só os desafios das turmas em que é o professor responsável.
     * Mesma regra das atividades (AtividadeService) e das turmas (TurmaService).
     */
    private static void exigirPodeAlterar(Turma turma, Usuario usuario) {
        if (TipoUsuario.ehAdministrador(usuario)) {
            return;
        }

        boolean professorDaTurma = turma != null
                && turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!professorDaTurma) {
            throw ApiException.proibido("SEM_ACESSO_AO_DESAFIO",
                    "Só o professor da turma ou um administrador pode alterar ou conduzir os desafios dela.");
        }
    }

    private static void exigirAguardando(Desafio desafio) {
        if (desafio.getStatus() != StatusDesafio.AGUARDANDO) {
            throw ApiException.conflito(
                    "DESAFIO_NAO_EDITAVEL", "Só é possível editar um desafio antes de iniciá-lo.");
        }
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private Usuario exigirProfessorOuAdministrador(String username) {
        Usuario usuario = carregarUsuario(username);

        if (!TipoUsuario.ehProfessorOuAdministrador(usuario)) {
            throw ApiException.proibido(
                    "SOMENTE_PROFESSOR_OU_ADMINISTRADOR",
                    "Esta operação é exclusiva para professores e administradores.");
        }

        return usuario;
    }

    private static ApiException perguntaJaRespondida() {
        return ApiException.conflito("PERGUNTA_JA_RESPONDIDA", "Você já respondeu esta pergunta.");
    }

    private static DesafioResponse toResponse(Desafio desafio) {
        return new DesafioResponse(
                desafio.getId(),
                desafio.getTitulo(),
                desafio.getDescricao(),
                desafio.getStatus(),
                new TurmaResponse(desafio.getTurma().getId(), desafio.getTurma().getNome()),
                desafio.getPerguntas().size(),
                desafio.getDataCriacao()
        );
    }

    private static DesafioEdicaoResponse toEdicaoResponse(Desafio desafio) {
        return new DesafioEdicaoResponse(
                desafio.getId(),
                desafio.getTitulo(),
                desafio.getDescricao(),
                desafio.getTurma().getId(),
                desafio.getPerguntas().stream()
                        .map(pergunta -> new PerguntaDesafioRequest(
                                pergunta.getEnunciado(),
                                pergunta.getTempoSegundos(),
                                pergunta.getAlternativas().stream()
                                        .map(alternativa -> new AlternativaRequest(
                                                alternativa.getTexto(), alternativa.getCorreta()))
                                        .toList()))
                        .toList()
        );
    }

    private static PerguntaDesafioResponse toPerguntaResponse(PerguntaDesafio pergunta) {
        return new PerguntaDesafioResponse(
                pergunta.getId(),
                pergunta.getEnunciado(),
                pergunta.getTempoSegundos(),
                pergunta.getAlternativas().stream()
                        .map(alternativa -> new AlternativaResponse(alternativa.getId(), alternativa.getTexto()))
                        .toList()
        );
    }
}
