package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "tb_turma")
@Getter
@Setter
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
    private List<Atividade> atividade = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "tb_turma_aluno",
            joinColumns = @JoinColumn(name = "turma_id"),
            inverseJoinColumns = @JoinColumn(name = "aluno_id")
    )
    // LGPD: na turma, cada aluno sai só com id, nome e tipo
    @JsonIgnoreProperties(
            value = {"usuario", "turma", "atividade", "aguardandoPrimeiroAcesso"},
            allowSetters = true)
    private List<Usuario> alunos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    @JsonIgnore
    private Usuario professor;

    // Entrada (POST/PUT /turmas): o administrador escolhe o professor responsável.
    // Não vai para o banco: o vínculo fica gravado em "professor".
    @Transient
    @JsonProperty(value = "professorId", access = JsonProperty.Access.WRITE_ONLY)
    private Long professorIdInformado;

    // Saída: o professor responsável aparece nas telas de turma, atividade e desafio
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public ProfessorResponsavel getProfessorResponsavel() {
        return professor == null
                ? null
                : new ProfessorResponsavel(professor.getId(), professor.getNomeUsuario());
    }

    public record ProfessorResponsavel(Long id, String nome) {
    }
}
