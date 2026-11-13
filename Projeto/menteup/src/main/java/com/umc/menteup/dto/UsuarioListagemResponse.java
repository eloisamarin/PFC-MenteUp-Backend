package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.menteup.model.Usuario;

/*
 * Usuário nas listagens (LGPD: minimização dos dados).
 * - resumido: só id, nome e tipo. É o que o aluno recebe de todos.
 * - comEmail: id, nome, tipo e e-mail. É o que o professor recebe dos alunos,
 *   para escolher o aluno certo ao adicioná-lo na turma.
 * - completo: também se o convite está pendente. Só para o administrador
 *   (tela de usuários) ou para o próprio usuário.
 * Os campos nulos não aparecem no JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioListagemResponse(
        Long id,
        String nomeUsuario,
        String tipoUsuario,
        String usuario,
        Boolean aguardandoPrimeiroAcesso,
        // Animal escolhido no perfil (ex.: "PANDA"); ausente mostra as iniciais
        String avatar
) {

    private static String avatarDe(Usuario usuario) {
        return usuario.getAvatar() != null ? usuario.getAvatar().name() : null;
    }

    public static UsuarioListagemResponse resumido(Usuario usuario) {
        return new UsuarioListagemResponse(
                usuario.getId(), usuario.getNomeUsuario(), usuario.getTipoUsuario(), null, null, avatarDe(usuario));
    }

    public static UsuarioListagemResponse comEmail(Usuario usuario) {
        return new UsuarioListagemResponse(
                usuario.getId(), usuario.getNomeUsuario(), usuario.getTipoUsuario(), usuario.getUsuario(), null,
                avatarDe(usuario));
    }

    public static UsuarioListagemResponse completo(Usuario usuario) {
        return new UsuarioListagemResponse(
                usuario.getId(),
                usuario.getNomeUsuario(),
                usuario.getTipoUsuario(),
                usuario.getUsuario(),
                usuario.isAguardandoPrimeiroAcesso(),
                avatarDe(usuario));
    }
}
