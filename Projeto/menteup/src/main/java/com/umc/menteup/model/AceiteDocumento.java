package com.umc.menteup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// Comprovante de aceite: quem aceitou, qual versão exata e quando
// Continua guardado mesmo depois que a conta é excluída (anonimizada), como prova do aceite
@Entity
@Table(name = "tb_aceite_documento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "documento_id"}))
public class AceiteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoLegal documento;

    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm;

    protected AceiteDocumento() {
    }

    public AceiteDocumento(Usuario usuario, DocumentoLegal documento, LocalDateTime aceitoEm) {
        this.usuario = usuario;
        this.documento = documento;
        this.aceitoEm = aceitoEm;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public DocumentoLegal getDocumento() {
        return documento;
    }

    public LocalDateTime getAceitoEm() {
        return aceitoEm;
    }
}
