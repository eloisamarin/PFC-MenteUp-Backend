package com.umc.menteup.controller;

import com.umc.menteup.dto.DocumentoLegalDtos;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.service.DocumentoLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Rotas públicas (sem login): o cadastro e o rodapé do site mostram o texto vigente
// Rotas em /documentos/gestao: só o administrador, para publicar e cancelar versões
// {tipo} é "termo" ou "politica"
@RestController
@RequestMapping("/documentos")
@Tag(name = "Termo e Política", description = "Versões do Termo de Aceite e da Política de Privacidade")
public class DocumentoLegalController {

    private final DocumentoLegalService documentoLegalService;

    public DocumentoLegalController(DocumentoLegalService documentoLegalService) {
        this.documentoLegalService = documentoLegalService;
    }

    @Operation(summary = "Versão que está valendo, com o texto completo")
    @GetMapping("/{tipo}/atual")
    public ResponseEntity<DocumentoLegalDtos.Documento> atual(@PathVariable String tipo) {
        return ResponseEntity.ok(DocumentoLegalDtos.Documento.de(documentoLegalService.vigente(tipoValido(tipo))));
    }

    @Operation(summary = "Histórico das versões publicadas (sem o texto)")
    @GetMapping("/{tipo}/versoes")
    public ResponseEntity<List<DocumentoLegalDtos.Versao>> versoes(@PathVariable String tipo) {
        return ResponseEntity.ok(documentoLegalService.historico(tipoValido(tipo)).stream()
                .map(DocumentoLegalDtos.Versao::de)
                .toList());
    }

    @Operation(summary = "Texto de uma versão específica")
    @GetMapping("/{tipo}/versoes/{versao}")
    public ResponseEntity<DocumentoLegalDtos.Documento> versao(@PathVariable String tipo,
                                                               @PathVariable String versao) {
        return ResponseEntity.ok(DocumentoLegalDtos.Documento.de(
                documentoLegalService.versao(tipoValido(tipo), versao)));
    }

    // Gestão pelo administrador

    @Operation(summary = "Administrador: todas as versões, inclusive a agendada, com a situação de cada uma")
    @GetMapping("/gestao/{tipo}/versoes")
    public ResponseEntity<List<DocumentoLegalDtos.VersaoGestao>> versoesParaGestao(@PathVariable String tipo) {
        return ResponseEntity.ok(documentoLegalService.todasAsVersoes(tipoValido(tipo)).stream()
                .map(d -> DocumentoLegalDtos.VersaoGestao.de(d, documentoLegalService.situacao(d)))
                .toList());
    }

    @Operation(summary = "Administrador: texto de qualquer versão, inclusive a agendada")
    @GetMapping("/gestao/{tipo}/versoes/{versao}")
    public ResponseEntity<DocumentoLegalDtos.Documento> versaoParaGestao(@PathVariable String tipo,
                                                                         @PathVariable String versao) {
        return ResponseEntity.ok(DocumentoLegalDtos.Documento.de(
                documentoLegalService.versaoParaGestao(tipoValido(tipo), versao)));
    }

    @Operation(summary = "Administrador: publica uma versão nova (o número é escolhido pelo sistema)")
    @PostMapping("/gestao/{tipo}/versoes")
    public ResponseEntity<DocumentoLegalDtos.VersaoGestao> publicar(
            @PathVariable String tipo,
            @Valid @RequestBody DocumentoLegalDtos.NovaVersao pedido,
            Authentication authentication) {
        var nova = documentoLegalService.publicar(tipoValido(tipo), pedido.conteudo(), pedido.resumo(),
                pedido.vigenteDesde(), pedido.exigeNovoAceite(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentoLegalDtos.VersaoGestao.de(nova, documentoLegalService.situacao(nova)));
    }

    @Operation(summary = "Administrador: cancela uma versão agendada (que ainda não começou a valer)")
    @DeleteMapping("/gestao/{tipo}/versoes/{versao}")
    public ResponseEntity<Void> cancelar(@PathVariable String tipo, @PathVariable String versao,
                                         Authentication authentication) {
        documentoLegalService.cancelar(tipoValido(tipo), versao, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private static TipoDocumento tipoValido(String tipo) {
        return TipoDocumento.doTexto(tipo).orElseThrow(() -> ApiException.naoEncontrado(
                "DOCUMENTO_NAO_ENCONTRADO", "Use \"termo\" ou \"politica\"."));
    }
}
