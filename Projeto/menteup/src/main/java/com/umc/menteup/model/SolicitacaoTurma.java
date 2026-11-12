package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 * Pedido do aluno para ENTRAR ou SAIR de uma turma.
 * Fica PENDENTE até o professor da turma (ou um administrador) aprovar ou recusar.
 */
@Entity
@Table(name = "tb_solicitacao_turma")
@Getter
@Setter
@NoArgsConstructor // construtor vazio exigido pelo JPA
public class SolicitacaoTurma {

    public enum Tipo { ENTRADA, SAIDA }

    public enum Status { PENDENTE, APROVADA, RECUSADA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id")
    private Usuario aluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDENTE;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(name = "respondida_em")
    private LocalDateTime respondidaEm;

    // Professor ou administrador que aprovou/recusou
    @ManyToOne
    @JoinColumn(name = "respondida_por_id")
    private Usuario respondidaPor;

    public SolicitacaoTurma(Turma turma, Usuario aluno, Tipo tipo) {
        this.turma = turma;
        this.aluno = aluno;
        this.tipo = tipo;
    }

    public boolean estaPendente() {
        return status == Status.PENDENTE;
    }
}
