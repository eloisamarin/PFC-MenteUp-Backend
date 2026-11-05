package com.umc.menteup.service;

import com.umc.menteup.dto.DesempenhoAlunoResponse;
import com.umc.menteup.dto.DesempenhoTurmaResponse;
import com.umc.menteup.dto.ResumoDesempenhoAluno;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DesempenhoService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final ResultadoAtividadeRepository resultadoAtividadeRepository;

    public DesempenhoService(
            TurmaRepository turmaRepository,
            UsuarioRepository usuarioRepository,
            AtividadeRepository atividadeRepository,
            ResultadoAtividadeRepository resultadoAtividadeRepository

    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.resultadoAtividadeRepository = resultadoAtividadeRepository;

    }

    //

    private Usuario carregarUsuario(String username)
    {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado"
                ));
    }
    /*
    - O Administrador vizualiza as informações de  todas turmas.
    - O Professor vizualiza as informações as turmas que esteja vinculado.
    - O Aluno nao acesso autorizado.

     */

    @Transactional(readOnly = true)
    public List<TurmaResponse> listarTurmas(String username) {
        Usuario usuario = carregarUsuario(username);

        List<Turma> turmas;

        if (TipoUsuario.ehAdministrador(usuario)) {
            turmas = turmaRepository.findAll();
        } else if (TipoUsuario.ehProfessor(usuario)) {
            turmas = turmaRepository.findAllByProfessor_Id(usuario.getId());
        } else {
            throw ApiException.proibido(
                    "SOMENTE_PROFESSOR_OU_ADMINISTRADOR",
                    "Esta operação é exclusiva para professores e administradores.");
        }

        return turmas.stream()
                .map(turma -> new TurmaResponse(turma.getId(), turma.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DesempenhoTurmaResponse buscarDesempenho(Long turmaId, String username) {

        // Carregar os dados
        Usuario usuario = carregarUsuario(username);

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        // Verifica a permissao
        boolean professorDaTurma = turma.getProfessor() != null
                && turma.getProfessor().getId().equals(usuario.getId());

        if (!TipoUsuario.ehAdministrador(usuario) && !professorDaTurma) {
            throw ApiException.proibido(
                    "SEM_ACESSO_A_TURMA", "Você não tem acesso ao desempenho desta turma.");
        }

        // Pesquisa

        Map<Long, ResumoDesempenhoAluno> resumos = resultadoAtividadeRepository
                .buscarProgressoDaTurma(turmaId)
                .stream()
                .collect(Collectors.toMap(ResumoDesempenhoAluno::alunoId, Function.identity()));

        long totalAtividades = atividadeRepository
                .countByTurma_IdAndStatusNot(turmaId, StatusAtividade.PENDENTE);


        // Lista Resultado dos Alunos
        List<DesempenhoAlunoResponse> alunos = turma.getAlunos().stream()
                .map(aluno -> {
                    ResumoDesempenhoAluno resumo = resumos.getOrDefault(
                            aluno.getId(),
                            new ResumoDesempenhoAluno(aluno.getId(), 0L, 0L, 0L));

                    return new DesempenhoAlunoResponse(
                            aluno.getId(),
                            aluno.getNomeUsuario(),
                            resumo.pontuacao(),
                            resumo.acertos(),
                            resumo.atividadesConcluidas());
                })
                .sorted(Comparator.comparing(DesempenhoAlunoResponse::pontuacao).reversed())
                .toList();

        //Calcula os totais e mostra o resultado

        long pontuacaoTotal = alunos.stream()
                .mapToLong(DesempenhoAlunoResponse::pontuacao)
                .sum();

        long conclusoes = alunos.stream()
                .mapToLong(DesempenhoAlunoResponse::atividadesConcluidas)
                .sum();

        double mediaPontuacao = alunos.isEmpty()
                ? 0.0
                : Math.round(pontuacaoTotal * 10.0 / alunos.size()) / 10.0;

        long conclusoesPossiveis = alunos.size() * totalAtividades;

        int taxaParticipacao = conclusoesPossiveis == 0
                ? 0
                : (int) Math.round(conclusoes * 100.0 / conclusoesPossiveis);

        return new DesempenhoTurmaResponse(
                turma.getId(),
                turma.getNome(),
                alunos.size(),
                totalAtividades,
                mediaPontuacao,
                taxaParticipacao,
                alunos);
    }


}

