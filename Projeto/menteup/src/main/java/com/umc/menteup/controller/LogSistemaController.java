package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.LogSistemaResponse;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.LogSistema;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.service.LogSistemaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/logs")
@Tag(name = "Logs do sistema", description = "Histórico de ações (somente administrador)")
public class LogSistemaController {
    private final LogSistemaRepository logSistemaRepository;
    private final LogSistemaService logSistemaService;

    public LogSistemaController(LogSistemaRepository logSistemaRepository, LogSistemaService logSistemaService) {
        this.logSistemaRepository = logSistemaRepository;
        this.logSistemaService = logSistemaService;
    }

    @Operation(summary = "Lista os logs com filtros")
    @GetMapping
    public ResponseEntity<Page<LogSistemaResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoLog acao,
            @RequestParam(required = false) EntidadeLog entidade,
            @RequestParam(required = false) ResultadoLog resultado,
            @PageableDefault(size = 50, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Page<LogSistema> pagina = logSistemaRepository. buscar(
                usuarioId,
                acao,
                entidade,
                resultado,
                pageable);
        registrarConsulta(authentication, pageable, usuarioId, acao, entidade, resultado);
        return  ResponseEntity.ok(pagina.map(this::paraResponse));
    }

    // Acesso administrativo aos logs fica registrado (só a primeira página, para não repetir a cada rolagem)
    private void registrarConsulta(Authentication authentication, Pageable pageable, Long usuarioId,
                                   AcaoLog acao, EntidadeLog entidade, ResultadoLog resultado) {
        if (pageable.getPageNumber() > 0) {
            return;
        }

        Map<String, Object> filtros = new LinkedHashMap<>();
        if (usuarioId != null) {
            filtros.put("usuarioId", usuarioId);
        }
        if (acao != null) {
            filtros.put("acao", acao.name());
        }
        if (entidade != null) {
            filtros.put("entidade", entidade.name());
        }
        if (resultado != null) {
            filtros.put("resultado", resultado.name());
        }

        logSistemaService.registrarPorUsername(authentication.getName(), AcaoLog.CONSULTAR_LOGS, null, null,
                ResultadoLog.SUCESSO, "Consulta aos logs do sistema.", filtros.isEmpty() ? null : filtros);
    }

    private LogSistemaResponse paraResponse (LogSistema entrada) {
        return new LogSistemaResponse(
                entrada.getId(),
                entrada.getUsuario() == null ? null : entrada.getUsuario().getId(),
                entrada.getUsuario() == null ? null : entrada.getUsuario().getNomeUsuario(),
                entrada.getAcao(),
                entrada.getEntidade(),
                entrada.getEntidadeId(),
                entrada.getDataHora(),
                entrada.getResultado(),
                entrada.getDescricao(),
                entrada.getIp(),
                entrada.getDados()
        );
    }
}
