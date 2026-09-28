package com.umc.menteup.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class LogSistemaService {
    private static final Logger log = LoggerFactory.getLogger(LogSistemaService.class);

    private final LogSistemaRepository logSistemaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public LogSistemaService(
            LogSistemaRepository logSistemaRepository,
            UsuarioRepository usuarioRepository,
            ObjectMapper objectMapper
    ) {
        this.logSistemaRepository = logSistemaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
            Usuario usuario,
            AcaoLog acao,
            EntidadeLog entidade,
            Long entidadeId,
            ResultadoLog resultado,
            String descricao,
            Object dados
    ) {
        try {
            LogSistema entrada = new LogSistema();
            entrada.setUsuario(usuario);
            entrada.setAcao(acao);
            entrada.setEntidade(entidade);
            entrada.setEntidadeId(entidadeId);
            entrada.setResultado(resultado);
            entrada.setDescricao(truncar(descricao, 500));
            entrada.setIp(truncar(obterIpRequisicaoAtual(), 45));
            entrada.setDados(serializar(dados));
            logSistemaRepository.save(entrada);
        } catch (Exception e) {
            log.warn("Falha ao registrar log de sistema (acao= {}): {}", acao, e.getMessage());
        }
    }

    public void registrarSucesso(
            Usuario usuario,
            AcaoLog acao,
            EntidadeLog entidade,
            Long entidadeId,
            String descricao,
            Object dados
    ) {
        registrar(
                usuario,
                acao,
                entidade,
                entidadeId,
                ResultadoLog.SUCESSO,
                descricao,
                dados
        );
    }

    public void registrarFalha(
            Usuario usuario,
            AcaoLog acao,
            EntidadeLog entidade,
            Long entidadeId,
            String descricao,
            Object dados
    ) {
        registrar(
                usuario,
                acao,
                entidade,
                entidadeId,
                ResultadoLog.FALHA,
                descricao,
                dados
        );
    }

    public void registrarPorUsername(
            String username,
            AcaoLog acao,
            EntidadeLog entidade,
            Long entidadeId,
            ResultadoLog resultado,
            String descricao,
            Object dados
    ) {
        Usuario usuario = (username == null || username.isBlank())
                ? null
                : usuarioRepository.findByUsuario(username).orElse(null);
        registrar(
                usuario,
                acao,
                entidade,
                entidadeId,
                resultado,
                descricao,
                dados
        );
    }

    private String serializar(Object dados) {
        if (dados == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JacksonException e) {
            log.warn("Não foi possível serializar 'dados' do log de sistema: {}", e.getMessage());
            return null;
        }
    }

    private String truncar(String valor, int tamanhoMaximo) {
        if (valor == null || valor.length() <= tamanhoMaximo) {
            return valor;
        }
        return valor.substring(0, tamanhoMaximo);
    }

    private String obterIpRequisicaoAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String encaminhadoPor = request.getHeader("X-Forwarded-For");
            if (encaminhadoPor != null && !encaminhadoPor.isBlank()) {
                return encaminhadoPor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
