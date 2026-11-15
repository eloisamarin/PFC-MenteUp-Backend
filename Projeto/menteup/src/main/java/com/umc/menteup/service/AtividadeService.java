package com.umc.menteup.service;

import com.umc.menteup.dto.AtividadeAlunoResponse;
import com.umc.menteup.dto.ProgressoAtividadeResponse;
import com.umc.menteup.dto.QuestionarioResponse;
import com.umc.menteup.dto.ResultadoAtividadeResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.ProgressoAtividade;
import com.umc.menteup.model.ResultadoAtividade;
import com.umc.menteup.model.StatusAlunoAtividade;
import com.umc.menteup.model.StatusAtividade;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.template.TemplateResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.umc.menteup.model.Usuario;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AtividadeService {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ResultadoAtividadeRepository resultadoAtividadeRepository;

    @Autowired
    private ProgressoAtividadeRepository progressoAtividadeRepository;

    @Autowired
    private MatriculaService matriculaService;

    @Autowired
    private TemplateResolver templateResolver;

    public List<Atividade> listarTodas() {
        return atividadeRepository.findAll();
    }

    public Optional<Atividade> buscarPorId(Long id) {
        return atividadeRepository.findById(id);
    }

    public List<Atividade> buscarPorTitulo(String titulo) {
        return atividadeRepository
                .findAllByTituloContainingIgnoreCase(titulo);
    }

    // Lista completa (com questões e respostas certas) só para quem cria atividades
    // Administrador vê todas e professor só as das turmas dele. Título nulo traz todas
    @Transactional(readOnly = true)
    public List<Atividade> listarParaGestao(String username, String titulo) {
        Usuario usuario = carregarUsuario(username);

        if (TipoUsuario.ehAdministrador(usuario)) {
            return titulo == null
                    ? atividadeRepository.findAll()
                    : atividadeRepository.findAllByTituloContainingIgnoreCase(titulo);
        }

        if (TipoUsuario.ehProfessor(usuario)) {
            return titulo == null
                    ? atividadeRepository.findDoProfessor(usuario.getId())
                    : atividadeRepository.findDoProfessorPorTitulo(usuario.getId(), titulo);
        }

        // O aluno tem a lista própria em /alunos/me/atividades, sem as respostas certas
        throw ApiException.proibido("SOMENTE_PROFESSOR_OU_ADMINISTRADOR",
                "Esta listagem é exclusiva para professores e administradores.");
    }

    @Transactional
    public Atividade criarAtividade(Atividade atividade) {

        List<Turma> turmas = carregarTurmas(atividade);
        exigirMesmoProfessor(turmas);

        atividade.setTurmas(turmas);

        Atividade novaAtividade = atividadeRepository.save(atividade);

        ///Rascunho não aparece para o aluno, então só avisa quando a atividade já nasce publicada
        if (novaAtividade.getStatus() != StatusAtividade.PENDENTE) {
            enviarNotificacoes(novaAtividade, turmas);
        }

        return novaAtividade;
    }

    ///Busca no banco cada turma informada, sem repetir, e recusa se faltar alguma
    public List<Turma> carregarTurmas(Atividade atividade) {
        List<Turma> informadas = atividade.getTurmas();

        if (informadas == null || informadas.isEmpty()
                || informadas.stream().anyMatch(t -> t == null || t.getId() == null)) {

            throw ApiException.requisicaoInvalida(
                    "TURMA_OBRIGATORIA",
                    "A turma da atividade é obrigatória."
            );
        }

        Set<Long> ids = new LinkedHashSet<>();
        informadas.forEach(t -> ids.add(t.getId()));

        List<Turma> turmas = new ArrayList<>();
        for (Long id : ids) {
            turmas.add(turmaRepository.findById(id).orElseThrow(() ->
                    ApiException.naoEncontrado(
                            "TURMA_NAO_ENCONTRADA",
                            "Turma não encontrada."
                    )
            ));
        }

        return turmas;
    }

    ///Uma atividade só pode ser dividida entre turmas do mesmo professor
    public static void exigirMesmoProfessor(List<Turma> turmas) {
        Set<Long> professores = new HashSet<>();
        for (Turma turma : turmas) {
            professores.add(turma.getProfessor() == null ? null : turma.getProfessor().getId());
        }

        if (professores.size() > 1) {
            throw ApiException.requisicaoInvalida(
                    "TURMAS_DE_PROFESSORES_DIFERENTES",
                    "Uma atividade só pode ser atribuída a turmas do mesmo professor."
            );
        }
    }

    ///Avisa os alunos das turmas quando o professor publica uma atividade
    @Transactional(readOnly = true)
    public void notificarPublicacao(Atividade atividade) {
        List<Turma> turmas = new ArrayList<>();
        for (Turma turma : atividade.getTurmas()) {
            if (turma != null && turma.getId() != null) {
                turmaRepository.findById(turma.getId()).ifPresent(turmas::add);
            }
        }
        enviarNotificacoes(atividade, turmas);
    }

    ///Junta os alunos de todas as turmas, avisando uma vez só quem está em mais de uma
    private void enviarNotificacoes(Atividade novaAtividade, List<Turma> turmas) {
        List<Usuario> alunos = new ArrayList<>();
        Set<Long> jaAvisados = new HashSet<>();
        for (Turma turma : turmas) {
            if (turma.getAlunos() == null) {
                continue;
            }
            for (Usuario aluno : turma.getAlunos()) {
                if (aluno.getId() == null || jaAvisados.add(aluno.getId())) {
                    alunos.add(aluno);
                }
            }
        }
        avisarAlunos(novaAtividade, alunos);
    }

    ///Manda a notificação para cada aluno que tem o celular cadastrado
    private void avisarAlunos(Atividade novaAtividade, Collection<Usuario> alunos) {

        System.out.println(
                "QUANTIDADE DE ALUNOS: "
                        + alunos.size()
        );

        for (Usuario aluno : alunos) {

            System.out.println(
                    "ALUNO: "
                            + aluno.getUsuario()
            );

            if (aluno.getFcmToken() != null &&
                    !aluno.getFcmToken().isBlank()) {

                System.out.println(
                        "Enviando notificação para o aluno..."
                );

                ///Se a notificação falhar, a atividade continua salva
                try {
                    notificacaoService.notificarNovaAtividade(
                            novaAtividade,
                            aluno.getFcmToken()
                    );
                } catch (Exception e) {
                    System.err.println(
                            "Falha ao notificar o aluno "
                                    + aluno.getUsuario() + ": " + e.getMessage()
                    );
                }

            } else {

                System.out.println(
                        "Aluno sem FCM Token."
                );
            }
        }
    }

    public Optional<Atividade> atualizarAtividade(
            Long id,
            Atividade atividadeAtualizada) {

        Optional<Atividade> atividadeExistente =
                atividadeRepository.findById(id);

        if (atividadeExistente.isEmpty()) {
            return Optional.empty();
        }

        Atividade atividade = atividadeExistente.get();

        atividade.setTitulo(atividadeAtualizada.getTitulo());
        atividade.setDescricao(atividadeAtualizada.getDescricao());
        atividade.setPontuacao(atividadeAtualizada.getPontuacao());
        atividade.setStatus(atividadeAtualizada.getStatus());
        atividade.setTurmas(atividadeAtualizada.getTurmas());
        atividade.setUsuario(atividadeAtualizada.getUsuario());
        atividade.setPerguntas(atividadeAtualizada.getPerguntas());

        return Optional.of(
                atividadeRepository.save(atividade)
        );
    }

    /*
     * Quem pode criar, editar ou excluir uma atividade (e as questões dela):
     * - administrador: qualquer atividade;
     * - professor: só as atividades das turmas dele;
     * - aluno: nenhuma (o SecurityConfig já barra antes de chegar aqui).
     * Mesma regra das turmas (TurmaService).
     */
    public Usuario exigirPodeAlterar(Turma turma, String username) {
        Usuario usuario = carregarUsuario(username);

        if (!TipoUsuario.ehAdministrador(usuario) && !ehProfessorDaTurma(turma, usuario)) {
            throw semAcessoAAtividade();
        }

        return usuario;
    }

    ///Mesma regra, para a atividade: o professor precisa ser o dono de todas as turmas dela
    public Usuario exigirPodeAlterarAtividade(Atividade atividade, String username) {
        Usuario usuario = carregarUsuario(username);

        if (TipoUsuario.ehAdministrador(usuario)) {
            return usuario;
        }

        List<Turma> turmas = atividade.getTurmas();
        boolean professorDeTodas = turmas != null && !turmas.isEmpty()
                && turmas.stream().allMatch(turma -> ehProfessorDaTurma(turma, usuario));

        if (!professorDeTodas) {
            throw semAcessoAAtividade();
        }

        return usuario;
    }

    private static boolean ehProfessorDaTurma(Turma turma, Usuario usuario) {
        return TipoUsuario.ehProfessor(usuario)
                && turma != null
                && turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());
    }

    private static ApiException semAcessoAAtividade() {
        return ApiException.proibido("SEM_ACESSO_A_ATIVIDADE",
                "Só o professor da turma ou um administrador pode alterar as atividades dela.");
    }

    public boolean deletarAtividade(Long id) {

        if (!atividadeRepository.existsById(id)) {
            return false;
        }

        atividadeRepository.deleteById(id);

        return true;
    }

    @Transactional(readOnly = true)
    public QuestionarioResponse buscarQuestionario(Long atividadeId, String username) {
        Usuario usuario = carregarUsuario(username);

        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(AtividadeService::atividadeNaoEncontrada);

        ResultadoAtividadeResponse meuResultado = null;
        ProgressoAtividadeResponse meuProgresso = null;

        if (TipoUsuario.ehAluno(usuario)) {
            matriculaService.exigirMatricula(atividade, usuario);

            if (atividade.getStatus() == StatusAtividade.PENDENTE) {
                throw atividadeNaoEncontrada();
            }

            int totalQuestoes = atividade.getPerguntas().size();

            meuResultado = resultadoAtividadeRepository
                    .findByUsuario_IdAndAtividade_Id(usuario.getId(), atividadeId)
                    .map(resultado -> QuestionarioMapper.toResultado(resultado, totalQuestoes))
                    .orElse(null);

            if (meuResultado == null) {
                meuProgresso = progressoAtividadeRepository
                        .findByUsuario_IdAndAtividade_Id(usuario.getId(), atividadeId)
                        .map(progresso -> QuestionarioMapper.toProgresso(progresso, totalQuestoes))
                        .orElse(null);
            }
        }

        ///Professor e administrador vão receber o gabarito para visualização e editação
        if (!TipoUsuario.ehAluno(usuario)) {
            return QuestionarioMapper.toQuestionarioProfessor(atividade);
        }

        ///O que vai aparecer para o aluno agora depende do tamplate
        return QuestionarioMapper.toQuestionario(
                atividade,
                templateResolver.resolver(atividade).perguntasParaAluno(atividade),
                meuResultado,
                meuProgresso
        );
    }

    @Transactional(readOnly = true)
    public List<AtividadeAlunoResponse> listarParaAluno(String username) {
        Usuario aluno = carregarUsuario(username);

        if (!TipoUsuario.ehAluno(aluno)) {
            throw ApiException.proibido("SOMENTE_ALUNO", "Esta listagem é exclusiva para alunos.");
        }

        List<Atividade> atividades = atividadeRepository
                .findVisiveisParaAluno(aluno.getId(), StatusAtividade.PENDENTE);

        if (atividades.isEmpty()) {
            return List.of();
        }

        List<Long> ids = atividades.stream().map(Atividade::getId).toList();

        Map<Long, ResultadoAtividade> resultadoPorAtividade = resultadoAtividadeRepository
                .findAllByUsuario_IdAndAtividade_IdIn(aluno.getId(), ids)
                .stream()
                .collect(Collectors.toMap(r -> r.getAtividade().getId(), Function.identity()));

        Map<Long, ProgressoAtividade> progressoPorAtividade = progressoAtividadeRepository
                .findAllByUsuario_IdAndAtividade_IdIn(aluno.getId(), ids)
                .stream()
                .collect(Collectors.toMap(p -> p.getAtividade().getId(), Function.identity()));

        return atividades.stream()
                .map(atividade -> montarAtividadeAluno(
                        atividade,
                        resultadoPorAtividade.get(atividade.getId()),
                        progressoPorAtividade.get(atividade.getId())
                ))
                .toList();
    }

    private AtividadeAlunoResponse montarAtividadeAluno(
            Atividade atividade, ResultadoAtividade resultado, ProgressoAtividade progresso
    ) {
        if (resultado != null) {
            return new AtividadeAlunoResponse(
                    QuestionarioMapper.toAtividadeResponse(atividade),
                    StatusAlunoAtividade.CONCLUIDA,
                    resultado.getAcertos(),
                    resultado.getPontuacao(),
                    100
            );
        }

        if (progresso != null) {
            int totalQuestoes = atividade.getPerguntas().size();
            int respondidas = progresso.getRespostas().size();
            int percentual = totalQuestoes == 0
                    ? 0
                    : (int) Math.round((respondidas * 100.0) / totalQuestoes);

            return new AtividadeAlunoResponse(
                    QuestionarioMapper.toAtividadeResponse(atividade),
                    StatusAlunoAtividade.EM_ANDAMENTO,
                    null,
                    null,
                    percentual
            );
        }

        return new AtividadeAlunoResponse(
                QuestionarioMapper.toAtividadeResponse(atividade),
                StatusAlunoAtividade.PENDENTE,
                null,
                null,
                0
        );
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private static ApiException atividadeNaoEncontrada() {
        return ApiException.naoEncontrado("ATIVIDADE_NAO_ENCONTRADA", "Atividade não encontrada.");
    }
}
