package com.umc.menteup.util;

import com.umc.menteup.model.Codigo2FA;
import com.umc.menteup.model.Codigo2FARequest;
import com.umc.menteup.model.UsuarioLogin;
import com.umc.menteup.repository.Codigo2FARepository;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class JwtHelper {

    private JwtHelper() {
    }

    public static String obterToken(TestRestTemplate rest,
                                    Codigo2FARepository codigoRepository,
                                    String email,
                                    String senha) {

        // Passo 1: usuário e senha (a aplicação grava o código 2FA no banco)
        HttpEntity<UsuarioLogin> loginRequest =
                new HttpEntity<>(TestBuilder.criarUsuarioLogin(email, senha));

        ResponseEntity<UsuarioLogin> loginResposta = rest.exchange(
                "/usuarios/logar", HttpMethod.POST, loginRequest, UsuarioLogin.class);

        if (loginResposta.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Falha no login: " + email);
        }

        // Passo 2: lê o código gerado e valida o 2FA
        Codigo2FA codigo = codigoRepository
                .findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(email)
                .orElseThrow(() -> new RuntimeException("Código 2FA não encontrado para: " + email));

        Codigo2FARequest dois = new Codigo2FARequest();
        dois.setUsuario(email);
        dois.setCodigo(codigo.getCodigo());

        ResponseEntity<UsuarioLogin> resposta = rest.exchange(
                "/usuarios/validar-2fa", HttpMethod.POST, new HttpEntity<>(dois), UsuarioLogin.class);

        UsuarioLogin body = resposta.getBody();
        if (body != null && body.getToken() != null) {
            return body.getToken();
        }

        throw new RuntimeException("Falha na validação do 2FA: " + email);
    }

    public static <T> HttpEntity<T> criarRequisicaoComToken(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        String tokenLimpo = token.startsWith("Bearer ") ? token.substring(7) : token;
        headers.setBearerAuth(tokenLimpo);
        return new HttpEntity<>(body, headers);
    }

    public static HttpEntity<Void> criarRequisicaoComToken(String token) {
        return criarRequisicaoComToken(null, token);
    }
}
