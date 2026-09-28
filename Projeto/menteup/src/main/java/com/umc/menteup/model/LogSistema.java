package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table (name = "log_sistema")
@Getter
@Setter
public class LogSistema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "usuario_id", nullable = true)
    private Usuario usuario;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 40)
    private AcaoLog acao;

    @Enumerated (EnumType.STRING)
    @Column (length = 30)
    private EntidadeLog entidade;

    @Column (name = "entidade_id")
    private Long entidadeId;

    @CreationTimestamp
    @Column (name = "data_hora", updatable = false, nullable = false)
    private LocalDateTime dataHora;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 10)
    private ResultadoLog resultado;

    @Column (length = 500)
    private String descricao;

    @Column (length = 45)
    private String ip;

    @Column (columnDefinition = "TEXT")
    private String dados;
}
