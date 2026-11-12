package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

///O XP ganho com conquistas é a soma de xpConcedido. Conquista que foi ganha nunca é perdida
@Entity
@Getter
@Setter
@Table(
        name = "tb_usuario_conquista",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "conquista_id"})
)
public class UsuarioConquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conquista_id", nullable = false)
    private Conquista conquista;

    @Column(name = "progresso_atual", nullable = false)
    private Integer progressoAtual = 0;

    ///Meta usada para este aluno. Depois de ganha, não muda mais
    @Column(nullable = false)
    private Integer meta;

    @Column(nullable = false)
    private Boolean desbloqueada = false;

    @Column(name = "data_desbloqueio")
    private LocalDateTime dataDesbloqueio;

    ///XP recebido ao ganhar a conquista
    @Column(name = "xp_concedido", nullable = false)
    private Integer xpConcedido = 0;

    ///Fica false até o aluno ver a tela de comemoração
    @Column(nullable = false)
    private Boolean visualizada = false;

    @Column(name = "ultima_avaliacao", nullable = false)
    private LocalDateTime ultimaAvaliacao = LocalDateTime.now();

    ///Evita que duas gravações ao mesmo tempo apaguem uma à outra
    @Version
    private Long versao;

    public static UsuarioConquista nova(Usuario usuario, Conquista conquista) {
        UsuarioConquista uc = new UsuarioConquista();
        uc.setUsuario(usuario);
        uc.setConquista(conquista);
        uc.setMeta(conquista.getMeta());
        return uc;
    }
}
