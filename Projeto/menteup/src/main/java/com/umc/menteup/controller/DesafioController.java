package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.*;
import com.umc.menteup.service.DesafioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/desafios")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Desafios ao vivo", description = "Criação, condução, respostas e ranking dos desafios")
public class DesafioController {

    private final DesafioService desafioService;

    public DesafioController(DesafioService desafioService) {
        this.desafioService = desafioService;
    }

    @Operation(summary = "Lista os desafios")
    @GetMapping
    public ResponseEntity<List<DesafioResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(desafioService.listar(authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR cria desafio
    @Operation(summary = "Cria um desafio")
    @PostMapping
    public ResponseEntity<DesafioResponse> criar(
            @Valid @RequestBody CriarDesafioRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(desafioService.criar(request, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR consulta o desafio, com gabarito, para editar
    @Operation(summary = "Busca o desafio para edição")
    @GetMapping("/{id}/edicao")
    public ResponseEntity<DesafioEdicaoResponse> buscarParaEdicao(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.buscarParaEdicao(id, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR edita, e apenas antes de iniciar o desafio
    @Operation(summary = "Edita o desafio (só enquanto aguarda)")
    @PutMapping("/{id}")
    public ResponseEntity<DesafioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CriarDesafioRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.atualizar(id, request, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR exclui desafio
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui o desafio")
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, Authentication authentication) {
        desafioService.excluir(id, authentication.getName());
    }

    // As perguntas só aparecem depois desta chamada do PROFESSOR ou ADMINISTRADOR
    @Operation(summary = "Professor inicia o desafio")
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<EstadoDesafioResponse> iniciar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.iniciar(id, authentication.getName()));
    }

    // Abre a próxima pergunta, depois da última e encerra o desafio
    @Operation(summary = "Professor passa para a próxima pergunta")
    @PostMapping("/{id}/avancar")
    public ResponseEntity<EstadoDesafioResponse> avancar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.avancar(id, authentication.getName()));
    }

    @Operation(summary = "Estado atual do desafio")
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoDesafioResponse> estado(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.buscarEstado(id, authentication.getName()));
    }

    @Operation(summary = "Aluno responde a pergunta atual")
    @PostMapping("/{id}/respostas")
    public ResponseEntity<RespostaProgressoResponse> responder(
            @PathVariable Long id,
            @Valid @RequestBody ResponderDesafioRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(desafioService.responder(id, request, authentication.getName()));
    }

    @Operation(summary = "Ranking do desafio")
    @GetMapping("/{id}/ranking")
    public ResponseEntity<List<RankingDesafioResponse>> ranking(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.ranking(id, authentication.getName()));
    }
}
