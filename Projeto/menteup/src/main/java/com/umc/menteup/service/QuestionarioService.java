package com.umc.menteup.service;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.QuestionarioRequest;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.repository.AtividadeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class QuestionarioService {
    private final AtividadeRepository atividadeRepository;

    public QuestionarioService(AtividadeRepository atividadeRepository){
        this.atividadeRepository = atividadeRepository;
    }

    @Transactional
    public Atividade cadastrarQuesitonario(
            Long atividadeId,
            QuestionarioRequest request
    ){
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() ->new RuntimeException("Atividade não encontrada")
                );
        for (PerguntaRequest perguntaRequest : request.perguntas()){
            Pergunta pergunta = new Pergunta();

            pergunta.setEnunciado(perguntaRequest.enunciado());
            pergunta.setAtividade(atividade);

            for(AlternativaRequest alternativaRequest : perguntaRequest.alternativas()){
                Alternativa alternativa = new Alternativa();

                alternativa.setTexto(alternativaRequest.texto());
                alternativa.setCorreta(alternativaRequest.correta());
                alternativa.setPergunta(pergunta);

                pergunta.getAlternativas().add(alternativa);
            }
            atividade.getPerguntas().add(pergunta);
        }
        return  atividadeRepository.save(atividade);
    }
}
