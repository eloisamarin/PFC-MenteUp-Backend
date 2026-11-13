package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.*;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.service.QuestionarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Atividades", description = "Criação, questionário, progresso, respostas e exclusão de atividades")
public class QuestionarioController {

    private final QuestionarioService questionarioService;

    public QuestionarioController(
            QuestionarioService questionarioService
    ) {
        this.questionarioService = questionarioService;
    }

    @Operation(summary = "Cadastra as perguntas da atividade")
    @PostMapping("/{atividadeId}/questionario")
    public ResponseEntity<QuestionarioResponse> cadastrarQuestionario(
            @PathVariable Long atividadeId,
            @Valid @RequestBody QuestionarioRequest request,
            Authentication authentication
    ) {
        Atividade atividade =
                questionarioService.cadastrarQuesitonario(
                        atividadeId,
                        request,
                        authentication.getName()
                );

        QuestionarioResponse response =
                QuestionarioMapper.toQuestionarioProfessor(atividade);

        return ResponseEntity.ok(response);
    }

    /// Substitui as questões da atividade
    @Operation(summary = "Substitui as perguntas (antes de algum aluno começar)")
    @PutMapping("/{atividadeId}/questionario")
    public ResponseEntity<QuestionarioResponse> substituirQuestionario(
            @PathVariable Long atividadeId,
            @Valid @RequestBody QuestionarioRequest request,
            Authentication authentication
    ) {
        Atividade atividade =
                questionarioService.substituirQuestionario(
                        atividadeId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(QuestionarioMapper.toQuestionarioProfessor(atividade));
    }
}
