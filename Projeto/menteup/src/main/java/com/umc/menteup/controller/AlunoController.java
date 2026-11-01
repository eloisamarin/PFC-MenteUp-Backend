package com.umc.menteup.controller;

import com.umc.menteup.dto.AtividadeAlunoResponse;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.service.AtividadeService;
import com.umc.menteup.service.MatriculaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alunos/me")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AlunoController {

    private final AtividadeService atividadeService;
    private final MatriculaService matriculaService;

    public AlunoController(AtividadeService atividadeService, MatriculaService matriculaService) {
        this.atividadeService = atividadeService;
        this.matriculaService = matriculaService;
    }

    @GetMapping("/atividades")
    public ResponseEntity<List<AtividadeAlunoResponse>> minhasAtividades(Authentication authentication) {
        return ResponseEntity.ok(atividadeService.listarParaAluno(authentication.getName()));
    }

    @GetMapping("/turmas")
    public ResponseEntity<List<TurmaResponse>> minhasTurmas(Authentication authentication) {
        return ResponseEntity.ok(matriculaService.listarTurmasDoAluno(authentication.getName()));
    }

    @PostMapping("/turmas/{turmaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void entrarNaTurma(@PathVariable Long turmaId, Authentication authentication) {
        matriculaService.matricular(authentication.getName(), turmaId);
    }

    @DeleteMapping("/turmas/{turmaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sairDaTurma(@PathVariable Long turmaId, Authentication authentication) {
        matriculaService.desmatricular(authentication.getName(), turmaId);
    }
}
