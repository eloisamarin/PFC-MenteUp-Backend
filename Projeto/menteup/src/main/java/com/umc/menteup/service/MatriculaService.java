package com.umc.menteup.service;

import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatriculaService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogSistemaService logSistemaService;

    public MatriculaService(
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            LogSistemaService logSistemaService
    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logSistemaService = logSistemaService;
    }

    public boolean estaMatriculado(Long turmaId, Long alunoId) {
        return turmaRepository.contarMatricula(turmaId, alunoId) > 0;
    }

    public void exigirMatricula(Atividade atividade, Usuario aluno) {
        Turma turma = atividade.getTurma();

        if (turma == null || !estaMatriculado(turma.getId(), aluno.getId())) {
            throw ApiException.proibido(
                    "ALUNO_FORA_DA_TURMA",
                    "Você não pertence à turma desta atividade."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<TurmaResponse> listarTurmasDoAluno(String username) {
        Usuario aluno = carregarAluno(username);

        return turmaRepository.findAllByAlunos_Id(aluno.getId())
                .stream()
                .map(turma -> new TurmaResponse(turma.getId(), turma.getNome()))
                .toList();
    }



    private Usuario carregarAluno(String username) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (!TipoUsuario.ehAluno(usuario)) {
            throw ApiException.proibido("SOMENTE_ALUNO", "Esta operação é exclusiva para alunos.");
        }

        return usuario;
    }

    private Turma carregarTurma(Long turmaId) {
        return turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado("TURMA_NAO_ENCONTRADA", "Turma não encontrada."));
    }
}
