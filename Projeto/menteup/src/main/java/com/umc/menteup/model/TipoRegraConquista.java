package com.umc.menteup.model;

///Tipos de regra das conquistas. Em todos, o valor a alcançar é Conquista.meta
///Conquista nova de um tipo que já existe: basta cadastrar no banco
public enum TipoRegraConquista {

    ///XP total do aluno (atividades + desafios + conquistas)
    XP_ACUMULADO,

    ///Atividades concluídas
    CONTAGEM_ATIVIDADES,

    ///Atividades com todas as questões certas
    APROVEITAMENTO,

    ///Dias seguidos entrando na plataforma
    SEQUENCIA_DIAS,

    ///Posições subidas no ranking
    EVOLUCAO_RANKING,

    ///Chegar até a posição da meta no ranking (1 = primeiro lugar)
    POSICAO_RANKING
}
