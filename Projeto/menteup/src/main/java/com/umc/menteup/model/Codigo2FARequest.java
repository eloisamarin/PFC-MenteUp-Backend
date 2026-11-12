package com.umc.menteup.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// POST /usuarios/validar-2fa
public class Codigo2FARequest {

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String usuario;

    @NotBlank(message = "O código é obrigatório.")
    @Pattern(regexp = "\\d{6}", message = "O código deve ter 6 dígitos.")
    private String codigo;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}