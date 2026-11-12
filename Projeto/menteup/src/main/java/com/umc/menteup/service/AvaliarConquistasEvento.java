package com.umc.menteup.service;

import java.util.Collection;
import java.util.List;

///Aviso de que as conquistas destes alunos precisam ser verificadas
public record AvaliarConquistasEvento(List<Long> usuarioIds) {

    public static AvaliarConquistasEvento de(Long usuarioId) {
        return new AvaliarConquistasEvento(List.of(usuarioId));
    }

    public static AvaliarConquistasEvento de(Collection<Long> usuarioIds) {
        return new AvaliarConquistasEvento(List.copyOf(usuarioIds));
    }
}
