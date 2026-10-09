package com.umc.menteup.util;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioLogin;

public class TestBuilder {

    private TestBuilder() {
    }

    // Cria um usuário do tipo ALUNO (padrão dos testes)
    public static Usuario criarUsuario(Long id, String nome, String usuario, String senha) {
        return criarUsuario(id, nome, usuario, senha, "ALUNO");
    }

    public static Usuario criarUsuario(Long id, String nome, String usuario, String senha, String tipo) {
        Usuario novo = new Usuario();
        novo.setId(id);
        novo.setNomeUsuario(nome);
        novo.setUsuario(usuario);
        novo.setSenha(senha);
        novo.setTipoUsuario(tipo);
        return novo;
    }

    public static UsuarioLogin criarUsuarioLogin(String usuario, String senha) {
        UsuarioLogin login = new UsuarioLogin();
        login.setUsuario(usuario);
        login.setSenha(senha);
        return login;
    }
}