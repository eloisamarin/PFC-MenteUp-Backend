package com.umc.menteup.model;

///Formato da atividade que será escolhida pelo professor
public enum TipoAtividade {
    QUIZ,
    VERDADEIRO_FALSO,
    ASSOCIACAO,
    ORDENACAO,
    LACUNAS;

    ///Atividades que já estão criadas são consideradas como QUIZ
    public static TipoAtividade ouPadrao(TipoAtividade tipo) {
        return tipo == null ? QUIZ : tipo;
    }
}
