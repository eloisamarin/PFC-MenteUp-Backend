package com.umc.menteup.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Conta quantas vezes a mesma chave (IP, e-mail...) fez um pedido dentro de uma janela de tempo
// Fica em memória: ao reiniciar a aplicação, as contagens recomeçam
public class LimitadorDeTentativas {

    private final int maximo;
    private final Duration janela;
    private final Clock clock;
    private final Map<String, Deque<Instant>> pedidos = new ConcurrentHashMap<>();

    public LimitadorDeTentativas(int maximo, Duration janela, Clock clock) {
        this.maximo = maximo;
        this.janela = janela;
        this.clock = clock;
    }

    // Devolve true e conta o pedido se ainda estiver dentro do limite
    public boolean tentar(String chave) {
        Instant agora = clock.instant();
        Instant inicioDaJanela = agora.minus(janela);
        limparChavesAntigas(inicioDaJanela);

        Deque<Instant> daChave = pedidos.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (daChave) {
            while (!daChave.isEmpty() && !daChave.peekFirst().isAfter(inicioDaJanela)) {
                daChave.pollFirst();
            }
            if (daChave.size() >= maximo) {
                return false;
            }
            daChave.addLast(agora);
            return true;
        }
    }

    // Tira da memória as chaves sem pedidos recentes, para a lista não crescer sem parar
    private void limparChavesAntigas(Instant inicioDaJanela) {
        pedidos.entrySet().removeIf(entrada -> {
            Deque<Instant> daChave = entrada.getValue();
            synchronized (daChave) {
                Instant ultimo = daChave.peekLast();
                return ultimo == null || !ultimo.isAfter(inicioDaJanela);
            }
        });
    }
}
