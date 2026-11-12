package com.umc.menteup.controller;

import com.umc.menteup.dto.ConclusaoAtividadeRequest;
import com.umc.menteup.dto.ResultadoAtividadeResponse;
import com.umc.menteup.model.ResultadoAtividade;
import com.umc.menteup.service.ResultadoAtividadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ResultadoAtividadeController {
    private final ResultadoAtividadeService resultadoAtividadeService;

    public ResultadoAtividadeController(
            ResultadoAtividadeService resultadoAtividadeService
    ){
        this.resultadoAtividadeService = resultadoAtividadeService;
    }

    @PostMapping("/{atividadeId}/respostas")
    public ResponseEntity<ResultadoAtividadeResponse> responderAtividade(
            @PathVariable Long atividadeId,
            Authentication authentication
    ) {
        ResultadoAtividadeResponse response =
                resultadoAtividadeService.concluirAtividade(
                        atividadeId,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
