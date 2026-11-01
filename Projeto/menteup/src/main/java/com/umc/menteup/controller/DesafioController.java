package com.umc.menteup.controller;

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
public class DesafioController {

    private final DesafioService desafioService;

    public DesafioController(DesafioService desafioService) {
        this.desafioService = desafioService;
    }

    @GetMapping
    public ResponseEntity<List<DesafioResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(desafioService.listar(authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR cria desafio
    @PostMapping
    public ResponseEntity<DesafioResponse> criar(
            @Valid @RequestBody CriarDesafioRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(desafioService.criar(request, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR exclui desafio
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, Authentication authentication) {
        desafioService.excluir(id, authentication.getName());
    }

    // As perguntas só aparecem depois desta chamada do PROFESSOR ou ADMINISTRADOR
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<EstadoDesafioResponse> iniciar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.iniciar(id, authentication.getName()));
    }

    // Abre a próxima pergunta, depois da última e encerra o desafio
    @PostMapping("/{id}/avancar")
    public ResponseEntity<EstadoDesafioResponse> avancar(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.avancar(id, authentication.getName()));
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoDesafioResponse> estado(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.buscarEstado(id, authentication.getName()));
    }

    @PostMapping("/{id}/respostas")
    public ResponseEntity<RespostaProgressoResponse> responder(
            @PathVariable Long id,
            @Valid @RequestBody ResponderDesafioRequest request,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(desafioService.responder(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/ranking")
    public ResponseEntity<List<RankingDesafioResponse>> ranking(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desafioService.ranking(id, authentication.getName()));
    }
}
