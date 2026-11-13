package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.ConquistaAlunoResponse;
import com.umc.menteup.dto.ResumoConquistasResponse;
import com.umc.menteup.service.ConquistaConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

///Rotas de consulta de conquistas. As rotas /alunos/me/** são só para alunos
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Conquistas", description = "Catálogo de conquistas e conquistas do aluno")
public class ConquistaController {

    private final ConquistaConsultaService consultaService;

    public ConquistaController(ConquistaConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    ///Lista as conquistas. Qualquer usuário logado
    @Operation(summary = "Catálogo de conquistas")
    @GetMapping("/conquistas")
    public ResponseEntity<List<ConquistaAlunoResponse>> catalogo() {
        return ResponseEntity.ok(consultaService.listarCatalogo());
    }

    ///Tela de conquistas do aluno logado
    @Operation(summary = "Conquistas do aluno logado")
    @GetMapping("/alunos/me/conquistas")
    public ResponseEntity<ResumoConquistasResponse> minhasConquistas(Authentication authentication) {
        return ResponseEntity.ok(consultaService.buscarDoAlunoLogado(authentication.getName()));
    }

    ///Conquistas novas ainda não vistas (já marca como vistas). Usar na tela de comemoração
    @Operation(summary = "Conquistas novas ainda não vistas (marca como vistas)")
    @PostMapping("/alunos/me/conquistas/novas")
    public ResponseEntity<List<ConquistaAlunoResponse>> consumirNovas(Authentication authentication) {
        return ResponseEntity.ok(consultaService.consumirNovas(authentication.getName()));
    }

    ///Conquistas de um aluno, para professor ou admin
    @Operation(summary = "Conquistas de um aluno")
    @GetMapping("/conquistas/usuario/{id}")
    public ResponseEntity<ResumoConquistasResponse> conquistasDoAluno(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(consultaService.buscarDoAluno(id, authentication.getName()));
    }
}
