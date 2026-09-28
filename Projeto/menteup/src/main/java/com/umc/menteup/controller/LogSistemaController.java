package com.umc.menteup.controller;

import com.umc.menteup.dto.LogSistemaResponse;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.LogSistema;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.repository.LogSistemaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LogSistemaController {
    private final LogSistemaRepository logSistemaRepository;

    public LogSistemaController(LogSistemaRepository logSistemaRepository) {
        this.logSistemaRepository = logSistemaRepository;
    }

    @GetMapping
    public ResponseEntity<Page<LogSistemaResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) AcaoLog acao,
            @RequestParam(required = false) EntidadeLog entidade,
            @RequestParam(required = false) ResultadoLog resultado,
            @PageableDefault(size = 50, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<LogSistema> pagina = logSistemaRepository. buscar(
                usuarioId,
                acao,
                entidade,
                resultado,
                pageable);
        return  ResponseEntity.ok(pagina.map(this::paraResponse));
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
