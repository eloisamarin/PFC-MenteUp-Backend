package com.umc.menteup.service;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.dto.TotalPorAluno;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.RespostaDesafioRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioConquistaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingService {

    // Para o aluno, só os primeiros colocados aparecem com nome (os demais ficam ocultos)
    static final int NOMES_VISIVEIS_PARA_ALUNO = 10;
    static final String NOME_OCULTO = "Aluno";

    private final ResultadoAtividadeRepository resultadoRepository;
    private final RespostaDesafioRepository respostaDesafioRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;

    public RankingService(
            ResultadoAtividadeRepository resultadoRepository,
            RespostaDesafioRepository respostaDesafioRepository,
            UsuarioConquistaRepository usuarioConquistaRepository,
            UsuarioRepository usuarioRepository,
            TurmaRepository turmaRepository
    ) {
        this.resultadoRepository = resultadoRepository;
        this.respostaDesafioRepository = respostaDesafioRepository;
        this.usuarioConquistaRepository = usuarioConquistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
    }

    // Com turma: só os alunos dela, com os pontos das atividades e desafios daquela turma
    // Sem turma: XP total (atividades + desafios + conquistas) dos alunos que o usuário pode ver
    // Empate: fica à frente quem concluiu mais atividades
    @Transactional(readOnly = true)
    public List<RankingResponse> buscarRanking(Long turmaId, String username) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        List<RankingResponse> ranking = turmaId == null
                ? rankingGeral(participantesVisiveis(usuario))
                : rankingDaTurma(carregarTurmaPermitida(turmaId, usuario));

        return TipoUsuario.ehAluno(usuario) ? ocultarNomes(ranking, usuario.getId()) : ranking;
    }

    // Ranking de todos os alunos, só para uso interno (posição usada nas conquistas de ranking)
    @Transactional(readOnly = true)
    public List<RankingResponse> buscarRankingGeral() {
        return rankingGeral(usuarioRepository.findAllByTipoUsuarioIgnoreCase(TipoUsuario.ALUNO));
    }

    private List<RankingResponse> rankingGeral(Collection<Usuario> alunos) {
        return montar(
                alunos,
                porAluno(resultadoRepository.somarPorAluno()),
                porAluno(respostaDesafioRepository.somarPorAluno()),
                porAluno(usuarioConquistaRepository.somarPorAluno()));
    }

    // O XP das conquistas fica de fora porque não pertence a nenhuma turma
    private List<RankingResponse> rankingDaTurma(Turma turma) {
        return montar(
                turma.getAlunos(),
                porAluno(resultadoRepository.somarPorAlunoNaTurma(turma.getId())),
                porAluno(respostaDesafioRepository.somarPorAlunoNaTurma(turma.getId())),
                Map.of());
    }

    private static List<RankingResponse> montar(
            Collection<Usuario> alunos,
            Map<Long, TotalPorAluno> atividades,
            Map<Long, TotalPorAluno> desafios,
            Map<Long, TotalPorAluno> conquistas) {

        return alunos.stream()
                .filter(TipoUsuario::ehAluno)
                .map(aluno -> new RankingResponse(
                        aluno.getId(),
                        aluno.getNomeUsuario(),
                        pontos(atividades, aluno.getId()) + pontos(desafios, aluno.getId())
                                + pontos(conquistas, aluno.getId()),
                        quantidade(atividades, aluno.getId()),
                        aluno.getAvatar() != null ? aluno.getAvatar().name() : null
                ))
                // Só entra quem já fez alguma coisa
                .filter(r -> r.pontuacaoTotal() > 0 || r.atividadeConcluidas() > 0)
                .sorted(Comparator.comparing(RankingResponse::pontuacaoTotal).reversed()
                        .thenComparing(Comparator.comparing(RankingResponse::atividadeConcluidas).reversed())
                        .thenComparing(RankingResponse::usuarioId))
                .toList();
    }

    // Administrador: qualquer turma; professor: só as dele; aluno: só as que estuda
    private Turma carregarTurmaPermitida(Long turmaId, Usuario usuario) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "TURMA_NAO_ENCONTRADA", "Turma não encontrada."));

        boolean permitido;
        if (TipoUsuario.ehAdministrador(usuario)) {
            permitido = true;
        } else if (TipoUsuario.ehProfessor(usuario)) {
            permitido = turma.getProfessor() != null && turma.getProfessor().getId().equals(usuario.getId());
        } else {
            permitido = turmaRepository.contarMatricula(turma.getId(), usuario.getId()) > 0;
        }

        if (!permitido) {
            throw ApiException.proibido("SEM_ACESSO_A_TURMA", "Você não tem acesso ao ranking desta turma.");
        }

        return turma;
    }

    // Administrador: todos os alunos; professor: alunos das turmas dele; aluno: colegas das turmas dele
    private Collection<Usuario> participantesVisiveis(Usuario usuario) {
        if (TipoUsuario.ehAdministrador(usuario)) {
            return usuarioRepository.findAllByTipoUsuarioIgnoreCase(TipoUsuario.ALUNO);
        }

        List<Turma> turmas = TipoUsuario.ehProfessor(usuario)
                ? turmaRepository.findAllByProfessor_Id(usuario.getId())
                : turmaRepository.findAllByAlunos_Id(usuario.getId());

        // Quem está em mais de uma turma entra uma vez só
        Map<Long, Usuario> alunos = new LinkedHashMap<>();
        for (Turma turma : turmas) {
            turma.getAlunos().forEach(aluno -> alunos.putIfAbsent(aluno.getId(), aluno));
        }
        return alunos.values();
    }

    // O aluno vê o nome dos primeiros colocados e o próprio; dos outros, só a posição e os pontos
    static List<RankingResponse> ocultarNomes(List<RankingResponse> ranking, Long alunoId) {
        List<RankingResponse> resultado = new ArrayList<>(ranking.size());

        for (int i = 0; i < ranking.size(); i++) {
            RankingResponse r = ranking.get(i);
            boolean visivel = i < NOMES_VISIVEIS_PARA_ALUNO || r.usuarioId().equals(alunoId);
            resultado.add(visivel
                    ? r
                    : new RankingResponse(null, NOME_OCULTO, r.pontuacaoTotal(), r.atividadeConcluidas(), null));
        }

        return resultado;
    }

    private static Map<Long, TotalPorAluno> porAluno(List<TotalPorAluno> totais) {
        return totais.stream().collect(Collectors.toMap(TotalPorAluno::usuarioId, t -> t));
    }

    private static long pontos(Map<Long, TotalPorAluno> totais, Long usuarioId) {
        TotalPorAluno t = totais.get(usuarioId);
        return t == null || t.pontos() == null ? 0 : t.pontos();
    }

    private static long quantidade(Map<Long, TotalPorAluno> totais, Long usuarioId) {
        TotalPorAluno t = totais.get(usuarioId);
        return t == null || t.quantidade() == null ? 0 : t.quantidade();
    }
}
