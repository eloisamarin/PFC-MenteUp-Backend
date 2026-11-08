package com.umc.menteup.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O Atributo Nome é Obrigatório!")
    private String nomeUsuario;


    @NotBlank(message = "O Atributo Tipo de Usuario é Obrigatório!")
    private String tipoUsuario;

    @NotBlank(message =  "O Atributo Usuario é Obrigatório!")
    @Email(message = "O Atributo Usuario é Obrigatório!")
    private String usuario;

    @NotBlank(message = "O Atributo Senha é Obrigatório!")
    @Size(min = 8, message = "A Senha deve ter no mínimo 8 caracteres")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "avatar", length = 20)
    private AvatarAnimal avatar;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "usuario", cascade = CascadeType.REMOVE)
    @JsonIgnoreProperties(value = "usuario", allowSetters = true)
    private List<Atividade> atividade;

    @ManyToMany(mappedBy = "alunos", fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"alunos", "atividade"}, allowSetters = true)
    private List<Turma> turma;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    // Senhas erradas seguidas no login; volta a zero quando acerta ou redefine a senha
    @JsonIgnore
    @Column(name = "tentativas_login")
    private Integer tentativasLogin;

    // Preenchido quando a conta é bloqueada
    @JsonIgnore
    @Column(name = "bloqueado_em")
    private LocalDateTime bloqueadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public List<Atividade> getAtividade() {
        return atividade;
    }

    public void setAtividade(List<Atividade> atividade) {
        this.atividade = atividade;
    }

    public List<Turma> getTurma() {
        return turma;
    }

    public void setTurma(List<Turma> turma) {
        this.turma = turma;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public int getTentativasLogin() {
        return tentativasLogin == null ? 0 : tentativasLogin;
    }

    public void setTentativasLogin(Integer tentativasLogin) {
        this.tentativasLogin = tentativasLogin;
    }

    public boolean estaBloqueado() {
        return bloqueadoEm != null;
    }

    public void setBloqueadoEm(LocalDateTime bloqueadoEm) {
        this.bloqueadoEm = bloqueadoEm;
    }
    public AvatarAnimal getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarAnimal avatar) {
        this.avatar = avatar;
    }
}
