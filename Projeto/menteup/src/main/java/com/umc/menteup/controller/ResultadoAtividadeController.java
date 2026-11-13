package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.ResultadoAtividadeResponse;
import com.umc.menteup.service.ResultadoAtividadeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Atividades", description = "Criação, questionário, progresso, respostas e exclusão de atividades")
public class ResultadoAtividadeController {
    private final ResultadoAtividadeService resultadoAtividadeService;

    public ResultadoAtividadeController(
            ResultadoAtividadeService resultadoAtividadeService
    ){
        this.resultadoAtividadeService = resultadoAtividadeService;
    }

    @Operation(summary = "Conclui a atividade e calcula o resultado")
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
