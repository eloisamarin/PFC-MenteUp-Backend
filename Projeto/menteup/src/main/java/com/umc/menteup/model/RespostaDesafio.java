package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_resposta_desafio",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"pergunta_id", "usuario_id"}
                )
        }
)
@Getter
@Setter
public class RespostaDesafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ON DELETE CASCADE: excluir o desafio apaga as respostas junto com as perguntas
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PerguntaDesafio pergunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternativa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AlternativaDesafio alternativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Boolean correta;

    @Column(nullable = false)
    private Integer pontos;

    @Column(name = "respondida_em", nullable = false)
    private LocalDateTime respondidaEm;
}
