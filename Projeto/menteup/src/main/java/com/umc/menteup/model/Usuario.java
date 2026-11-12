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
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nomeUsuario;


    @NotBlank(message = "O Atributo Tipo de Usuario é Obrigatório!")
    private String tipoUsuario;

    @NotBlank(message =  "O Atributo Usuario é Obrigatório!")
    @Email(message = "E-mail inválido.")
    private String usuario;

    @NotBlank(message = "O Atributo Senha é Obrigatório!")
    // Máximo de 72: o BCrypt ignora o que passa de 72 bytes (o hash gravado tem 60, cabe na regra)
    @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
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

    // Token do celular para as notificações: nunca sai no JSON (é gravado pelo PUT /usuarios/fcm-token)
    @JsonIgnore
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

    // Criado pelo administrador e ainda sem criar a senha/aceitar o termo (null = false)
    @JsonIgnore
    @Column(name = "primeiro_acesso_pendente")
    private Boolean primeiroAcessoPendente;

    // Quando a pessoa aceitou o Termo de Aceite e a Política de Privacidade
    @JsonIgnore
    @Column(name = "termo_aceito_em")
    private LocalDateTime termoAceitoEm;

    // Só chega na requisição do cadastro público ("Li e aceito o Termo e a Política").
    // Não vai para o banco nem volta no JSON: o que fica gravado é a data em termoAceitoEm.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean aceitouTermos;

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

    public boolean isPrimeiroAcessoPendente() {
        return Boolean.TRUE.equals(primeiroAcessoPendente);
    }

    public void setPrimeiroAcessoPendente(Boolean primeiroAcessoPendente) {
        this.primeiroAcessoPendente = primeiroAcessoPendente;
    }

    public boolean isAceitouTermos() {
        return aceitouTermos;
    }

    public void setAceitouTermos(boolean aceitouTermos) {
        this.aceitouTermos = aceitouTermos;
    }

    public LocalDateTime getTermoAceitoEm() {
        return termoAceitoEm;
    }

    public void setTermoAceitoEm(LocalDateTime termoAceitoEm) {
        this.termoAceitoEm = termoAceitoEm;
    }

    // Aparece no JSON (só leitura) para a tela de usuários mostrar "Aguardando primeiro acesso"
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isAguardandoPrimeiroAcesso() {
        return isPrimeiroAcessoPendente();
    }
}
