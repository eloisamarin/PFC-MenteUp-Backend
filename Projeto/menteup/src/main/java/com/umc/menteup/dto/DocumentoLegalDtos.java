package com.umc.menteup.dto;

import com.umc.menteup.model.AceiteDocumento;
import com.umc.menteup.model.DocumentoLegal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // Administrador: nova versão do documento (o número é escolhido pelo sistema)
    public record NovaVersao(
            @NotBlank(message = "O texto do documento é obrigatório")
            @Size(max = 100_000, message = "O texto pode ter no máximo 100 mil caracteres")
            String conteudo,

            @NotBlank(message = "O resumo da mudança é obrigatório")
            @Size(max = 500, message = "O resumo pode ter no máximo 500 caracteres")
            String resumo,

            @NotNull(message = "A data de vigência é obrigatória")
            LocalDate vigenteDesde,

            // Sem valor, exige novo aceite
            Boolean exigeNovoAceite
    ) {
    }

    // Administrador: linha da lista de versões, inclusive as agendadas
    // situacao: AGENDADA (ainda não começou a valer), VIGENTE (a que vale hoje) ou ANTERIOR
    public record VersaoGestao(
            String tipo,
            String versao,
            LocalDate vigenteDesde,
            boolean exigeNovoAceite,
            String resumo,
            LocalDateTime publicadoEm,
            String situacao
    ) {
        public static VersaoGestao de(DocumentoLegal d, String situacao) {
            return new VersaoGestao(d.getTipo().name(), d.getVersao(), d.getVigenteDesde(), d.isExigeNovoAceite(),
                    d.getResumo(), d.getPublicadoEm(), situacao);
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
