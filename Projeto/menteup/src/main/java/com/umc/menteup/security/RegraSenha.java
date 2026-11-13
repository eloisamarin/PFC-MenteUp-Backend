package com.umc.menteup.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.umc.menteup.exception.ApiException;

/*
 * Regras de toda senha NOVA escolhida pela pessoa (cadastro, troca no "Meu perfil",
 * "Esqueci minha senha" e primeiro acesso). Senhas que já existem não são afetadas.
 *
 * - RF01: mínimo de 8 caracteres (também no @Size dos DTOs);
 * - RF03 a RF06: pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial;
 * - RF09: sem espaço no início ou no fim;
 * - limite de 72 BYTES: o BCrypt ignora o que passa disso. Letras com acento ocupam 2 bytes,
 *   então o @Size(max = 72), que conta caracteres, não basta.
 *
 * O frontend mostra as mesmas regras (src/utils/senha.ts); aqui é a garantia de verdade,
 * que vale mesmo para quem chama a API sem passar pela tela.
 */
public final class RegraSenha {

    public static final int MINIMO_CARACTERES = 8;
    public static final int MAXIMO_BYTES = 72;

    private RegraSenha() {
    }

    public static void validarNovaSenha(String senha) {
        if (senha == null || senha.isEmpty()) {
            throw ApiException.requisicaoInvalida("SENHA_FRACA", "Informe a senha.");
        }

        exigirTamanhoEmBytes(senha);

        boolean espacoNoInicio = Character.isWhitespace(senha.charAt(0));
        boolean espacoNoFim = Character.isWhitespace(senha.charAt(senha.length() - 1));
        if (espacoNoInicio || espacoNoFim) {
            throw ApiException.requisicaoInvalida("SENHA_COM_ESPACO",
                    "A senha não pode começar nem terminar com espaço.");
        }

        List<String> faltando = new ArrayList<>();
        if (senha.length() < MINIMO_CARACTERES) {
            faltando.add("no mínimo " + MINIMO_CARACTERES + " caracteres");
        }
        if (senha.chars().noneMatch(Character::isUpperCase)) {
            faltando.add("uma letra maiúscula");
        }
        if (senha.chars().noneMatch(Character::isLowerCase)) {
            faltando.add("uma letra minúscula");
        }
        if (senha.chars().noneMatch(Character::isDigit)) {
            faltando.add("um número");
        }
        if (senha.chars().noneMatch(RegraSenha::ehCaractereEspecial)) {
            faltando.add("um caractere especial (ex.: @ # $ % !)");
        }

        if (!faltando.isEmpty()) {
            throw ApiException.requisicaoInvalida("SENHA_FRACA",
                    "A senha precisa ter " + String.join(", ", faltando) + ".");
        }
    }

    public static void exigirTamanhoEmBytes(String senha) {
        if (senha != null && senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw ApiException.requisicaoInvalida("SENHA_LONGA_DEMAIS",
                    "A senha é longa demais. Use até 72 caracteres "
                            + "(letras com acento contam como 2).");
        }
    }

    // Qualquer símbolo conta (@ # $ % ! * & ? _ - ...): não é letra, número nem espaço
    private static boolean ehCaractereEspecial(int c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }
}
