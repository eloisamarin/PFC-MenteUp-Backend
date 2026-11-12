package com.umc.menteup.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Entrada do /logar (usuario + senha) e também a resposta do login (token, nome, tipo...).
// As validações só valem na entrada, onde há @Valid.
public class UsuarioLogin {

    private Long id;
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String usuario;

    @NotBlank(message = "A senha é obrigatória.")
    private String senha;
    private String token;
    private String tipoUsuario;
    private String avatar;

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}