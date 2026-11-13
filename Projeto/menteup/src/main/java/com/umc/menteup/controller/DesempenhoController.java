package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.*;
import com.umc.menteup.service.DesempenhoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Desempenho", description = "Desempenho da turma e do aluno, e comentários do professor")
public class DesempenhoController {

    // O service onde estão as regras: quem pode ver e como os números são calculados
    private final DesempenhoService desempenhoService;
    // Injeção pelo construtor: o Spring cria o service e o entrega aqui
    public DesempenhoController(DesempenhoService desempenhoService) {
        this.desempenhoService = desempenhoService;

    }

    /*
     * GET /turmas/minhas
     * Lista as turmas para o seletor da tela.
     * - Administrador: recebe todas as turmas.
     * - Professor: recebe só as turmas às quais está vinculado.
     * - Aluno: recebe 403 (sem permissão).
     *
     * O Authentication é preenchido pelo Spring a partir do token JWT,
     * e getName() devolve o e-mail do usuário logado. Assim, ninguém
     * consegue se passar por outra pessoa enviando outro e-mail na requisição.
     */
    @Operation(summary = "Turmas para acompanhar o desempenho")
    @GetMapping("/minhas")
    public ResponseEntity<List<TurmaResponse>> listarTurmas(Authentication authentication) {
        return ResponseEntity.ok(desempenhoService.listarTurmas(authentication.getName()));
    }

    /* Mostra o desempenho da turma escolhida: a pontuação, os acertos e
    as atividades concluídas de cada aluno, além da média e da participação da turma.
     */

    @Operation(summary = "Desempenho da turma")
    @GetMapping("/{id}/desempenho")
    public ResponseEntity<DesempenhoTurmaResponse> buscarDesempenho(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desempenhoService.buscarDesempenho(id, authentication.getName()));
    }
    /*
     * PUT /turmas/{turmaId}/resultados/{resultadoId}/comentario
     * O professor da turma (ou o administrador) comenta a atividade feita pelo aluno.
     * Enviar de novo substitui o comentário anterior.
     */
    @Operation(summary = "Professor comenta o resultado de um aluno")
    @PutMapping("/{turmaId}/resultados/{resultadoId}/comentario")
    public ResponseEntity<ComentarioResponse> comentar(
            @PathVariable Long turmaId,
            @PathVariable Long resultadoId,
            @Valid @RequestBody ComentarioRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(desempenhoService.comentarResultado(
                turmaId, resultadoId, request.texto(), authentication.getName()));
    }

    /*
     * GET /turmas/{turmaId}/alunos/{alunoId}/desempenho
     * Desempenho individual: atividades (com perguntas e comentário) e desafios do aluno.
     */
    @Operation(summary = "Desempenho de um aluno na turma")
    @GetMapping("/{turmaId}/alunos/{alunoId}/desempenho")
    public ResponseEntity<DesempenhoIndividualResponse> buscarDesempenhoDoAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId,
            Authentication authentication) {

        return ResponseEntity.ok(
                desempenhoService.buscarDesempenhoDoAluno(turmaId, alunoId, authentication.getName()));
    }





}
