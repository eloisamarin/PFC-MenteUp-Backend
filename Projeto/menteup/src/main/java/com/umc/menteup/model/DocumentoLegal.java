package com.umc.menteup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Uma versão publicada do Termo de Aceite ou da Política de Privacidade
// Depois de gravada, nunca é alterada: um texto novo vira uma versão nova
@Entity
@Table(name = "tb_documento_legal",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "versao"}))
public class    DocumentoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardado como texto simples: assim o banco não trava a lista de tipos (como aconteceu nos logs)
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String versao;

    // TEXT, e não @Lob: no PostgreSQL o @Lob vira objeto grande e o texto fica ilegível no banco
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    // SHA-256 do texto: prova de que a versão aceita é exatamente esta
    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    // false = só correção de texto; true = quem aceitou uma versão anterior precisa aceitar de novo
    @Column(name = "exige_novo_aceite", nullable = false)
    private boolean exigeNovoAceite;

    @Column(length = 500)
    private String resumo;

    @Column(name = "publicado_em", nullable = false)
    private LocalDateTime publicadoEm;

    protected DocumentoLegal() {
    }

    public DocumentoLegal(TipoDocumento tipo, String versao, String conteudo, String hash,
                          LocalDate vigenteDesde, boolean exigeNovoAceite, String resumo) {
        this.tipo = tipo.name();
        this.versao = versao;
        this.conteudo = conteudo;
        this.hash = hash;
        this.vigenteDesde = vigenteDesde;
        this.exigeNovoAceite = exigeNovoAceite;
        this.resumo = resumo;
        this.publicadoEm = LocalDateTime.now();
    }

    // Compara versões como números ("1.10" vem depois de "1.9")
    public static int compararVersoes(String a, String b) {
        String[] partesA = a.split("\\.");
        String[] partesB = b.split("\\.");
        for (int i = 0; i < Math.max(partesA.length, partesB.length); i++) {
            int numeroA = i < partesA.length ? numero(partesA[i]) : 0;
            int numeroB = i < partesB.length ? numero(partesB[i]) : 0;
            if (numeroA != numeroB) {
                return Integer.compare(numeroA, numeroB);
            }
        }
        return 0;
    }

    private static int numero(String parte) {
        try {
            return Integer.parseInt(parte.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Long getId() {
        return id;
    }

    public TipoDocumento getTipo() {
        return TipoDocumento.valueOf(tipo);
    }

    public String getVersao() {
        return versao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public String getHash() {
        return hash;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public boolean isExigeNovoAceite() {
        return exigeNovoAceite;
    }

    public String getResumo() {
        return resumo;
    }

    public LocalDateTime getPublicadoEm() {
        return publicadoEm;
    }
}
