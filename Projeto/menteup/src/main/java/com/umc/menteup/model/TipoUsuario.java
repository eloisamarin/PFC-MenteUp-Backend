package com.umc.menteup.model;

/**
 * Helpers para o campo textual Usuario.tipoUsuario (ALUNO, PROFESSOR, ADMINISTRADOR).
 * Fica fora de Usuario para não virar propriedade serializada pelo Jackson.
 */
public final class TipoUsuario {

    public static final String ALUNO = "ALUNO";

    private TipoUsuario() {
    }

    public static boolean ehAluno(Usuario usuario) {
        return usuario != null && ALUNO.equalsIgnoreCase(usuario.getTipoUsuario());
    }
}
