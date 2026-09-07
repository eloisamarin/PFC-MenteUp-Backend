package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "tb_atividade")
public class Atividade {


    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Getter @Setter
    @NotBlank(message = "O atributo título é obrigatório!")
    @Size(min = 5, max = 100, message = "O atributo título deve ter no mínimo 5 e no máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String titulo;

    @NotBlank(message = "A atributo Descrição é obrigatório!")
    @Size(min = 10, max= 1000, message = "O atributo Descrição deve ter no mínimo 10 e no mánimo 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String descricao;

    @NotNull(message = "O atributo pontuação é obrigatório!")
    @Min(value = 1, message = "O atibuto pontuação deve ser no mínimo 1 ponto")
    private Integer pontuacao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O Status da atividade é obrigatório!")
    private StatusAtividade status = StatusAtividade.PENDENTE;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private Date dataAtualizacao;

    @ManyToOne
    @JsonIgnoreProperties("atividade")
    private Turma turma;

    @ManyToOne
    @JsonIgnoreProperties("atividade")
    private Usuario usuario;

    @OneToMany(
            mappedBy = "atividade",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Pergunta> perguntas = new ArrayList<>();
}
