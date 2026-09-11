package com.umc.menteup.controller;

import com.umc.menteup.dto.*;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.service.QuestionarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class QuestionarioController {
    private final QuestionarioService questionarioService;

    public QuestionarioController(
            QuestionarioService questionarioService
    ){
        this.questionarioService = questionarioService;
    }
    @PostMapping("/{atividadeId}/questionario")
    public ResponseEntity<QuestionarioResponse> cadastrarQuestionario(
            @PathVariable Long atividadeId,
            @Valid @RequestBody QuestionarioRequest request
            ){
        Atividade atividade =
                questionarioService.cadastrarQuesitonario(
                        atividadeId,
                        request
                );
        List<PerguntaResponse> perguntas = atividade.getPerguntas()
                .stream()
                .map(pergunta -> new PerguntaResponse(
                        pergunta.getId(),
                        pergunta.getEnunciado(),
                        pergunta.getAlternativas()
                                .stream()
                                .map(alternativa -> new AlternativaResponse(
                                        alternativa.getId(),
                                        alternativa.getTexto(),
                                        alternativa.getCorreta()
                                ))
                                .toList()
                ))
                .toList();
        QuestionarioResponse response = new QuestionarioResponse(

                new AtividadeResponse(
                        atividade.getId(),
                        atividade.getTitulo(),
                        atividade.getDescricao(),

                        new TurmaResponse(
                                atividade.getTurma().getId(),
                                atividade.getTurma().getNome()
                        ),

                        atividade.getUsuario() == null
                                ? null
                                : new UsuarioResponse(
                                        atividade.getUsuario().getId(),
                                        atividade.getUsuario().getNomeUsuario()
                                )
                ), perguntas
        );
        return  ResponseEntity.ok(response);
    }
}
