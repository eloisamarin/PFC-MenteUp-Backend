package com.umc.menteup.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio com status HTTP e código estável para o front.
 * É convertido em { "codigo": ..., "mensagem": ... } pelo GlobalExceptionHandler.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public static ApiException requisicaoInvalida(String codigo, String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }

    public static ApiException naoAutenticado(String codigo, String mensagem) {
        return new ApiException(HttpStatus.UNAUTHORIZED, codigo, mensagem);
    }

    public static ApiException proibido(String codigo, String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, codigo, mensagem);
    }

    public static ApiException naoEncontrado(String codigo, String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, codigo, mensagem);
    }

    public static ApiException conflito(String codigo, String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, codigo, mensagem);
    }
}
