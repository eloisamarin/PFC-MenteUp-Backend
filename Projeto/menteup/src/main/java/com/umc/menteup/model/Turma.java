package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
@Table(name = "tb_turma")
public class Turma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O Atributo Nome da Turma é obrigatório")
    @Size(max = 255, message = "O atributo Nome de Turma deve ter no máximo 255 caracteres")
    @Column(length = 255)
    private String nome;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "turma",
            cascade = CascadeType.REMOVE
    )
    @JsonIgnoreProperties(value = "turma", allowSetters = true)
    private List<Atividade> atividade;

    @ManyToOne
    @JsonIgnoreProperties("atividade")
    private Usuario usuario;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<Atividade> getAtividade() {
        return atividade;
    }

    public void setAtividade(List<Atividade> atividade) {
        this.atividade = atividade;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
