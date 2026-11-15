package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@Tag(name = "Ranking", description = "Classificação dos alunos por XP")
public class RankingController {
    private final RankingService rankingService;

    public RankingController(
            RankingService rankingService
    ) {
        this.rankingService = rankingService;
    }

    // Com turmaId: ranking daquela turma. Sem turmaId: só os alunos que o usuário logado pode ver
    @Operation(summary = "Ranking dos alunos (por turma com turmaId; para o aluno, só o top 10 aparece com nome)")
    @GetMapping
    public ResponseEntity<List<RankingResponse>> ranking(
            @RequestParam(required = false) Long turmaId,
            Authentication authentication) {
        return ResponseEntity.ok(
                rankingService.buscarRanking(turmaId, authentication.getName())
        );
    }
}
