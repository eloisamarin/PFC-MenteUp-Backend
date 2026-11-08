package com.umc.menteup.controller;

import com.umc.menteup.dto.EsqueciSenhaRequest;
import com.umc.menteup.dto.RedefinirSenhaRequest;
import com.umc.menteup.service.RedefinicaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios/senha")
@CrossOrigin(origins = "*", allowedHeaders = "*")

public class RedefinicaoSenhaController {
    private final RedefinicaoSenhaService redefinicaoSenhaService;

    public RedefinicaoSenhaController(RedefinicaoSenhaService redefinicaoSenhaService) {
        this.redefinicaoSenhaService = redefinicaoSenhaService;
    }

    // POST /usuarios/senha/esqueci: sempre 204, exista o e-mail ou não
    @PostMapping("/esqueci")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        redefinicaoSenhaService.solicitar(request.email());
    }

    // POST /usuarios/senha/redefinir: troca a senha usando o token do link
    @PostMapping("/redefinir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        redefinicaoSenhaService.redefinir(request.token(), request.novaSenha());
    }
}
