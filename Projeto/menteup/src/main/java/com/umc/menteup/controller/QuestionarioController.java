package com.umc.menteup.controller;

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
public class QuestionarioController {

    private final QuestionarioService questionarioService;

    public QuestionarioController(
            QuestionarioService questionarioService
    ) {
        this.questionarioService = questionarioService;
    }

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
                QuestionarioMapper.toQuestionario(atividade, null, null);

        return ResponseEntity.ok(response);
    }
}
