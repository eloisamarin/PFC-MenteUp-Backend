package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.AlterarAvatarRequest;
import com.umc.menteup.dto.AlterarSenhaRequest;
import com.umc.menteup.dto.DocumentoLegalDtos;
import com.umc.menteup.dto.ExcluirContaRequest;
import com.umc.menteup.dto.MeusDadosResponse;
import com.umc.menteup.dto.PerfilResponse;
import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.service.DocumentoLegalService;
import com.umc.menteup.service.ExportacaoDadosService;
import com.umc.menteup.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios/me")
@Tag(name = "Meu perfil", description = "Dados, senha, avatar e exclusão da conta de quem está logado")
public class PerfilController {
    private final PerfilService perfilService;
    private final DocumentoLegalService documentoLegalService;
    private final ExportacaoDadosService exportacaoDadosService;

    public PerfilController(PerfilService perfilService, DocumentoLegalService documentoLegalService,
                            ExportacaoDadosService exportacaoDadosService) {
        this.perfilService = perfilService;
        this.documentoLegalService = documentoLegalService;
        this.exportacaoDadosService = exportacaoDadosService;
    }

    // GET /usuarios/me/aceites: o que falta aceitar (versões novas) e o histórico dos aceites
    @Operation(summary = "Mostra os aceites do Termo e da Política e o que está pendente")
    @GetMapping("/aceites")
    public ResponseEntity<DocumentoLegalDtos.SituacaoAceites> aceites(Authentication authentication) {
        Long id = perfilService.buscarPerfil(authentication.getName()).id();
        return ResponseEntity.ok(situacao(id, documentoLegalService.pendentes(id)));
    }

    // POST /usuarios/me/aceites: registra o aceite das versões vigentes que a pessoa leu
    @Operation(summary = "Aceita a versão atual do Termo e/ou da Política")
    @PostMapping("/aceites")
    public ResponseEntity<DocumentoLegalDtos.SituacaoAceites> aceitar(
            @Valid @RequestBody DocumentoLegalDtos.AceitarDocumentos request,
            Authentication authentication) {

        List<DocumentoLegal> pendentes = documentoLegalService.aceitar(
                authentication.getName(), request.termoVersao(), request.politicaVersao());
        Long id = perfilService.buscarPerfil(authentication.getName()).id();
        return ResponseEntity.ok(situacao(id, pendentes));
    }

    // GET /usuarios/me/exportar: arquivo com todos os dados da pessoa (LGPD: acesso e portabilidade)
    @Operation(summary = "Baixa todos os dados da conta em um arquivo JSON")
    @GetMapping("/exportar")
    public ResponseEntity<MeusDadosResponse> exportar(Authentication authentication) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meus-dados-menteup.json\"")
                .body(exportacaoDadosService.exportar(authentication.getName()));
    }

    private DocumentoLegalDtos.SituacaoAceites situacao(Long usuarioId, List<DocumentoLegal> pendentes) {
        return new DocumentoLegalDtos.SituacaoAceites(
                pendentes.stream().map(DocumentoLegalDtos.Versao::de).toList(),
                documentoLegalService.aceitesDo(usuarioId).stream().map(DocumentoLegalDtos.Aceite::de).toList());
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
