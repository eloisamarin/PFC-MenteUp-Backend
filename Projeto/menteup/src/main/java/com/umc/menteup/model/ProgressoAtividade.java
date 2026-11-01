package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "progresso_atividade",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"usuario_id", "atividade_id"}
                )
        }
)

@Getter
@Setter
public class ProgressoAtividade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "atividade_id", nullable = false)
    private Atividade atividade;

    @Column ( nullable = false )
    private LocalDateTime iniciadoEm = LocalDateTime.now();

    @Column ( nullable = false )
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(
            mappedBy = "progresso",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RespostaAtividade> respostas  =new ArrayList<>();

    @PreUpdate
    public void atualizarData() {
        atualizadoEm = LocalDateTime.now();
    }
}
