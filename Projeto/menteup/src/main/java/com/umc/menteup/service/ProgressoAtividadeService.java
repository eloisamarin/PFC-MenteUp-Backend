package com.umc.menteup.service;

import com.umc.menteup.dto.ProgressoAtividadeResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.template.TemplateAtividade;
import com.umc.menteup.template.TemplateResolver;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProgressoAtividadeService {
    private final UsuarioRepository usuarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final ProgressoAtividadeRepository progressoRepository;
    private final ResultadoAtividadeRepository resultadoRepository;
    private final LogSistemaService logSistemaService;
    private final MatriculaService matriculaService;
    private final TemplateResolver templateResolver;

    public ProgressoAtividadeService(
            UsuarioRepository usuarioRepository,
            AtividadeRepository atividadeRepository,
            ProgressoAtividadeRepository progressoRepository,
            ResultadoAtividadeRepository resultadoRepository,
            LogSistemaService logSistemaService,
            MatriculaService matriculaService,
            TemplateResolver templateResolver
    ) {
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.progressoRepository = progressoRepository;
        this.resultadoRepository = resultadoRepository;
        this.logSistemaService = logSistemaService;
        this.matriculaService = matriculaService;
        this.templateResolver = templateResolver;
    }

    @Transactional
    public ProgressoAtividadeResponse salvarResposta(
            Long atividadeId,
            String username,
            SalvarRespostaRequest request
    ) {
        Usuario usuario = usuarioRepository.findByUsuario(username).orElseThrow(() ->
                ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO",
                        "Usário não encontrado."
                ));
        if (!TipoUsuario.ehAluno(usuario)) {
            throw ApiException.proibido(
                    "SOMENTE_ALUNO",
                    "Somente alunos podem responder atividades."
            );
        }
        Atividade atividade = atividadeRepository.findById(atividadeId).orElseThrow(() ->
                ApiException.naoEncontrado(
                        "ATIVIDADE_NAO_ENCONTRADA",
                        "Atividade não encontrada."
                )
        );

        matriculaService.exigirMatricula(atividade, usuario);

        if (resultadoRepository.existsByUsuario_IdAndAtividade_Id(
                usuario.getId(),
                atividadeId
        )) {
            throw ApiException.conflito(
                    "ATIVIDADE_JA_RESPONDIDA",
                    "Esta atividade não está disponível para respostas."
            );
        }

        exigirAtividadeAberta(atividade);

        Pergunta pergunta = atividade.getPerguntas()
                .stream()
                .filter(p -> p.getId().equals(request.perguntaId()))
                .findFirst()
                .orElseThrow(() ->
                        ApiException.requisicaoInvalida(
                                "PERGUNTA_INVALIDA",
                                "A pergunta não pertence a esta atividade."
                        )
                );
        TemplateAtividade template = templateResolver.resolver(atividade);
        template.validarResposta(pergunta, request);

        ProgressoAtividade progresso = progressoRepository.findByUsuario_IdAndAtividade_Id(
                        usuario.getId(),
                        atividadeId
                )
                .orElse(null);

        boolean primeiraResposta = progresso == null;

        if (progresso == null) {
            progresso = new ProgressoAtividade();
            progresso.setUsuario(usuario);
            progresso.setAtividade(atividade);
        }

        RespostaAtividade respostaExistente = progresso.getRespostas()
                .stream()
                .filter(r -> r.getPergunta()
                        .getId()
                        .equals(pergunta.getId()))
                .findFirst()
                .orElse(null);
        if (respostaExistente == null) {
            RespostaAtividade resposta = new RespostaAtividade();
            resposta.setProgresso(progresso);
            resposta.setPergunta(pergunta);
            template.preencherResposta(resposta, pergunta, request);
            progresso.getRespostas().add(resposta);
        } else {
            template.preencherResposta(respostaExistente, pergunta, request);
        }
        progressoRepository.save(progresso);

        if (primeiraResposta) {
            logSistemaService.registrarSucesso(
                    usuario,
                    AcaoLog.INICIAR_ATIVIDADE,
                    EntidadeLog.ATIVIDADE,
                    atividade.getId(),
                    "Aluno \"" + usuario.getNomeUsuario() + "\" iniciou a atividade \"" + atividade.getTitulo() + "\".",
                    Map.of("atividadeId", atividade.getId())
            );
        }

        logSistemaService.registrarSucesso(
                usuario,
                AcaoLog.RESPONDER_QUESTAO,
                EntidadeLog.QUESTAO,
                pergunta.getId(),
                "Aluno respondeu a questão " + pergunta.getId() + " da atividade \"" + atividade.getTitulo() + "\".",
                dadosDaResposta(atividade, pergunta, request)
        );

        return montarResponse(progresso, atividade);
    }

    private void exigirAtividadeAberta(Atividade atividade) {
        switch (atividade.getStatus()) {
            case PENDENTE -> throw ApiException.naoEncontrado(
                    "ATIVIDADE_NAO_ENCONTRADA",
                    "Atividade não encontrada."
            );
            case CONCLUIDO -> throw ApiException.conflito(
                    "ATIVIDADE_ENCERRADA",
                    "Esta atividade foi encerrada pelo professor e não aceita mais respostas."
            );
            case EM_ANDAMENTO -> {
            }
        }
    }

    private ProgressoAtividadeResponse montarResponse(
            ProgressoAtividade progreso,
            Atividade atividade
    ) {
        int total = atividade.getPerguntas().size();
        int respondidas = progreso.getRespostas().size();

        int percentual = total == 0
                ? 0
                : (int) Math.round(
                respondidas * 100.0 / total
        );
        return new ProgressoAtividadeResponse(
                percentual,
                respondidas,
                total,
                progreso.getRespostas()
                        .stream()
                        .map(QuestionarioMapper::toRespostaProgresso)
                        .toList()
        );
    }

    private static Map<String, Object> dadosDaResposta(
            Atividade atividade,
            Pergunta pergunta,
            SalvarRespostaRequest request
    ) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atividadeId", atividade.getId());
        dados.put("perguntaId", pergunta.getId());
        if (request.alternativaId() != null) {
            dados.put("alternativaId", request.alternativaId());
        }
        if (request.alternativasIds() != null) {
            dados.put("alternativasIds", request.alternativasIds());
        }
        return dados;
    }
}


