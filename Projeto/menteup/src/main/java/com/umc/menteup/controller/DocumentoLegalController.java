package com.umc.menteup.controller;

import com.umc.menteup.dto.DocumentoLegalDtos;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.service.DocumentoLegalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Rotas públicas (sem login): o cadastro e o rodapé do site mostram o texto vigente
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

    private static TipoDocumento tipoValido(String tipo) {
        return TipoDocumento.doTexto(tipo).orElseThrow(() -> ApiException.naoEncontrado(
                "DOCUMENTO_NAO_ENCONTRADO", "Use \"termo\" ou \"politica\"."));
    }
}
