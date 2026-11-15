package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.EsqueciSenhaRequest;
import com.umc.menteup.dto.PrimeiroAcessoRequest;
import com.umc.menteup.dto.RedefinirSenhaRequest;
import com.umc.menteup.security.IpDoCliente;
import com.umc.menteup.service.RedefinicaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios/senha")

@Tag(name = "Usuários e login", description = "Cadastro, login com código de 2 etapas e gestão de usuários")
public class RedefinicaoSenhaController {
    private final RedefinicaoSenhaService redefinicaoSenhaService;
    private final IpDoCliente ipDoCliente;

    public RedefinicaoSenhaController(RedefinicaoSenhaService redefinicaoSenhaService, IpDoCliente ipDoCliente) {
        this.redefinicaoSenhaService = redefinicaoSenhaService;
        this.ipDoCliente = ipDoCliente;
    }

    // POST /usuarios/senha/esqueci: sempre 204, exista o e-mail ou não
    @Operation(summary = "Esqueci minha senha: envia o link por e-mail")
    @PostMapping("/esqueci")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request, HttpServletRequest http) {
        redefinicaoSenhaService.solicitar(request.email(), ipDoCliente.de(http));
    }

    // POST /usuarios/senha/redefinir: troca a senha usando o token do link
    @Operation(summary = "Cria a nova senha pelo link do e-mail")
    @PostMapping("/redefinir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        redefinicaoSenhaService.redefinir(request.token(), request.novaSenha());
    }

    // POST /usuarios/senha/primeiro-acesso: quem foi criado pelo administrador cria a senha e aceita o termo
    @Operation(summary = "Primeiro acesso: aceita os termos e cria a senha")
    @PostMapping("/primeiro-acesso")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void primeiroAcesso(@Valid @RequestBody PrimeiroAcessoRequest request) {
        redefinicaoSenhaService.concluirPrimeiroAcesso(request.token(), request.novaSenha(), request.aceitouTermos(),
                request.termoVersao(), request.politicaVersao());
    }
}
