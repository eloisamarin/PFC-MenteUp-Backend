package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
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
    @Size(min = 10, max= 1000, message = "O atributo Descrição deve ter no mínimo 10 e no máximo 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String descricao;

    @NotNull(message = "O atributo pontuação é obrigatório!")
    @Min(value = 1, message = "O atributo pontuação deve ser no mínimo 1 ponto")
    // Sem teto, uma atividade de 1 milhão de XP distorceria o ranking
    @Max(value = 1000, message = "O atributo pontuação deve ser no máximo 1000 pontos")
    private Integer pontuacao;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "O Status da atividade é obrigatório!")
    private StatusAtividade status = StatusAtividade.PENDENTE;

    /**
     * Template escolhido na etapa 2 da criação. A coluna é anulável para não quebrar
     * as linhas já existentes no banco; o getter trata nulo como QUIZ.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30)
    private TipoAtividade tipo = TipoAtividade.QUIZ;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    private Date dataAtualizacao;

    @ManyToOne
    @JsonIgnoreProperties("atividade")
    private Turma turma;

    // Autor da atividade: definido pelo token no POST /atividades, nunca pelo corpo da requisição.
    // Não sai no JSON (o questionário devolve só id e nome pelo UsuarioResponse).
    @ManyToOne
    @JsonIgnore
    private Usuario usuario;

    @OneToMany(
            mappedBy = "atividade",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Pergunta> perguntas = new ArrayList<>();

    public TipoAtividade getTipo() {
        return TipoAtividade.ouPadrao(tipo);
    }
}
