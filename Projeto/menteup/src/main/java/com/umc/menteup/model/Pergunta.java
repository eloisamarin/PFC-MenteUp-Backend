package com.umc.menteup.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "perguntas")
@Getter
@Setter
public class Pergunta {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String enunciado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @OneToMany(
            mappedBy =  "pergunta",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Alternativa> alternativas = new ArrayList<>();
}
