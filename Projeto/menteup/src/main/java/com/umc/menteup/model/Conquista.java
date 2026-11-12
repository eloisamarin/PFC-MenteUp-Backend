package com.umc.menteup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

///Catálogo de conquistas
///Para tirar uma do ar, use ativa = false em vez de excluir: assim quem já ganhou não perde
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tb_conquista")
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaConquista categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regra", nullable = false, length = 40)
    private TipoRegraConquista tipoRegra;

    ///Valor a alcançar (ex.: 501 XP, 10 atividades, 30 dias)
    @Column(nullable = false)
    private Integer meta;

    @Column(name = "recompensa_xp", nullable = false)
    private Integer recompensaXp;

    ///Nome do ícone usado no front-end
    @Column(length = 60)
    private String icone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RaridadeConquista raridade;

    @Column(nullable = false)
    private Boolean ativa = true;

    ///Se true, nome e descrição ficam escondidos até o aluno ganhar
    @Column(nullable = false)
    private Boolean secreta = false;

    @Column(name = "ordem_exibicao", nullable = false)
    private Integer ordemExibicao;

    public Conquista(String codigo, String nome, String descricao,
                     CategoriaConquista categoria, TipoRegraConquista tipoRegra,
                     int meta, int recompensaXp, String icone,
                     RaridadeConquista raridade, int ordemExibicao) {
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.tipoRegra = tipoRegra;
        this.meta = meta;
        this.recompensaXp = recompensaXp;
        this.icone = icone;
        this.raridade = raridade;
        this.ordemExibicao = ordemExibicao;
    }
}
