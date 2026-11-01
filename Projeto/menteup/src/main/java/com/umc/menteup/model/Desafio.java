package com.umc.menteup.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Desafio no estilo Kahoot: as perguntas só aparecem depois que o professor inicia,
 * uma por vez, cada uma com o seu tempo. O relógio fica aqui no servidor
 * (perguntaExpiraEm), então o tempo não depende do horário do aparelho do aluno.
 */
@Entity
@Getter
@Setter
@Table(name = "tb_desafio")
public class Desafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O atributo título é obrigatório!")
    @Size(min = 5, max = 100, message = "O atributo título deve ter no mínimo 5 e no máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String titulo;

    @NotBlank(message = "O atributo Descrição é obrigatório!")
    @Size(min = 10, max = 1000, message = "O atributo Descrição deve ter no mínimo 10 e no máximo 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O Status do desafio é obrigatório!")
    @Column(nullable = false, length = 20)
    private StatusDesafio status = StatusDesafio.AGUARDANDO;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;

    @ManyToOne(optional = false)
    private Turma turma;

    @ManyToOne
    private Usuario usuario;

    // Posição na lista de perguntas; nulo enquanto o desafio não está em andamento
    private Integer indicePerguntaAtual;

    private LocalDateTime perguntaExpiraEm;

    @OneToMany(
            mappedBy = "desafio",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("ordem ASC")
    private List<PerguntaDesafio> perguntas = new ArrayList<>();
}
