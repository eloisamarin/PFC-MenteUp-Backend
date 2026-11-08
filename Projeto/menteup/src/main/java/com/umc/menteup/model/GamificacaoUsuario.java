package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

///Dados de gamificação que não existem em outras tabelas. Uma linha por aluno
///O Intensivão conta dias de login, não necessariamente dias de estudo
@Entity
@Getter
@Setter
@Table(name = "tb_gamificacao_usuario")
public class GamificacaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    ///Intensivão

    @Column(name = "dias_consecutivos", nullable = false)
    private Integer diasConsecutivos = 0;

    @Column(name = "maior_sequencia", nullable = false)
    private Integer maiorSequencia = 0;

    ///Último dia contado (horário de Brasília)
    @Column(name = "ultimo_acesso")
    private LocalDate ultimoAcesso;

    ///Ranking

    @Column(name = "posicao_ranking_atual")
    private Integer posicaoRankingAtual;

    ///Pior colocação já registrada; base para "subiu 5 posições"
    @Column(name = "pior_posicao_ranking")
    private Integer piorPosicaoRanking;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @PreUpdate
    public void atualizarData() {
        atualizadoEm = LocalDateTime.now();
    }

    public static GamificacaoUsuario novo(Usuario usuario) {
        GamificacaoUsuario g = new GamificacaoUsuario();
        g.setUsuario(usuario);
        return g;
    }
}
