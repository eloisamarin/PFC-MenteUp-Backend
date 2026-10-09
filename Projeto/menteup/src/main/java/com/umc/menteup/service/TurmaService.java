package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaService logSistemaService;

    public TurmaService(
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            LogSistemaService logSistemaService
    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logSistemaService = logSistemaService;
    }

    @Transactional
    public Turma criarTurma(Turma turma, String username) {
        Usuario autor = exigirProfessorOuAdministrador(username);

        turma.setId(null);
        Turma salva = turmaRepository.save(turma);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.CRIAR_TURMA,
                EntidadeLog.TURMA,
                salva.getId(),
                "Turma \"" + salva.getNome() + "\" criada.",
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
    public Turma atualizarTurma(Turma dados, String username) {
        exigirProfessorOuAdministrador(username);

        if (dados.getId() == null) {
            throw ApiException.requisicaoInvalida(
                    "ID_TURMA_OBRIGATORIO", "O id da turma é obrigatório para atualizar.");
        }

        Turma existente = turmaRepository.findById(dados.getId())
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        // Atualiza só o nome. Salvar o objeto recebido do cliente por inteiro
        // apagaria os alunos vinculados (a lista chega nula no JSON).
        existente.setNome(dados.getNome());

        return turmaRepository.save(existente);
    }

    @Transactional
    public void excluirTurma(Long turmaId, String username) {
        exigirProfessorOuAdministrador(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        turmaRepository.delete(turma);
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