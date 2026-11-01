package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Column(nullable = false, unique = true)
    private String titulo_desafio;

    @NotBlank(message = "O atributo Descrição é obrigatório!")
    @Size(min = 10, max = 1000, message = "O atributo Descrição deve ter no mínimo 10 e no máximo 1000 caracteres")
    @Column(nullable = false, unique = true)
    private String descricao_desafio;

    @Enumerated(EnumType.STRING)
    @NotNull (message = " O Status da ativade é obrigatório!")
    private StatusDesafio statusDesafio = StatusDesafio.ATIVO;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao = LocalDateTime.now();

    @ManyToOne
    @JsonIgnoreProperties("desafio")
    private Turma turma;

    @ManyToOne
    @JsonIgnoreProperties("desafio")
    private Usuario usuario;

    @OneToMany(
            mappedBy =  "atividade",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Pergunta> perguntas = new ArrayList<>();






}
