package com.umc.menteup.model;

/**
 * Helpers para o campo textual Usuario.tipoUsuario (ALUNO, PROFESSOR, ADMINISTRADOR).
 * Fica fora de Usuario para não virar propriedade serializada pelo Jackson.
 */
public final class TipoUsuario {

    public static final String ALUNO = "ALUNO";
    public static final String PROFESSOR = "PROFESSOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private TipoUsuario() {
    }

    public static boolean ehAluno(Usuario usuario) {
        return usuario != null && ALUNO.equalsIgnoreCase(usuario.getTipoUsuario());
    }
    public static boolean ehProfessorOuAdministrador(Usuario usuario) {
        return usuario != null
                && (PROFESSOR.equalsIgnoreCase(usuario.getTipoUsuario())
                || ADMINISTRADOR.equalsIgnoreCase(usuario.getTipoUsuario()));
    }
    public static boolean ehProfessor(Usuario usuario) {
        return usuario != null && PROFESSOR.equalsIgnoreCase(usuario.getTipoUsuario());
    }
    public static boolean ehAdministrador(Usuario usuario) {
        return usuario != null && ADMINISTRADOR.equalsIgnoreCase(usuario.getTipoUsuario());
    }
}
