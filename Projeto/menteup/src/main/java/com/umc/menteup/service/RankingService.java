package com.umc.menteup.service;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.dto.TotalPorAluno;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.repository.RespostaDesafioRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.UsuarioConquistaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RankingService {
    private final ResultadoAtividadeRepository resultadoRepository;
    private final RespostaDesafioRepository respostaDesafioRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final UsuarioRepository usuarioRepository;

    public RankingService(
            ResultadoAtividadeRepository resultadoRepository,
            RespostaDesafioRepository respostaDesafioRepository,
            UsuarioConquistaRepository usuarioConquistaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.resultadoRepository = resultadoRepository;
        this.respostaDesafioRepository = respostaDesafioRepository;
        this.usuarioConquistaRepository = usuarioConquistaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    ///Ranking geral pelo XP: atividades + desafios + bônus de conquistas
    ///Empate: fica à frente quem concluiu mais atividades
    @Transactional(readOnly = true)
    public List<RankingResponse> buscarRanking() {
        Map<Long, TotalPorAluno> atividades = porAluno(resultadoRepository.somarPorAluno());
        Map<Long, TotalPorAluno> desafios = porAluno(respostaDesafioRepository.somarPorAluno());
        Map<Long, TotalPorAluno> conquistas = porAluno(usuarioConquistaRepository.somarPorAluno());

        return usuarioRepository.findAllByTipoUsuarioIgnoreCase(TipoUsuario.ALUNO).stream()
                .map(aluno -> new RankingResponse(
                        aluno.getId(),
                        aluno.getNomeUsuario(),
                        pontos(atividades, aluno.getId()) + pontos(desafios, aluno.getId()) + pontos(conquistas, aluno.getId()),
                        quantidade(atividades, aluno.getId())
                ))
                ///Só entra quem já fez alguma coisa
                .filter(r -> r.pontuacaoTotal() > 0 || r.atividadeConcluidas() > 0)
                .sorted(Comparator.comparing(RankingResponse::pontuacaoTotal).reversed()
                        .thenComparing(Comparator.comparing(RankingResponse::atividadeConcluidas).reversed())
                        .thenComparing(RankingResponse::usuarioId))
                .toList();
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
