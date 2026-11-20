package com.umc.menteup.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.umc.menteup.dto.AlunoNaSalaResponse;
import com.umc.menteup.model.Usuario;

/*
  - Sala de espera do desafio: quem está com a tela do desafio aberta.

  - A tela do aluno consulta o estado do desafio a cada segundo, cada consulta conta como "estou na sala".
  - Quem não dá sinal há mais de 10 segundos (fechou a aba, perdeu a conexão) sai da lista.
  - Fica em memória, sem gravar no banco: é uma informação do momento. Se o servidor reiniciar,
     a lista se refaz sozinha no segundo seguinte, com as novas consultas dos alunos.
 */
@Component
public class PresencaDesafio {

    static final Duration TEMPO_SEM_SINAL = Duration.ofSeconds(10);

    private record Sinal(Long id, String nome, String avatar, Instant em) {
    }

    private final Map<Long, Map<Long, Sinal>> salas = new ConcurrentHashMap<>();
    private final Clock clock;

    public PresencaDesafio(Clock clock) {
        this.clock = clock;
    }

    /// O aluno está com o desafio aberto agora
    public void registrar(Long desafioId, Usuario aluno) {
        salas.computeIfAbsent(desafioId, id -> new ConcurrentHashMap<>())
                .put(aluno.getId(), new Sinal(
                        aluno.getId(),
                        aluno.getNomeUsuario(),
                        aluno.getAvatar() == null ? null : aluno.getAvatar().name(),
                        clock.instant()));
    }

    /// Alunos que deram sinal nos últimos 10 segundos, em ordem alfabética
    public List<AlunoNaSalaResponse> naSala(Long desafioId) {
        Map<Long, Sinal> sala = salas.get(desafioId);
        if (sala == null) {
            return List.of();
        }

        Instant limite = clock.instant().minus(TEMPO_SEM_SINAL);
        // Tira quem saiu, para a lista não crescer sem parar
        sala.values().removeIf(sinal -> sinal.em().isBefore(limite));

        return sala.values().stream()
                .sorted(Comparator.comparing(Sinal::nome, String.CASE_INSENSITIVE_ORDER))
                .map(sinal -> new AlunoNaSalaResponse(sinal.id(), sinal.nome(), sinal.avatar()))
                .toList();
    }

    /// Desafio encerrado: a sala não é mais necessária
    public void fechar(Long desafioId) {
        salas.remove(desafioId);
    }
}
