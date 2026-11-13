package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import java.util.List;
import java.util.Optional;



import com.umc.menteup.model.Codigo2FARequest;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioLogin;
import com.umc.menteup.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.umc.menteup.dto.FcmTokenRequest;
import com.umc.menteup.dto.AtualizarUsuarioRequest;
import com.umc.menteup.dto.NovoUsuarioRequest;
import com.umc.menteup.dto.ReenviarCodigo2FARequest;
import com.umc.menteup.dto.UsuarioListagemResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Usuários e login", description = "Cadastro, login com código de 2 etapas e gestão de usuários")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Administrador: com e-mail. Professor e aluno: só id, nome e tipo (LGPD)
    @Operation(summary = "Lista os usuários (e-mail só para administrador; professor vê o dos alunos)")
    @GetMapping("/all")
    public ResponseEntity<List<UsuarioListagemResponse>> getAll(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.listar(authentication.getName()));
    }

    // Só o próprio usuário ou o administrador
    @Operation(summary = "Busca um usuário (só o próprio ou o administrador)")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioListagemResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id, authentication.getName()));
    }

    @Operation(summary = "Cadastro público de aluno ou professor")
    @PostMapping("/cadastrar")
    public ResponseEntity<Usuario> post(@Valid @RequestBody Usuario usuario) {
        return usuarioService.cadastrarUsuario(usuario)
                .map(resposta -> ResponseEntity.status(HttpStatus.CREATED).body(resposta))
                .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    // Só o administrador: cria aluno, professor ou outro administrador.
    // A pessoa recebe um e-mail para criar a senha e aceitar o termo.
    @Operation(summary = "Administrador cria usuário e envia convite por e-mail")
    @PostMapping
    public ResponseEntity<Usuario> criarPorAdministrador(
            @Valid @RequestBody NovoUsuarioRequest pedido,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criarUsuarioPorAdministrador(pedido, authentication.getName()));
    }

    @Operation(summary = "Atualiza nome, e-mail e tipo do usuário")
    @PutMapping("/atualizar")
    public ResponseEntity<Usuario> put(
            @Valid @RequestBody AtualizarUsuarioRequest dados,
            Authentication authentication) {

        return usuarioService.atualizarUsuario(dados, authentication.getName())
                .map(resposta -> ResponseEntity.status(HttpStatus.OK).body(resposta))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Passo 1: valida usuário e senha e envia o código 2FA por e-mail
    @Operation(summary = "Login, passo 1: confere e-mail e senha e envia o código")
    @PostMapping("/logar")
    public ResponseEntity<UsuarioLogin> autenticar(@Valid @RequestBody Optional<UsuarioLogin> usuarioLogin) {
        return usuarioService.autenticarUsuario(usuarioLogin)
                .map(resposta -> ResponseEntity.status(HttpStatus.OK).body(resposta))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // Passo 2: valida o código 2FA e devolve o token JWT
    @Operation(summary = "Login, passo 2: confere o código e devolve o token")
    @PostMapping("/validar-2fa")
    public ResponseEntity<UsuarioLogin> validarCodigo2FA(@Valid @RequestBody Codigo2FARequest request) {
        return usuarioService.validarCodigo2FA(request.getUsuario(), request.getCodigo())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // "Reenviar código": manda um novo código 2FA para quem acabou de entrar com a senha
    @Operation(summary = "Envia um novo código de login")
    @PostMapping("/reenviar-2fa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarCodigo2FA(@Valid @RequestBody ReenviarCodigo2FARequest request) {
        usuarioService.reenviarCodigo2FA(request.usuario());
    }

    @Operation(summary = "Registra o token do celular para notificações")
    @PostMapping("/fcm-token")
    public ResponseEntity<?> registrarToken(
            @Valid @RequestBody FcmTokenRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        boolean salvo = usuarioService.salvarFcmToken(email, request.getFcmToken());

        if (!salvo) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado.");
        }

        return ResponseEntity.ok("Token Firebase registrado com sucesso.");
    }
}