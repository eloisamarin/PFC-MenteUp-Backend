package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;



@Entity
@Table(name = "tb_token_redefinicao_senha")
@NoArgsConstructor
@Getter
@Setter
public class TokenRedefinicaoSenha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    private boolean utilizado = false;

    // Usado pelo service para criar um link novo
    public TokenRedefinicaoSenha(Usuario usuario, String tokenHash, LocalDateTime expiracao) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiracao = expiracao;
    }

}
