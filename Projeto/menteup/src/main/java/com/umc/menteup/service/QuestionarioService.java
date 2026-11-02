package com.umc.menteup.service;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.QuestionarioRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
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

     public QuestionarioService(
            AtividadeRepository atividadeRepository,
            LogSistemaService logSistemaService,
            TemplateResolver templateResolver
    ){
        this.atividadeRepository = atividadeRepository;
        this.logSistemaService = logSistemaService;
        this.templateResolver = templateResolver;
    }

    @Transactional
    public Atividade cadastrarQuesitonario(
            Long atividadeId,
            QuestionarioRequest request,
            String username
    ){
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() ->new RuntimeException("Atividade não encontrada")
                );

        TipoAtividade tipo = definirTipo(atividade, request.tipo());
        TemplateAtividade template = templateResolver.resolver(tipo);
        template.validarQuestionario(atividade, request.perguntas());
        atividade.setTipo(tipo);

        List<Pergunta> criadasNestaChamada = new ArrayList<>();

        for (PerguntaRequest perguntaRequest : request.perguntas()){
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

        Atividade salva = atividadeRepository.save(atividade);

        for (Pergunta pergunta : criadasNestaChamada) {
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

        return salva;
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
