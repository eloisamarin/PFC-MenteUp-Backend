package com.umc.menteup.security;

import java.nio.charset.StandardCharsets;

import com.umc.menteup.exception.ApiException;

/*
 * Limite da senha em BYTES (e não em caracteres).
 * O BCrypt só usa os primeiros 72 bytes: o resto é ignorado em silêncio. Letras com acento
 * (á, ç, ã...) ocupam 2 bytes em UTF-8, então uma senha de 72 caracteres pode passar de 72 bytes,
 * e o @Size(max = 72) dos DTOs, que conta caracteres, não pega esse caso.
 * Chamado antes de todo passwordEncoder.encode(...) de senha escolhida pela pessoa.
 */
public final class RegraSenha {

    public static final int MAXIMO_BYTES = 72;

    private RegraSenha() {
    }

    public static void exigirTamanhoEmBytes(String senha) {
        if (senha != null && senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw ApiException.requisicaoInvalida("SENHA_LONGA_DEMAIS",
                    "A senha é longa demais. Use até 72 caracteres "
                            + "(letras com acento contam como 2).");
        }
    }
}
