package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "resposta_atividade",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = { "progresso_id", "pergunta_id" }
                )
        }
)

@Getter
@Setter
public class RespostaAtividade {
    @Id
    @GeneratedValue ( strategy = GenerationType.IDENTITY )
    private Long id;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn ( name = "progresso_id", nullable = false )
    private ProgressoAtividade progresso;

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn( name = "pergunta_id", nullable = false )
    private Pergunta pergunta;

    ///Quiz, V/F e associação: alternativa escolhida. Ordenação e lacunas: fica nulo
    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn ( name = "alternativa_id" )
    private Alternativa alternativa;


    ///Ordenação e lacunas: ids das alternativas na ordem em que o aluno respondeu
    @ElementCollection
    @CollectionTable(
            name = "resposta_atividade_sequencia",
            joinColumns = @JoinColumn(name = "resposta_id")
    )
    @OrderColumn(name = "posicao")
    @Column(name = "alternativa_id", nullable = false)
    private List<Long> alternativasIds = new ArrayList<>();
    ///Preenchido na conclusão: se o aluno acertou esta pergunta. Nulo enquanto a atividade está em andamento
    private Boolean correta;



}
