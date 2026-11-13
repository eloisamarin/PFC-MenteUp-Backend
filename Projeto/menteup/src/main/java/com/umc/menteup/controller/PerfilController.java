package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.AlterarAvatarRequest;
import com.umc.menteup.dto.AlterarSenhaRequest;
import com.umc.menteup.dto.ExcluirContaRequest;
import com.umc.menteup.dto.PerfilResponse;
import com.umc.menteup.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios/me")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Meu perfil", description = "Dados, senha, avatar e exclusão da conta de quem está logado")
public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // GET /usuarios/me: perfil
    @Operation(summary = "Mostra o perfil de quem está logado")
    @GetMapping
    public ResponseEntity<PerfilResponse> buscarPerfil(Authentication authentication) {
        return ResponseEntity.ok(perfilService.buscarPerfil(authentication.getName()));
    }

    // PUT /usuarios/me/senha: troca a senha, confirmando a atual
    @Operation(summary = "Troca a senha (pede a senha atual)")
    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(
            @Valid @RequestBody AlterarSenhaRequest request,
            Authentication authentication) {

        perfilService.alterarSenha(authentication.getName(), request.senhaAtual(), request.novaSenha());
    }
    // POST /usuarios/me/exclusao: anonimiza os dados do próprio usuário
    @Operation(summary = "Exclui a própria conta")
    @PostMapping("/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConta(
            @Valid @RequestBody ExcluirContaRequest request,
            Authentication authentication) {

        perfilService.excluirConta(authentication.getName(), request.senha());
    }
    // PUT /usuarios/me/avatar: escolhe a foto de perfil
    @Operation(summary = "Escolhe o avatar")
    @PutMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarAvatar(
            @Valid @RequestBody AlterarAvatarRequest request,
            Authentication authentication) {

        perfilService.alterarAvatar(authentication.getName(), request.avatar());
    }






}
