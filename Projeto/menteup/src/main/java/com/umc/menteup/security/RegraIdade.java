package com.umc.menteup.security;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Usuario;

import java.time.LocalDate;

// O MenteUp é para alunos com 12 anos ou mais (pela LGPD, abaixo disso seria preciso o consentimento
// de um responsável). A data de nascimento só serve para conferir a idade: ela não é guardada,
// fica gravado apenas quando a idade mínima foi confirmada
// Vale só para alunos: professor e administrador são cadastrados como adultos
public final class RegraIdade {

    public static final int IDADE_MINIMA = 12;

    // Acima disso, a data foi digitada errada
    private static final int IDADE_MAXIMA_POSSIVEL = 120;

    private RegraIdade() {
    }

    public static void exigirIdadeMinima(LocalDate nascimento, LocalDate hoje) {
        exigirDataValida(nascimento, hoje);
        if (!temIdadeMinima(nascimento, hoje)) {
            throw ApiException.requisicaoInvalida("IDADE_MINIMA",
                    "O MenteUp é para pessoas com " + IDADE_MINIMA + " anos ou mais.");
        }
    }

    public static void exigirDataValida(LocalDate nascimento, LocalDate hoje) {
        if (nascimento == null) {
            throw ApiException.requisicaoInvalida("DATA_NASCIMENTO_OBRIGATORIA",
                    "Informe a data de nascimento.");
        }
        if (nascimento.isAfter(hoje) || nascimento.isBefore(hoje.minusYears(IDADE_MAXIMA_POSSIVEL))) {
            throw ApiException.requisicaoInvalida("DATA_NASCIMENTO_INVALIDA",
                    "Confira a data de nascimento.");
        }
    }

    public static boolean temIdadeMinima(LocalDate nascimento, LocalDate hoje) {
        return !nascimento.plusYears(IDADE_MINIMA).isAfter(hoje);
    }

    // Aluno que ainda não confirmou a idade (contas criadas antes desta regra)
    public static boolean precisaConfirmar(Usuario usuario) {
        return TipoUsuario.ehAluno(usuario) && usuario.getIdadeMinimaConfirmadaEm() == null;
    }
}
