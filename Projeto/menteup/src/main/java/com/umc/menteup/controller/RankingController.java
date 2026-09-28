package com.umc.menteup.controller;

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
public class RankingController {
    private final RankingService rankingService;

    public RankingController(
            RankingService rankingService
    ) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public ResponseEntity<List<RankingResponse>> ranking() {
        return ResponseEntity.ok(
                rankingService.buscarRanking()
        );
    }
}
