package com.umc.menteup.dto;

import com.umc.menteup.model.AceiteDocumento;
import com.umc.menteup.model.DocumentoLegal;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Respostas e pedidos das rotas do Termo de Aceite e da Política de Privacidade
public final class DocumentoLegalDtos {

    private DocumentoLegalDtos() {
    }

    // Texto completo de uma versão (o front mostra o conteúdo como Markdown)
    public record Documento(
            String tipo,
            String versao,
            LocalDate vigenteDesde,
            String resumo,
            String conteudo
    ) {
        public static Documento de(DocumentoLegal d) {
            return new Documento(d.getTipo().name(), d.getVersao(), d.getVigenteDesde(), d.getResumo(),
                    d.getConteudo());
        }
    }

    // Linha do histórico de versões, sem o texto
    public record Versao(
            String tipo,
            String versao,
            LocalDate vigenteDesde,
            boolean exigeNovoAceite,
            String resumo
    ) {
        public static Versao de(DocumentoLegal d) {
            return new Versao(d.getTipo().name(), d.getVersao(), d.getVigenteDesde(), d.isExigeNovoAceite(),
                    d.getResumo());
        }
    }

    public record Aceite(String tipo, String versao, LocalDateTime aceitoEm) {
        public static Aceite de(AceiteDocumento a) {
            return new Aceite(a.getDocumento().getTipo().name(), a.getDocumento().getVersao(), a.getAceitoEm());
        }
    }

    // GET /usuarios/me/aceites: o que falta aceitar e o histórico do que já foi aceito
    public record SituacaoAceites(List<Versao> pendentes, List<Aceite> aceites) {
    }

    // POST /usuarios/me/aceites: as versões que a pessoa leu e aceitou
    public record AceitarDocumentos(
            @Size(max = 20, message = "Versão inválida") String termoVersao,
            @Size(max = 20, message = "Versão inválida") String politicaVersao
    ) {
    }
}
