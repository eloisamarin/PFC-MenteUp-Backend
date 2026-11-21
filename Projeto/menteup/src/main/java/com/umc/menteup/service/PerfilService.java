package com.umc.menteup.service;

import com.umc.menteup.security.MascaraDados;
import com.umc.menteup.security.RegraSenha;
import com.umc.menteup.dto.PerfilResponse;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.umc.menteup.model.AvatarAnimal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PerfilService {

    ///E-mail que fica no lugar do original quando a conta é excluída
    private static final String EMAIL_REMOVIDO_PREFIXO = "removido-";
    private static final String EMAIL_REMOVIDO_DOMINIO = "@menteup.invalid";
    private static final String NOME_REMOVIDO = "Usuário removido";

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Codigo2FARepository codigo2FARepository;
    private final LogSistemaService logSistemaService;
    private final LogSistemaRepository logSistemaRepository;

    public PerfilService(UsuarioRepository usuarioRepository, TurmaRepository turmaRepository,
                         PasswordEncoder passwordEncoder, Codigo2FARepository codigo2FARepository,
                         LogSistemaService logSistemaService, LogSistemaRepository logSistemaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.passwordEncoder = passwordEncoder;
        this.codigo2FARepository = codigo2FARepository;
        this.logSistemaService = logSistemaService;
        this.logSistemaRepository = logSistemaRepository;
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
        return MascaraDados.email(email);
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
    @Transactional
    public void alterarSenha(String username, String senhaAtual, String novaSenha) {
        RegraSenha.validarNovaSenha(novaSenha);
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
        // Quem estiver logado com a senha antiga (inclusive em outro aparelho) precisa entrar de novo
        usuario.encerrarSessoes();

        logSistemaService.registrarSucesso(usuario, AcaoLog.ALTERAR_SENHA, EntidadeLog.USUARIO, usuario.getId(),
                "Usuário #" + usuario.getId() + " trocou a senha pelo perfil.", null);
    }

    ///Os dados são apagados de forma que não seja possível identificar a pessoa
    @Transactional
    public void excluirConta(String username, String senha) {
        Usuario usuario = carregarUsuario(username);

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw ApiException.requisicaoInvalida("SENHA_INCORRETA", "Senha incorreta.");
        }

        ///Evita que o sistema fique sem nenhum administrador
        if (TipoUsuario.ehAdministrador(usuario)) {
            throw ApiException.proibido(
                    "ADMINISTRADOR_NAO_PODE_EXCLUIR", "Contas de administrador não podem ser excluídas por aqui.");
        }

        // Registro do atendimento do pedido de exclusão (só com o id, sem dados da pessoa)
        logSistemaService.registrarSucesso(usuario, AcaoLog.EXCLUIR_PROPRIA_CONTA, EntidadeLog.USUARIO,
                usuario.getId(), "Conta #" + usuario.getId() + " excluída pelo próprio usuário.",
                Map.of("tipoUsuario", usuario.getTipoUsuario()));

        anonimizar(usuario);
    }

    ///Administrador exclui um aluno ou professor do mesmo jeito que o próprio usuário faria
    @Transactional
    public void excluirPorAdministrador(Long id, String usernameAdmin, String senhaAdmin) {
        Usuario autor = carregarUsuario(usernameAdmin);

        if (!TipoUsuario.ehAdministrador(autor)) {
            throw ApiException.proibido("SOMENTE_ADMINISTRADOR",
                    "Somente administradores podem excluir usuários.");
        }

        ///Administrador confirma com a própria senha
        if (!passwordEncoder.matches(senhaAdmin, autor.getSenha())) {
            throw ApiException.requisicaoInvalida("SENHA_INCORRETA", "Senha incorreta.");
        }

        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        ///Administrador não pode excluir outro administrador
        if (TipoUsuario.ehAdministrador(alvo)) {
            throw ApiException.proibido("ADMINISTRADOR_NAO_PODE_EXCLUIR",
                    "Contas de administrador não podem ser excluídas.");
        }

        ///Conta que já foi excluída antes
        if (ehContaExcluida(alvo)) {
            throw ApiException.conflito("USUARIO_JA_EXCLUIDO", "Este usuário já foi excluído.");
        }

        anonimizar(alvo);

        ///O log guarda só o id para não expor quem era a pessoa
        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.EXCLUIR_USUARIO,
                EntidadeLog.USUARIO,
                alvo.getId(),
                "Conta do usuário #" + alvo.getId() + " excluída pelo administrador.",
                Map.of("tipoUsuario", alvo.getTipoUsuario())
        );
    }

    // Descarte pela rotina de retenção (pedido rejeitado ou convite vencido): mesma anonimização da exclusão
    // Devolve false quando a conta já tinha sido excluída antes
    @Transactional
    public boolean anonimizarPorRetencao(Usuario usuario) {
        if (ehContaExcluida(usuario)) {
            return false;
        }
        anonimizar(usuario);
        // Sai das buscas da rotina, para não ser encontrada de novo nos próximos dias
        usuario.setStatusAlteradoEm(null);
        usuario.setPrimeiroAcessoPendente(false);
        return true;
    }

    ///Apaga o que identifica a pessoa, mas mantém o usuário e o histórico
    private void anonimizar(Usuario usuario) {
        String emailOriginal = usuario.getUsuario();

        ///O aluno sai das turmas (o vínculo fica do lado da Turma)
        for (Turma turma : turmaRepository.findAllByAlunos_Id(usuario.getId())) {
            turma.getAlunos().removeIf(aluno -> aluno.getId().equals(usuario.getId()));
        }

        // Os logs antigos escreviam o nome entre aspas: ele sai de lá também
        logSistemaRepository.substituirNasDescricoes(
                "\"" + usuario.getNomeUsuario() + "\"", "\"" + NOME_REMOVIDO + "\"");

        usuario.setNomeUsuario(NOME_REMOVIDO);
        usuario.setUsuario(EMAIL_REMOVIDO_PREFIXO + usuario.getId() + EMAIL_REMOVIDO_DOMINIO);
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setFcmToken(null);
        usuario.encerrarSessoes();

        codigo2FARepository.deleteByUsuario(emailOriginal);
    }

    private static boolean ehContaExcluida(Usuario usuario) {
        return usuario.getUsuario() != null && usuario.getUsuario().endsWith(EMAIL_REMOVIDO_DOMINIO);
    }
}