package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.AtividadeAlunoResponse;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.service.AtividadeService;
import com.umc.menteup.service.MatriculaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alunos/me")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Área do aluno", description = "Atividades e turmas do aluno logado")
public class AlunoController {

    private final AtividadeService atividadeService;
    private final MatriculaService matriculaService;

    public AlunoController(AtividadeService atividadeService, MatriculaService matriculaService) {
        this.atividadeService = atividadeService;
        this.matriculaService = matriculaService;
    }

    @Operation(summary = "Atividades do aluno logado")
    @GetMapping("/atividades")
    public ResponseEntity<List<AtividadeAlunoResponse>> minhasAtividades(Authentication authentication) {
        return ResponseEntity.ok(atividadeService.listarParaAluno(authentication.getName()));
    }

    @Operation(summary = "Turmas do aluno logado")
    @GetMapping("/turmas")
    public ResponseEntity<List<TurmaResponse>> minhasTurmas(Authentication authentication) {
        return ResponseEntity.ok(matriculaService.listarTurmasDoAluno(authentication.getName()));
    }


}
