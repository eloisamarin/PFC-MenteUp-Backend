package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.security.MascaraDados;

// Usuário nas listagens (LGPD: só o necessário para cada perfil)
// - resumido: id, nome e tipo. É o que o aluno recebe de todos
// - comEmailMascarado: também o e-mail mascarado (ni***@hotmail.com). É o que o professor recebe dos alunos,
//   para diferenciar dois alunos com o mesmo nome ao adicioná-los na turma
// - completo: também a situação da conta. É o que o administrador recebe, ainda com o e-mail mascarado
//   (o e-mail inteiro só aparece quando ele pede em GET /usuarios/{id}/email, e fica registrado no log)
// - proprio: o completo com o e-mail inteiro, só para a própria pessoa
// Os campos nulos não aparecem no JSON
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioListagemResponse(
        Long id,
        String nomeUsuario,
        String tipoUsuario,
        // E-mail inteiro: só na resposta para a própria pessoa
        String usuario,
        String emailMascarado,
        Boolean aguardandoPrimeiroAcesso,
        // Animal escolhido no perfil (ex.: "PANDA"); ausente mostra as iniciais
        String avatar,
        // Pedido de professor: PENDENTE, APROVADA ou REJEITADA (só para o administrador)
        String statusCadastro
) {
    private static String avatarDe(Usuario usuario) {
        return usuario.getAvatar() != null ? usuario.getAvatar().name() : null;
    }

    public static UsuarioListagemResponse resumido(Usuario usuario) {
        return new UsuarioListagemResponse(usuario.getId(), usuario.getNomeUsuario(), usuario.getTipoUsuario(),
                null, null, null, avatarDe(usuario), null);
    }

    public static UsuarioListagemResponse comEmailMascarado(Usuario usuario) {
        return new UsuarioListagemResponse(usuario.getId(), usuario.getNomeUsuario(), usuario.getTipoUsuario(),
                null, MascaraDados.email(usuario.getUsuario()), null, avatarDe(usuario), null);
    }

    public static UsuarioListagemResponse completo(Usuario usuario) {
        return new UsuarioListagemResponse(
                usuario.getId(),
                usuario.getNomeUsuario(),
                usuario.getTipoUsuario(),
                null,
                MascaraDados.email(usuario.getUsuario()),
                usuario.isAguardandoPrimeiroAcesso(),
                avatarDe(usuario),
                usuario.getStatusCadastro() != null ? usuario.getStatusCadastro().name() : null);
    }

    public static UsuarioListagemResponse proprio(Usuario usuario) {
        UsuarioListagemResponse base = completo(usuario);
        return new UsuarioListagemResponse(base.id(), base.nomeUsuario(), base.tipoUsuario(), usuario.getUsuario(),
                base.emailMascarado(), base.aguardandoPrimeiroAcesso(), base.avatar(), base.statusCadastro());
    }
}
