package com.umc.menteup.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.umc.menteup.exception.ApiException;

/*
 * Regras de senha nova (RF01, RF03 a RF06, RF09 e o limite de 72 bytes do BCrypt).
 * Teste unitário: não sobe o Spring.
 */
class RegraSenhaTest {

    @ParameterizedTest
    @ValueSource(strings = {"Senha@123", "Aa1!aaaa", "Minha Senha#9", "Ç@olá1ÁÉ", "X9_yyyyy", "Abc-1234"})
    void senhaForte_deveSerAceita(String senha) {
        assertThatCode(() -> RegraSenha.validarNovaSenha(senha)).doesNotThrowAnyException();
    }

    @Test
    void semMaiuscula_deveDizerQueFaltaMaiuscula() {
        assertFraca("senha@123", "uma letra maiúscula");
    }

    @Test
    void semMinuscula_deveDizerQueFaltaMinuscula() {
        assertFraca("SENHA@123", "uma letra minúscula");
    }

    @Test
    void semNumero_deveDizerQueFaltaNumero() {
        assertFraca("Senha@abc", "um número");
    }

    @Test
    void semCaractereEspecial_deveDizerQueFaltaEspecial() {
        assertFraca("Senha1234", "um caractere especial");
    }

    @Test
    void curta_deveDizerOMinimo() {
        assertFraca("Aa1!", "no mínimo 8 caracteres");
    }

    @Test
    void variasRegrasFaltando_deveListarTodasNaMesmaMensagem() {
        ApiException erro = fraca("abcdefgh");

        assertThat(erro.getMessage())
                .contains("uma letra maiúscula", "um número", "um caractere especial")
                .doesNotContain("minúscula");
    }

    @ParameterizedTest
    @ValueSource(strings = {" Senha@123", "Senha@123 ", "\tSenha@123"})
    void espacoNoInicioOuNoFim_deveSerRecusado(String senha) {
        assertThatThrownBy(() -> RegraSenha.validarNovaSenha(senha))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SENHA_COM_ESPACO"));
    }

    @Test
    void acimaDe72Bytes_deveSerRecusadaPeloTamanho() {
        String senha = "Aa1!" + "ç".repeat(35); // 39 caracteres, 74 bytes

        assertThatThrownBy(() -> RegraSenha.validarNovaSenha(senha))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SENHA_LONGA_DEMAIS"));
    }

    @Test
    void vazia_deveSerRecusada() {
        assertThat(fraca("").getCodigo()).isEqualTo("SENHA_FRACA");
    }

    // ------------------------------------------------------------- Helpers

    private static void assertFraca(String senha, String trechoDaMensagem) {
        ApiException erro = fraca(senha);
        assertThat(erro.getCodigo()).isEqualTo("SENHA_FRACA");
        assertThat(erro.getMessage()).contains(trechoDaMensagem);
    }

    private static ApiException fraca(String senha) {
        try {
            RegraSenha.validarNovaSenha(senha);
        } catch (ApiException e) {
            return e;
        }
        throw new AssertionError("A senha \"" + senha + "\" deveria ser recusada");
    }
}
