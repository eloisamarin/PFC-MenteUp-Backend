package com.umc.menteup.service;

import com.umc.menteup.dto.ConclusaoAtividadeRequest;
import com.umc.menteup.dto.RespostaQuestaoRequest;
import com.umc.menteup.dto.ResultadoAtividadeResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.umc.menteup.model.TipoUsuario.ehAluno;

@Service
public class ResultadoAtividadeService {
    private final AtividadeRepository atividadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ResultadoAtividadeRepository resultadoAtividadeRepository;
    private final MatriculaService matriculaService;
    private final ProgressoAtividadeRepository progressoRepository;
    private final LogSistemaService logSistemaService;

    public ResultadoAtividadeService(
            AtividadeRepository atividadeRepository,
            UsuarioRepository usuarioRepository,
            ResultadoAtividadeRepository resultadoAtividadeRepository,
            MatriculaService matriculaService,
            ProgressoAtividadeRepository progressoRepository,
            LogSistemaService logSistemaService
    ) {
        this.atividadeRepository = atividadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.resultadoAtividadeRepository = resultadoAtividadeRepository;
        this.matriculaService = matriculaService;
        this.progressoRepository = progressoRepository;
        this.logSistemaService = logSistemaService;
    }

    @Transactional
    public ResultadoAtividadeResponse concluirAtividade(
            Long atividadeId,
            String username
    ) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO",
                        "Usuário não encontrado."
                ));

        if (!com.umc.menteup.model.TipoUsuario.ehAluno(usuario)) {
            throw ApiException.proibido(
                    "SOMENTE_ALUNO",
                    "Somente alunos podem responder as atividades."
            );
        }

        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ATIVIDADE_NAO_ENCONTRADA",
                        "Atividade não encontrada."
                ));

        matriculaService.exigirMatricula(atividade, usuario);

        if (resultadoAtividadeRepository.existsByUsuario_IdAndAtividade_Id(
                usuario.getId(),
                atividadeId
        )) {
            throw jaRespondida();
        }

        exigirAtividadeAberta(atividade);

        List<Pergunta> perguntas = atividade.getPerguntas();

        if (perguntas.isEmpty()) {
            throw ApiException.conflito(
                    "ATIVIDADE_SEM_QUESTOES",
                    "A atividade não possui questões."
            );
        }

        ProgressoAtividade progresso = progressoRepository
                .findByUsuario_IdAndAtividade_Id(
                        usuario.getId(),
                        atividadeId
                )
                .orElseThrow(() ->
                        ApiException.requisicaoInvalida(
                                "RESPOSTAS_INCOMPLETAS",
                                "É preciso responder todas as questões da atividade antes de enviá-la."
                        )
                );

        if (progresso.getRespostas().size() != perguntas.size()) {
            throw ApiException.requisicaoInvalida(
                    "RESPOSTAS_INCOMPLETAS",
                    "É preciso responder todas as questões da atividade antes de enviá-la."
            );
        }

        Map<Long, Long> escolhaPorPergunta =
                new HashMap<>();

        progresso.getRespostas()
                .forEach(resposta -> {

                    Long perguntaId =
                            resposta.getPergunta().getId();

                    Long alternativaId =
                            resposta.getAlternativa().getId();

                    if (escolhaPorPergunta.put(
                            perguntaId,
                            alternativaId
                    ) != null) {

                        throw ApiException.requisicaoInvalida(
                                "PERGUNTA_DUPLICADA",
                                "A pergunta "
                                        + perguntaId
                                        + " foi respondida mais de uma vez."
                        );
                    }
                });

        int acertos = corrigir(
                perguntas,
                escolhaPorPergunta
        );

        int totalQuestoes = perguntas.size();

        int pontuacao =
                (int) Math.round(
                        (double) acertos
                                * atividade.getPontuacao()
                                / totalQuestoes
                );

        ResultadoAtividade resultado =
                new ResultadoAtividade();

        resultado.setUsuario(usuario);
        resultado.setAtividade(atividade);
        resultado.setAcertos(acertos);
        resultado.setPontuacao(pontuacao);

        try {

            resultado =
                    resultadoAtividadeRepository
                            .saveAndFlush(resultado);

        } catch (DataIntegrityViolationException e) {

            throw jaRespondida();
        }

        progressoRepository.delete(progresso);

        logSistemaService.registrarSucesso(
                usuario,
                AcaoLog.CONCLUIR_ATIVIDADE,
                EntidadeLog.ATIVIDADE,
                atividade.getId(),
                "Aluno \"" + usuario.getNomeUsuario() + "\" concluiu a atividade \"" + atividade.getTitulo() + "\".",
                Map.of(
                        "acertos", acertos,
                        "totalQuestoes", totalQuestoes,
                        "pontuacao", pontuacao
                )
        );

        return QuestionarioMapper.toResultado(
                resultado,
                totalQuestoes
        );
    }


        private void exigirAtividadeAberta (Atividade atividade){
            switch (atividade.getStatus()) {
                case PENDENTE ->
                        throw ApiException.naoEncontrado(
                                "ATIVIDADE_NAO_ENCONTRADA",
                                "Atividade não encontrada."
                        );
                case CONCLUIDO ->
                        throw ApiException.conflito(
                                "ATIVIDADE_ENCERRADA",
                                "Esta atividade foi encerrada plo professor e não serão aceitas mais respostas"
                        );
                case EM_ANDAMENTO -> {
                }
            }
        }

        private int corrigir (List < Pergunta > perguntas, Map<Long, Long> escolhaPorPergunta){

            int acertos = 0;

            for (Pergunta pergunta : perguntas) {
                Long alternativaEscolhida = escolhaPorPergunta.remove(pergunta.getId());
                if (alternativaEscolhida == null) {
                    throw respostasIncompletas();
                }

                Alternativa alternativa = pergunta.getAlternativas().stream()
                        .filter(a -> a.getId().equals(alternativaEscolhida))
                        .findFirst()
                        .orElseThrow(() -> ApiException.requisicaoInvalida(
                                "ALTERNATIVA_INVALIDA",
                                "A alternativa" + alternativaEscolhida + "não pertence à pergunta" + pergunta.getId()));

                if (Boolean.TRUE.equals(alternativa.getCorreta())) {
                    acertos++;
                }
            }
            if (!escolhaPorPergunta.isEmpty()) {
                throw ApiException.requisicaoInvalida(
                        "PERGUNTA_INVALIDA",
                        "As perguntas" + escolhaPorPergunta.keySet() + "não pertecem a esta atividade");
            }
            return acertos;
        }

        private static ApiException respostasIncompletas () {
            return ApiException.requisicaoInvalida(
                    "RESPOSTAS_INCOMPLETAS",
                    "É preciso responder todas as questões da atividade para envia-la");
        }

        private static ApiException jaRespondida () {
            return ApiException.conflito(
                    "ATIVIDADE_JA_RESPONDIDA",
                    "Você já concluiu esta atividade.");
        }
    }