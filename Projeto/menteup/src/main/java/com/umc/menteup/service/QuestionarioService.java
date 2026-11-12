package com.umc.menteup.service;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.QuestionarioRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.template.TemplateAtividade;
import com.umc.menteup.template.TemplateResolver;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuestionarioService {
    private final AtividadeRepository atividadeRepository;
    private final LogSistemaService logSistemaService;
    private final TemplateResolver templateResolver;
    private final ProgressoAtividadeRepository progressoRepository;
    private final ResultadoAtividadeRepository resultadoRepository;
    private final AtividadeService atividadeService;

     public QuestionarioService(
            AtividadeRepository atividadeRepository,
            LogSistemaService logSistemaService,
            TemplateResolver templateResolver,
            ProgressoAtividadeRepository progressoRepository,
            ResultadoAtividadeRepository resultadoRepository,
            AtividadeService atividadeService
    ){
        this.atividadeRepository = atividadeRepository;
        this.logSistemaService = logSistemaService;
        this.templateResolver = templateResolver;
        this.progressoRepository = progressoRepository;
        this.resultadoRepository = resultadoRepository;
        this.atividadeService = atividadeService;
    }

    @Transactional
    public Atividade cadastrarQuesitonario(
            Long atividadeId,
            QuestionarioRequest request,
            String username
    ){
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ATIVIDADE_NAO_ENCONTRADA",
                        "Atividade não encontrada."
                ));

        // Professor só cadastra questões nas atividades das turmas dele
        atividadeService.exigirPodeAlterar(atividade.getTurma(), username);

        TipoAtividade tipo = definirTipo(atividade, request.tipo());
        TemplateAtividade template = templateResolver.resolver(tipo);
        template.validarQuestionario(atividade, request.perguntas());
        atividade.setTipo(tipo);

        List<Pergunta> criadasNestaChamada = adicionarPerguntas(atividade, request.perguntas());

        Atividade salva = atividadeRepository.save(atividade);

        registrarQuestoesCriadas(username, salva, criadasNestaChamada);

        return salva;
    }

    ///Substitui todas as questões, desde que nenhum aluno tenha iniciado a atividade
    @Transactional
    public Atividade substituirQuestionario(
            Long atividadeId,
            QuestionarioRequest request,
            String username
    ){
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ATIVIDADE_NAO_ENCONTRADA",
                        "Atividade não encontrada."
                ));

        atividadeService.exigirPodeAlterar(atividade.getTurma(), username);

        if (progressoRepository.existsByAtividade_Id(atividadeId)
                || resultadoRepository.existsByAtividade_Id(atividadeId)) {
            throw ApiException.conflito(
                    "ATIVIDADE_COM_RESPOSTAS",
                    "As questões não podem ser alteradas porque alunos já começaram ou concluíram esta atividade."
            );
        }

        int removidas = atividade.getPerguntas().size();

        atividade.getPerguntas().clear();

        ///o tipo pode ser alterado livremente
        TipoAtividade tipo = request.tipo() == null ? atividade.getTipo() : request.tipo();
        TemplateAtividade template = templateResolver.resolver(tipo);
        template.validarQuestionario(atividade, request.perguntas());
        atividade.setTipo(tipo);

        List<Pergunta> criadas = adicionarPerguntas(atividade, request.perguntas());

        Atividade salva = atividadeRepository.save(atividade);

        logSistemaService.registrarPorUsername(
                username,
                AcaoLog.EDITAR_ATIVIDADE,
                EntidadeLog.ATIVIDADE,
                salva.getId(),
                ResultadoLog.SUCESSO,
                "Questões da atividade \"" + salva.getTitulo() + "\" substituídas.",
                Map.of(
                        "tipo", tipo.name(),
                        "questoesRemovidas", removidas,
                        "questoesCriadas", criadas.size()
                )
        );
        registrarQuestoesCriadas(username, salva, criadas);

        return salva;
    }

    ///Cria as perguntas e alternativas do request e vincula à atividade
    private List<Pergunta> adicionarPerguntas(Atividade atividade, List<PerguntaRequest> perguntas) {
        List<Pergunta> criadasNestaChamada = new ArrayList<>();

        for (PerguntaRequest perguntaRequest : perguntas){
            Pergunta pergunta = new Pergunta();

            pergunta.setEnunciado(perguntaRequest.enunciado());
            pergunta.setAtividade(atividade);

            for(AlternativaRequest alternativaRequest : perguntaRequest.alternativas()){
                Alternativa alternativa = new Alternativa();

                alternativa.setTexto(alternativaRequest.texto());
                alternativa.setCorreta(alternativaRequest.correta());
                alternativa.setOrdem(alternativaRequest.ordem());
                alternativa.setPergunta(pergunta);

                pergunta.getAlternativas().add(alternativa);
            }
            atividade.getPerguntas().add(pergunta);
            criadasNestaChamada.add(pergunta);
        }
        return criadasNestaChamada;
    }

    private void registrarQuestoesCriadas(String username, Atividade salva, List<Pergunta> criadas) {
        for (Pergunta pergunta : criadas) {
            logSistemaService.registrarPorUsername(
                    username,
                    AcaoLog.CRIAR_QUESTAO,
                    EntidadeLog.QUESTAO,
                    pergunta.getId(),
                    ResultadoLog.SUCESSO,
                    "Questão criada na atividade \"" + salva.getTitulo() + "\".",
                    Map.of(
                            "atividadeId", salva.getId(),
                            "enunciado", pergunta.getEnunciado()
                    )
            );
        }
    }

    private TipoAtividade definirTipo(Atividade atividade, TipoAtividade tipoSolicitado) {
        TipoAtividade tipoAtual = atividade.getTipo();

        if (tipoSolicitado == null || tipoSolicitado == tipoAtual) {
            return tipoAtual;
        }

        if (!atividade.getPerguntas().isEmpty()) {
            throw ApiException.conflito(
                    "TIPO_ATIVIDADE_IMUTAVEL",
                    "Esta atividade já possui questões do tipo " + tipoAtual
                            + ". Remova as questões antes de trocar o tipo."
            );
        }
        return tipoSolicitado;
    }
}
