package com.umc.menteup.service;

import com.umc.menteup.dto.SolicitacaoTurmaDtos.Solicitacao;
import com.umc.menteup.dto.SolicitacaoTurmaDtos.TurmaDoAluno;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.SolicitacaoTurma;
import com.umc.menteup.model.SolicitacaoTurma.Status;
import com.umc.menteup.model.SolicitacaoTurma.Tipo;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.SolicitacaoTurmaRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * O aluno não entra nem sai da turma sozinho: ele SOLICITA,
 * e o professor da turma (ou um administrador) aprova ou recusa.
 */
@Service
public class SolicitacaoTurmaService {

    private final SolicitacaoTurmaRepository solicitacaoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaService logSistemaService;

    public SolicitacaoTurmaService(
            SolicitacaoTurmaRepository solicitacaoRepository,
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            LogSistemaService logSistemaService) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logSistemaService = logSistemaService;
    }

    // ------------------------------------------------------------- Aluno

    // Todas as turmas, dizendo se o aluno está nela e se tem pedido aberto
    @Transactional(readOnly = true)
    public List<TurmaDoAluno> turmasDoAluno(String username) {
        Usuario aluno = carregarAluno(username);

        Map<Long, SolicitacaoTurma> pendentes = new java.util.HashMap<>();
        solicitacaoRepository.findAllByAluno_IdOrderByCriadaEmDesc(aluno.getId()).stream()
                .filter(SolicitacaoTurma::estaPendente)
                .forEach(s -> pendentes.putIfAbsent(s.getTurma().getId(), s));

        return turmaRepository.findAll().stream()
                .sorted(Comparator.comparing(Turma::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(turma -> {
                    SolicitacaoTurma pendente = pendentes.get(turma.getId());
                    return new TurmaDoAluno(
                            turma.getId(),
                            turma.getNome(),
                            turma.getProfessor() == null ? null : turma.getProfessor().getNomeUsuario(),
                            estaMatriculado(turma.getId(), aluno.getId()),
                            pendente == null ? null : pendente.getId(),
                            pendente == null ? null : pendente.getTipo());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Solicitacao> minhasSolicitacoes(String username) {
        Usuario aluno = carregarAluno(username);

        return solicitacaoRepository.findAllByAluno_IdOrderByCriadaEmDesc(aluno.getId()).stream()
                .map(Solicitacao::de)
                .toList();
    }

    @Transactional
    public Solicitacao solicitar(String username, Long turmaId, Tipo tipo) {
        Usuario aluno = carregarAluno(username);
        Turma turma = carregarTurma(turmaId);
        boolean matriculado = estaMatriculado(turmaId, aluno.getId());

        if (tipo == Tipo.ENTRADA && matriculado) {
            throw ApiException.conflito("JA_MATRICULADO", "Você já está nesta turma.");
        }
        if (tipo == Tipo.SAIDA && !matriculado) {
            throw ApiException.conflito("NAO_MATRICULADO", "Você não está nesta turma.");
        }
        if (solicitacaoRepository.existsByTurma_IdAndAluno_IdAndStatus(turmaId, aluno.getId(), Status.PENDENTE)) {
            throw ApiException.conflito("SOLICITACAO_PENDENTE",
                    "Você já tem um pedido aguardando resposta para esta turma.");
        }

        SolicitacaoTurma salva = solicitacaoRepository.save(new SolicitacaoTurma(turma, aluno, tipo));
        return Solicitacao.de(salva);
    }

    // O aluno desiste de um pedido que ainda não foi respondido
    @Transactional
    public void cancelar(String username, Long solicitacaoId) {
        Usuario aluno = carregarAluno(username);

        SolicitacaoTurma solicitacao = solicitacaoRepository.findByIdAndAluno_Id(solicitacaoId, aluno.getId())
                .orElseThrow(this::naoEncontrada);

        exigirPendente(solicitacao);
        solicitacao.setStatus(Status.CANCELADA);
        solicitacao.setRespondidaEm(LocalDateTime.now());
    }

    // ------------------------------------------------------------- Professor e administrador

    @Transactional(readOnly = true)
    public List<Solicitacao> pendentesDaTurma(String username, Long turmaId) {
        Usuario usuario = carregarUsuario(username);
        exigirAcessoATurma(carregarTurma(turmaId), usuario);

        return solicitacaoRepository.findAllByTurma_IdAndStatusOrderByCriadaEmAsc(turmaId, Status.PENDENTE).stream()
                .map(Solicitacao::de)
                .toList();
    }

    @Transactional
    public void aprovar(String username, Long turmaId, Long solicitacaoId) {
        Usuario autor = carregarUsuario(username);
        SolicitacaoTurma solicitacao = carregarDaTurma(autor, turmaId, solicitacaoId);
        Turma turma = solicitacao.getTurma();
        Usuario aluno = solicitacao.getAluno();

        if (solicitacao.getTipo() == Tipo.ENTRADA) {
            if (!estaMatriculado(turma.getId(), aluno.getId())) {
                turma.getAlunos().add(aluno);
            }
            registrarLog(autor, AcaoLog.ADICIONAR_ALUNO_TURMA, turma, aluno,
                    "Pedido aprovado: aluno \"" + aluno.getNomeUsuario()
                            + "\" entrou na turma \"" + turma.getNome() + "\".");
        } else {
            turma.getAlunos().removeIf(matriculado -> matriculado.getId().equals(aluno.getId()));
            registrarLog(autor, AcaoLog.REMOVER_ALUNO_TURMA, turma, aluno,
                    "Pedido aprovado: aluno \"" + aluno.getNomeUsuario()
                            + "\" saiu da turma \"" + turma.getNome() + "\".");
        }

        responder(solicitacao, autor, Status.APROVADA);
    }

    @Transactional
    public void recusar(String username, Long turmaId, Long solicitacaoId) {
        Usuario autor = carregarUsuario(username);
        SolicitacaoTurma solicitacao = carregarDaTurma(autor, turmaId, solicitacaoId);

        responder(solicitacao, autor, Status.RECUSADA);
    }

    // ------------------------------------------------------------- Auxiliares

    private SolicitacaoTurma carregarDaTurma(Usuario autor, Long turmaId, Long solicitacaoId) {
        exigirAcessoATurma(carregarTurma(turmaId), autor);

        SolicitacaoTurma solicitacao = solicitacaoRepository.findByIdAndTurma_Id(solicitacaoId, turmaId)
                .orElseThrow(this::naoEncontrada);

        exigirPendente(solicitacao);
        return solicitacao;
    }

    private void responder(SolicitacaoTurma solicitacao, Usuario autor, Status status) {
        solicitacao.setStatus(status);
        solicitacao.setRespondidaEm(LocalDateTime.now());
        solicitacao.setRespondidaPor(autor);
    }

    // Administrador: qualquer turma. Professor: só as turmas dele
    private void exigirAcessoATurma(Turma turma, Usuario usuario) {
        boolean professorDaTurma = turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!TipoUsuario.ehAdministrador(usuario) && !professorDaTurma) {
            throw ApiException.proibido("SEM_ACESSO_A_TURMA",
                    "Só o professor da turma ou um administrador pode responder aos pedidos.");
        }
    }

    private static void exigirPendente(SolicitacaoTurma solicitacao) {
        if (!solicitacao.estaPendente()) {
            throw ApiException.conflito("SOLICITACAO_JA_RESPONDIDA", "Este pedido já foi respondido ou cancelado.");
        }
    }

    private boolean estaMatriculado(Long turmaId, Long alunoId) {
        return turmaRepository.contarMatricula(turmaId, alunoId) > 0;
    }

    private void registrarLog(Usuario autor, AcaoLog acao, Turma turma, Usuario aluno, String descricao) {
        logSistemaService.registrarSucesso(autor, acao, EntidadeLog.TURMA, turma.getId(), descricao,
                Map.of("alunoId", aluno.getId(), "turmaId", turma.getId()));
    }

    private ApiException naoEncontrada() {
        return ApiException.naoEncontrado("SOLICITACAO_NAO_ENCONTRADA", "Pedido não encontrado.");
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private Usuario carregarAluno(String username) {
        Usuario usuario = carregarUsuario(username);

        if (!TipoUsuario.ehAluno(usuario)) {
            throw ApiException.proibido("SOMENTE_ALUNO", "Esta operação é exclusiva para alunos.");
        }
        return usuario;
    }

    private Turma carregarTurma(Long turmaId) {
        return Optional.ofNullable(turmaId)
                .flatMap(turmaRepository::findById)
                .orElseThrow(() -> ApiException.naoEncontrado("TURMA_NAO_ENCONTRADA", "Turma não encontrada."));
    }
}
