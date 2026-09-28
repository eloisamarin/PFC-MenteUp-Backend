package com.umc.menteup.exception;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.service.LogSistemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LogSistemaService logSistemaService;

    public GlobalExceptionHandler(LogSistemaService logSistemaService) {
        this.logSistemaService = logSistemaService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApi(ApiException ex) {
        return montar(ex.getStatus(), ex.getCodigo(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        return montar(HttpStatus.BAD_REQUEST, "VALIDACAO_FALHOU",
                "Existem campos inválidos na requisição.", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "Corpo da requisição ausente, malformado ou com tipos inválidos.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados: {}", ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS",
                "A operação conflita com dados já existentes ou vinculados.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AccessDeniedException ex) {
        return montar(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Você não tem permissão para realizar esta operação.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex) {
        if (ex instanceof ErrorResponse erroSpring) {
            HttpStatusCode status = erroSpring.getStatusCode();
            String detalhe = erroSpring.getBody().getDetail();
            HttpStatus conhecido = HttpStatus.resolve(status.value());

            return montar(
                    status,
                    conhecido != null ? conhecido.name() : "ERRO_HTTP_" + status.value(),
                    detalhe == null || detalhe.isBlank()
                            ? (conhecido != null ? conhecido.getReasonPhrase() : "Erro na requisição.")
                            : detalhe,
                    null
            );
        }

        log.error("Erro inesperado", ex);

        logSistemaService.registrarPorUsername(
                usernameAutenticadoOuNull(),
                AcaoLog.ERRO_SISTEMA,
                null,
                null,
                ResultadoLog.FALHA,
                "Erro inesperado: " + ex.getClass().getSimpleName() + (ex.getMessage() != null ? " - " + ex.getMessage() : ""),
                Map.of("excecao", ex.getClass().getName())
        );

        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", null);
    }

    private String usernameAutenticadoOuNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            return authentication.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<ErroResponse> montar(
            HttpStatusCode status, String codigo, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(new ErroResponse(codigo, mensagem, campos));
    }
}
