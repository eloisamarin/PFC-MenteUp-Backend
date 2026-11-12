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
import com.umc.menteup.service.NotificacaoService;
import com.umc.menteup.template.TemplateResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.umc.menteup.model.Usuario;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Collections;
import java.util.Map;
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

    @Transactional
    public Atividade criarAtividade(Atividade atividade) {

        if (atividade.getTurma() == null ||
                atividade.getTurma().getId() == null) {

            throw ApiException.requisicaoInvalida(
                    "TURMA_OBRIGATORIA",
                    "A turma da atividade é obrigatória."
            );
        }

        Turma turma = turmaRepository.findById(
                atividade.getTurma().getId()
        ).orElseThrow(() ->
                ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA",
                        "Turma não encontrada."
                )
        );

        atividade.setTurma(turma);

        Atividade novaAtividade = atividadeRepository.save(atividade);

        ///Rascunho não aparece para o aluno, então só avisa quando a atividade já nasce publicada
        if (novaAtividade.getStatus() != StatusAtividade.PENDENTE) {
            enviarNotificacoes(novaAtividade, turma);
        }

        return novaAtividade;
    }

    ///Avisa os alunos da turma quando o professor publica uma atividade
    @Transactional(readOnly = true)
    public void notificarPublicacao(Atividade atividade) {
        if (atividade.getTurma() == null || atividade.getTurma().getId() == null) {
            return;
        }
        turmaRepository.findById(atividade.getTurma().getId())
                .ifPresent(turma -> enviarNotificacoes(atividade, turma));
    }

    ///Manda a notificação para cada aluno da turma que tem o celular cadastrado
    private void enviarNotificacoes(Atividade novaAtividade, Turma turma) {
        List<Usuario> alunos = turma.getAlunos() == null
                ? Collections.emptyList()
                : turma.getAlunos();

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
        atividade.setTurma(atividadeAtualizada.getTurma());
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
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (TipoUsuario.ehAdministrador(usuario)) {
            return usuario;
        }

        boolean professorDaTurma = TipoUsuario.ehProfessor(usuario)
                && turma != null
                && turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!professorDaTurma) {
            throw ApiException.proibido("SEM_ACESSO_A_ATIVIDADE",
                    "Só o professor da turma ou um administrador pode alterar as atividades dela.");
        }

        return usuario;
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
