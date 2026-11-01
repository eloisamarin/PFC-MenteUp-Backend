package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @ManyToOne ( fetch = FetchType.LAZY )
    @JoinColumn ( name = "alternativa_id", nullable = false )
    private Alternativa alternativa;
}
