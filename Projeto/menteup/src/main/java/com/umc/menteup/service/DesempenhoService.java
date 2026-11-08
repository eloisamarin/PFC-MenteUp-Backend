package com.umc.menteup.service;

import com.umc.menteup.dto.*;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DesempenhoService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final ResultadoAtividadeRepository resultadoAtividadeRepository;
    private final ProgressoAtividadeRepository progressoAtividadeRepository;
    private final RespostaDesafioRepository respostaDesafioRepository;
    private final LogSistemaService logSistemaService;

    public DesempenhoService(
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            AtividadeRepository atividadeRepository,
            ResultadoAtividadeRepository resultadoAtividadeRepository,
            RespostaDesafioRepository respostaDesafioRepository,
            ProgressoAtividadeRepository progressoAtividadeRepository,
            LogSistemaService logSistemaService
    ) {
        this.logSistemaService = logSistemaService;
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.resultadoAtividadeRepository = resultadoAtividadeRepository;
        this.progressoAtividadeRepository = progressoAtividadeRepository;
        this.respostaDesafioRepository = respostaDesafioRepository;

    }

    //
    // Somente o professor da turma ou o administrador tem acesso a desempenho
    private void exigirAcessoATurma(Turma turma, Usuario usuario) {
        boolean professorDaTurma = turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!TipoUsuario.ehAdministrador(usuario) && !professorDaTurma) {
            throw ApiException.proibido(
                    "SEM_ACESSO_A_TURMA", "Você não tem acesso ao desempenho desta turma.");
        }
    }
    private Usuario carregarUsuario(String username)
    {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"
                ));
    }
    /*
    - O Administrador vizualiza as informações de  todas turmas.
    - O Professor vizualiza as informações as turmas que esteja vinculado.
    - O Aluno nao acesso autorizado.

     */

    @Transactional(readOnly = true)
    public List<TurmaResponse> listarTurmas(String username) {
        Usuario usuario = carregarUsuario(username);

        List<Turma> turmas;

        if (TipoUsuario.ehAdministrador(usuario)) {
            turmas = turmaRepository.findAll();
        } else if (TipoUsuario.ehProfessor(usuario)) {
            turmas = turmaRepository.findAllByProfessor_Id(usuario.getId());
        } else {
            throw ApiException.proibido(
                    "SOMENTE_PROFESSOR_OU_ADMINISTRADOR",
                    "Esta operação é exclusiva para professores e administradores.");
        }

        return turmas.stream()
                .map(turma -> new TurmaResponse(turma.getId(), turma.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DesempenhoTurmaResponse buscarDesempenho(Long turmaId, String username) {

        // Carregar os dados
        Usuario usuario = carregarUsuario(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        // Verifica a permissao
        exigirAcessoATurma(turma, usuario);

        // Pesquisa

        Map<Long, ResumoDesempenhoAluno> resumos = resultadoAtividadeRepository
                .buscarProgressoDaTurma(turmaId)
                .stream()
                .collect(Collectors.toMap(ResumoDesempenhoAluno::alunoId, Function.identity()));

        long totalAtividades = atividadeRepository
                .countByTurma_IdAndStatusNot(turmaId, StatusAtividade.PENDENTE);


        // Lista Resultado dos Alunos
        List<DesempenhoAlunoResponse> alunos = turma.getAlunos().stream()
                .map(aluno -> {
                    ResumoDesempenhoAluno resumo = resumos.getOrDefault(
                            aluno.getId(),
                            new ResumoDesempenhoAluno(aluno.getId(), 0L, 0L, 0L));

                    return new DesempenhoAlunoResponse(
                            aluno.getId(),
                            aluno.getNomeUsuario(),
                            resumo.pontuacao(),
                            resumo.acertos(),
                            resumo.atividadesConcluidas());
                })
                .sorted(Comparator.comparing(DesempenhoAlunoResponse::pontuacao).reversed())
                .toList();

        //Calcula os totais e mostra o resultado

        long pontuacaoTotal = alunos.stream()
                .mapToLong(DesempenhoAlunoResponse::pontuacao)
                .sum();

        long conclusoes = alunos.stream()
                .mapToLong(DesempenhoAlunoResponse::atividadesConcluidas)
                .sum();

        double mediaPontuacao = alunos.isEmpty()
                ? 0.0
                : Math.round(pontuacaoTotal * 10.0 / alunos.size()) / 10.0;

        long conclusoesPossiveis = alunos.size() * totalAtividades;

        int taxaParticipacao = conclusoesPossiveis == 0
                ? 0
                : (int) Math.round(conclusoes * 100.0 / conclusoesPossiveis);

        return new DesempenhoTurmaResponse(
                turma.getId(),
                turma.getNome(),
                alunos.size(),
                totalAtividades,
                mediaPontuacao,
                taxaParticipacao,
                alunos);
    }
    @Transactional
    public ComentarioResponse comentarResultado(
            Long turmaId, Long resultadoId, String texto, String username) {

        Usuario usuario = carregarUsuario(username);

        ResultadoAtividade resultado = resultadoAtividadeRepository.findById(resultadoId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "RESULTADO_NAO_ENCONTRADO", "Resultado não encontrado."));

        Turma turma = resultado.getAtividade().getTurma();

        // O resultado precisa ser da turma informada na rota
        if (turma == null || !turma.getId().equals(turmaId)) {
            throw ApiException.naoEncontrado(
                    "RESULTADO_NAO_ENCONTRADO", "Resultado não encontrado nesta turma.");
        }

        exigirAcessoATurma(turma, usuario);

        resultado.setComentarioProfessor(texto.trim());
        resultado.setComentadoEm(LocalDateTime.now());
        resultado.setComentadoPor(usuario);

        logSistemaService.registrarSucesso(
                usuario,
                AcaoLog.COMENTAR_RESULTADO,
                EntidadeLog.ATIVIDADE,
                resultado.getAtividade().getId(),
                "Comentário feito na atividade \"" + resultado.getAtividade().getTitulo() + "\".",
                Map.of("resultadoId", resultado.getId(), "turmaId", turmaId)
        );

        return new ComentarioResponse(
                resultado.getId(),
                resultado.getComentarioProfessor(),
                usuario.getNomeUsuario(),
                resultado.getComentadoEm());
    }
    /*
     * Desempenho individual de um aluno na turma:
     * atividades concluídas (com cada pergunta e o comentário) e desafios (com cada pergunta).
     */
    @Transactional(readOnly = true)
    public DesempenhoIndividualResponse buscarDesempenhoDoAluno(Long turmaId, Long alunoId, String username) {
        Usuario usuario = carregarUsuario(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        exigirAcessoATurma(turma, usuario);

        // Só mostra alunos que estão matriculados nesta turma
        if (turmaRepository.contarMatricula(turmaId, alunoId) == 0) {
            throw ApiException.naoEncontrado(
                    "ALUNO_NAO_ENCONTRADO_NA_TURMA", "Aluno não encontrado nesta turma.");
        }

        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ALUNO_NAO_ENCONTRADO_NA_TURMA", "Aluno não encontrado nesta turma."));

        List<AtividadeDoAlunoResponse> atividades = resultadoAtividadeRepository
                .findAllByUsuario_IdAndAtividade_Turma_Id(alunoId, turmaId)
                .stream()
                .map(resultado -> montarAtividade(resultado, alunoId))
                .toList();

        return new DesempenhoIndividualResponse(
                aluno.getId(),
                aluno.getNomeUsuario(),
                turma.getId(),
                turma.getNome(),
                atividades,
                montarDesafios(alunoId, turmaId));
    }

    // Uma atividade concluída, com as perguntas
    private AtividadeDoAlunoResponse montarAtividade(ResultadoAtividade resultado, Long alunoId) {
        Atividade atividade = resultado.getAtividade();
        TipoAtividade tipo = TipoAtividade.ouPadrao(atividade.getTipo());

        // As respostas ficam no progresso, nas atividades antigas ele foi apagado
        Map<Long, RespostaAtividade> respostaPorPergunta = progressoAtividadeRepository
                .findByUsuario_IdAndAtividade_Id(alunoId, atividade.getId())
                .map(progresso -> progresso.getRespostas().stream()
                        .collect(Collectors.toMap(r -> r.getPergunta().getId(), Function.identity())))
                .orElse(Map.of());

        List<QuestaoAlunoResponse> questoes = respostaPorPergunta.isEmpty()
                ? List.of()
                : atividade.getPerguntas().stream()
                  .map(pergunta -> montarQuestaoDaAtividade(pergunta, respostaPorPergunta.get(pergunta.getId()), tipo))
                  .toList();

        return new AtividadeDoAlunoResponse(
                resultado.getId(),
                atividade.getId(),
                atividade.getTitulo(),
                tipo.name(),
                resultado.getPontuacao(),
                atividade.getPontuacao(),
                resultado.getAcertos(),
                atividade.getPerguntas().size(),
                resultado.getConcluida_em(),
                resultado.getComentarioProfessor(),
                resultado.getComentadoPor() == null ? null : resultado.getComentadoPor().getNomeUsuario(),
                questoes);
    }

    private QuestaoAlunoResponse montarQuestaoDaAtividade(Pergunta pergunta, RespostaAtividade resposta, TipoAtividade tipo) {
        // A resposta correta só é montada nos tipos de múltipla escolha
        boolean multiplaEscolha = tipo == TipoAtividade.QUIZ || tipo == TipoAtividade.VERDADEIRO_FALSO;

        String respostaCorreta = !multiplaEscolha ? null : pergunta.getAlternativas().stream()
                                                           .filter(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                                                           .map(Alternativa::getTexto)
                                                           .collect(Collectors.joining(", "));

        return new QuestaoAlunoResponse(
                pergunta.getEnunciado(),
                resposta == null ? null : resposta.getCorreta(),
                resposta == null ? null : descreverResposta(pergunta, resposta),
                respostaCorreta);
    }

    // Quiz, V/F e associação: a alternativa escolhida. Ordenação e lacunas: os itens na ordem do aluno
    private String descreverResposta(Pergunta pergunta, RespostaAtividade resposta) {
        if (resposta.getAlternativa() != null) {
            return resposta.getAlternativa().getTexto();
        }

        Map<Long, String> textos = pergunta.getAlternativas().stream()
                .collect(Collectors.toMap(Alternativa::getId, Alternativa::getTexto));

        return resposta.getAlternativasIds().stream()
                .map(id -> textos.getOrDefault(id, "?"))
                .collect(Collectors.joining(", "));
    }

    // Agrupa as respostas do aluno por desafio, mantendo a ordem
    private List<DesafioDoAlunoResponse> montarDesafios(Long alunoId, Long turmaId) {
        Map<Desafio, List<RespostaDesafio>> porDesafio = respostaDesafioRepository
                .buscarDoAlunoNaTurma(alunoId, turmaId)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getPergunta().getDesafio(), LinkedHashMap::new, Collectors.toList()));

        return porDesafio.entrySet().stream()
                .map(item -> {
                    Desafio desafio = item.getKey();
                    List<RespostaDesafio> respostas = item.getValue();

                    List<QuestaoAlunoResponse> questoes = respostas.stream()
                            .map(resposta -> new QuestaoAlunoResponse(
                                    resposta.getPergunta().getEnunciado(),
                                    resposta.getCorreta(),
                                    resposta.getAlternativa().getTexto(),
                                    resposta.getPergunta().getAlternativas().stream()
                                            .filter(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                                            .map(AlternativaDesafio::getTexto)
                                            .findFirst()
                                            .orElse(null)))
                            .toList();

                    return new DesafioDoAlunoResponse(
                            desafio.getId(),
                            desafio.getTitulo(),
                            respostas.stream().mapToInt(RespostaDesafio::getPontos).sum(),
                            (int) respostas.stream().filter(r -> Boolean.TRUE.equals(r.getCorreta())).count(),
                            desafio.getPerguntas().size(),
                            questoes);
                })
                .toList();
    }



}

