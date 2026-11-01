package com.umc.menteup.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "resultado_atividade",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"usuario_id", "atividade_id"}
                )
        }
)

@Getter
@Setter
public class ResultadoAtividade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @Column(nullable = false)
    private Integer acertos;

    @Column(nullable = false)
    private Integer pontuacao;

    @Column(nullable = false)
    private LocalDateTime concluida_em = LocalDateTime.now();
}
