package com.umc.menteup.service;

import com.umc.menteup.dto.PerfilResponse;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.umc.menteup.model.AvatarAnimal;

import java.util.List;
import java.util.UUID;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Codigo2FARepository codigo2FARepository;

    public PerfilService(UsuarioRepository usuarioRepository, TurmaRepository turmaRepository, PasswordEncoder passwordEncoder, Codigo2FARepository codigo2FARepository) {
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.passwordEncoder = passwordEncoder;
        this.codigo2FARepository = codigo2FARepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscarPerfil(String username) {
        Usuario usuario = carregarUsuario(username);

        return new PerfilResponse(
                usuario.getId(),
                usuario.getNomeUsuario(),
                anonimizarEmail(usuario.getUsuario()),
                usuario.getTipoUsuario(),
                usuario.getAvatar() == null ? null : usuario.getAvatar().name(),
                turmasDo(usuario));

    }

    // Aluno: turmas em que está matriculado. Professor: turmas que leciona. Administrador: nenhuma
    private List<TurmaResponse> turmasDo(Usuario usuario) {
        List<Turma> turmas;

        if (TipoUsuario.ehAluno(usuario)) {
            turmas = turmaRepository.findAllByAlunos_Id(usuario.getId());
        } else if (TipoUsuario.ehProfessor(usuario)) {
            turmas = turmaRepository.findAllByProfessor_Id(usuario.getId());
        } else {
            turmas = List.of();
        }

        return turmas.stream()
                .map(turma -> new TurmaResponse(turma.getId(), turma.getNome()))
                .toList();
    }
    // Troca a foto de perfil por um dos 10 animais
    @Transactional
    public void alterarAvatar(String username, AvatarAnimal avatar) {
        Usuario usuario = carregarUsuario(username);
        usuario.setAvatar(avatar);
        usuarioRepository.save(usuario);
    }

    // Anonimização do e-mail (em***@gmail.com)
    static String anonimizarEmail(String email) {
        int arroba = email == null ? -1 : email.indexOf('@');

        if (arroba <= 0) {
            return "***";
        }

        String visivel = email.substring(0, Math.min(2, arroba));
        return visivel + "***" + email.substring(arroba);
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
    @Transactional
    public void alterarSenha(String username, String senhaAtual, String novaSenha) {
        Usuario usuario = carregarUsuario(username);

        // Verificação de senha antiga para atuzalição da nova
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw ApiException.requisicaoInvalida(
                    "SENHA_ATUAL_INCORRETA", "A senha atual está incorreta.");
        }

        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw ApiException.requisicaoInvalida(
                    "NOVA_SENHA_IGUAL", "A nova senha deve ser diferente da atual.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
    }

    /*
     Os dados serão anonimiza que não será possível indentificar a pesssoa.
     */
    @Transactional
    public void excluirConta(String username, String senha) {
        Usuario usuario = carregarUsuario(username);

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw ApiException.requisicaoInvalida("SENHA_INCORRETA", "Senha incorreta.");
        }

        // Evita que o sistema fique sem nenhum administrador
        if (TipoUsuario.ehAdministrador(usuario)) {
            throw ApiException.proibido(
                    "ADMINISTRADOR_NAO_PODE_EXCLUIR", "Contas de administrador não podem ser excluídas por aqui.");
        }

        String emailOriginal = usuario.getUsuario();

        // O aluno sai das turmas (o vínculo fica do lado da Turma)
        for (Turma turma : turmaRepository.findAllByAlunos_Id(usuario.getId())) {
            turma.getAlunos().removeIf(aluno -> aluno.getId().equals(usuario.getId()));
        }

        usuario.setNomeUsuario("Usuário removido");
        usuario.setUsuario("removido-" + usuario.getId() + "@menteup.invalid");
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setFcmToken(null);

        codigo2FARepository.deleteByUsuario(emailOriginal);
    }
}