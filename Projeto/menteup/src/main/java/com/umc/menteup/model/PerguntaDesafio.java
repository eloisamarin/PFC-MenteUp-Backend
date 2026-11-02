package com.umc.menteup.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_pergunta_desafio")
@Getter
@Setter
public class PerguntaDesafio {

    public static final int TEMPO_MINIMO_SEGUNDOS = 5;
    public static final int TEMPO_MAXIMO_SEGUNDOS = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String enunciado;

    @NotNull
    @Min(TEMPO_MINIMO_SEGUNDOS)
    @Max(TEMPO_MAXIMO_SEGUNDOS)
    @Column(name = "tempo_segundos", nullable = false)
    private Integer tempoSegundos;

    @NotNull
    @Column(nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desafio_id", nullable = false)
    private Desafio desafio;

    @OneToMany(
            mappedBy = "pergunta",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<AlternativaDesafio> alternativas = new ArrayList<>();
}
