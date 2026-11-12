package com.umc.menteup.model;

/**
 * Helpers para o campo textual Usuario.tipoUsuario (ALUNO, PROFESSOR, ADMINISTRADOR).
 * Fica fora de Usuario para não virar propriedade serializada pelo Jackson.
 */
public final class TipoUsuario {

    public static final String ALUNO = "ALUNO";
    public static final String PROFESSOR = "PROFESSOR";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    // Únicos valores aceitos no campo tipoUsuario
    private static final java.util.Set<String> VALIDOS = java.util.Set.of(ALUNO, PROFESSOR, ADMINISTRADOR);

    // Tipos que qualquer pessoa pode escolher no cadastro público
    private static final java.util.Set<String> CADASTRO_PUBLICO = java.util.Set.of(ALUNO, PROFESSOR);

    private TipoUsuario() {
    }

    // " aluno " -> "ALUNO"; null continua null
    public static String normalizar(String tipo) {
        return tipo == null ? null : tipo.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public static boolean ehValido(String tipo) {
        return VALIDOS.contains(normalizar(tipo));
    }

    public static boolean permitidoNoCadastroPublico(String tipo) {
        return CADASTRO_PUBLICO.contains(normalizar(tipo));
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
