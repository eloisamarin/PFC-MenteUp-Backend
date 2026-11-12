package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.SolicitacaoTurmaRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final SolicitacaoTurmaRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaService logSistemaService;

    public TurmaService(
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            LogSistemaService logSistemaService,
            SolicitacaoTurmaRepository solicitacaoRepository
    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logSistemaService = logSistemaService;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional
    public Turma criarTurma(Turma turma, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        turma.setId(null);

        // Professor: a turma é dele. Administrador: escolhe (opcional) o professor responsável.
        if (TipoUsuario.ehProfessor(autor)) {
            exigirSemEscolhaDeProfessor(turma);
            turma.setProfessor(autor);
        } else if (turma.getProfessorIdInformado() != null) {
            turma.setProfessor(carregarProfessor(turma.getProfessorIdInformado()));
        }

        Turma salva = turmaRepository.save(turma);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.CRIAR_TURMA,
                EntidadeLog.TURMA,
                salva.getId(),
                "Turma \"" + salva.getNome() + "\" criada" + descreverProfessor(salva) + ".",
                Map.of("turmaId", salva.getId())
        );

        return salva;
    }

    @Transactional
    public void vincularAluno(Long turmaId, Long alunoId, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        exigirAcessoATurma(turma, autor);

        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ALUNO_NAO_ENCONTRADO", "Aluno não encontrado."));

        if (!TipoUsuario.ehAluno(aluno)) {
            throw ApiException.requisicaoInvalida(
                    "USUARIO_NAO_E_ALUNO", "Só é possível vincular usuários do tipo aluno.");
        }

        if (turma.getAlunos() == null) {
            turma.setAlunos(new ArrayList<>());
        }

        boolean jaVinculado = turma.getAlunos().stream()
                .anyMatch(matriculado -> matriculado.getId().equals(aluno.getId()));

        if (jaVinculado) {
            throw ApiException.conflito(
                    "ALUNO_JA_VINCULADO", "O aluno já está vinculado a esta turma.");
        }

        turma.getAlunos().add(aluno);
        turmaRepository.save(turma);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.ADICIONAR_ALUNO_TURMA,
                EntidadeLog.TURMA,
                turma.getId(),
                "Aluno \"" + aluno.getNomeUsuario() + "\" vinculado à turma \"" + turma.getNome() + "\".",
                Map.of("alunoId", aluno.getId(), "turmaId", turma.getId())
        );
    }

    @Transactional
    public void removerAluno(Long turmaId, Long alunoId, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        exigirAcessoATurma(turma, autor);

        Usuario aluno = turma.getAlunos().stream()
                .filter(matriculado -> matriculado.getId().equals(alunoId))
                .findFirst()
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "ALUNO_NAO_VINCULADO", "O aluno não está vinculado a esta turma."));

        turma.getAlunos().remove(aluno);
        turmaRepository.save(turma);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.REMOVER_ALUNO_TURMA,
                EntidadeLog.TURMA,
                turma.getId(),
                "Aluno \"" + aluno.getNomeUsuario() + "\" removido da turma \"" + turma.getNome() + "\".",
                Map.of("alunoId", aluno.getId(), "turmaId", turma.getId())
        );
    }

    @Transactional
    public Turma atualizarTurma(Turma dados, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        if (dados.getId() == null) {
            throw ApiException.requisicaoInvalida(
                    "ID_TURMA_OBRIGATORIO", "O id da turma é obrigatório para atualizar.");
        }

        Turma existente = turmaRepository.findById(dados.getId())
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        // Professor só altera as turmas dele
        exigirAcessoATurma(existente, autor);

        // Só o administrador troca o professor responsável
        if (dados.getProfessorIdInformado() != null) {
            if (!TipoUsuario.ehAdministrador(autor)) {
                exigirSemEscolhaDeProfessor(dados);
            }
            existente.setProfessor(carregarProfessor(dados.getProfessorIdInformado()));
        }

        // Atualiza só o nome. Salvar o objeto recebido do cliente por inteiro
        // apagaria os alunos vinculados (a lista chega nula no JSON).
        String nomeAnterior = existente.getNome();
        existente.setNome(dados.getNome());

        Turma salva = turmaRepository.save(existente);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.EDITAR_TURMA,
                EntidadeLog.TURMA,
                salva.getId(),
                "Turma \"" + nomeAnterior + "\" atualizada para \"" + salva.getNome() + "\""
                        + descreverProfessor(salva) + ".",
                Map.of("nomeAnterior", String.valueOf(nomeAnterior), "nomeNovo", String.valueOf(salva.getNome()))
        );

        return salva;
    }

    @Transactional
    public void excluirTurma(Long turmaId, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        // Professor só exclui as turmas dele
        exigirAcessoATurma(turma, autor);

        // Os pedidos de entrada/saída da turma são apagados junto
        solicitacaoRepository.apagarDaTurma(turmaId);
        turmaRepository.delete(turma);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.EXCLUIR_TURMA,
                EntidadeLog.TURMA,
                turmaId,
                "Turma \"" + turma.getNome() + "\" excluída.",
                Map.of("nome", String.valueOf(turma.getNome()))
        );
    }
    // Administrador: qualquer turma. Professor: só as turmas dele
    private void exigirAcessoATurma(Turma turma, Usuario usuario) {
        boolean professorDaTurma = turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!TipoUsuario.ehAdministrador(usuario) && !professorDaTurma) {
            throw ApiException.proibido(
                    "SEM_ACESSO_A_TURMA", "Só o professor da turma ou um administrador pode alterar esta turma.");
        }
    }

    private static void exigirSemEscolhaDeProfessor(Turma turma) {
        if (turma.getProfessorIdInformado() != null) {
            throw ApiException.proibido("SOMENTE_ADMINISTRADOR_VINCULA_PROFESSOR",
                    "Somente o administrador escolhe o professor responsável pela turma.");
        }
    }

    private Usuario carregarProfessor(Long professorId) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "PROFESSOR_NAO_ENCONTRADO", "Professor não encontrado."));

        if (!TipoUsuario.ehProfessor(professor)) {
            throw ApiException.requisicaoInvalida(
                    "USUARIO_NAO_E_PROFESSOR", "O usuário escolhido não é um professor.");
        }

        return professor;
    }

    private static String descreverProfessor(Turma turma) {
        return turma.getProfessor() == null
                ? ""
                : " (professor responsável: " + turma.getProfessor().getNomeUsuario() + ")";
    }

    private Usuario exigirProfessorOuAdministrador(String username) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (!TipoUsuario.ehProfessorOuAdministrador(usuario)) {
            throw ApiException.proibido(
                    "SOMENTE_PROFESSOR_OU_ADMINISTRADOR",
                    "Esta operação é exclusiva para professores e administradores.");
        }

        return usuario;
    }
}