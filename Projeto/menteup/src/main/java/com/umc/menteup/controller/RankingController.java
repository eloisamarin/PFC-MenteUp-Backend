package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Ranking", description = "Classificação geral dos alunos por XP")
public class RankingController {
    private final RankingService rankingService;

    public RankingController(
            RankingService rankingService
    ) {
        this.rankingService = rankingService;
    }

    @Operation(summary = "Ranking geral dos alunos")
    @GetMapping
    public ResponseEntity<List<RankingResponse>> ranking() {
        return ResponseEntity.ok(
                rankingService.buscarRanking()
        );
    }
}
