package com.umc.menteup.controller;

import com.umc.menteup.dto.*;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.service.AtividadeService;
import com.umc.menteup.service.LogSistemaService;
import com.umc.menteup.service.ProgressoAtividadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AtividadeController {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private ProgressoAtividadeService progressoAtividadeService;

    @Autowired
    private LogSistemaService logSistemaService;

    @GetMapping
    public ResponseEntity<List<Atividade>> getAll() {
        return ResponseEntity.ok(atividadeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionarioResponse> getById(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                atividadeService.buscarQuestionario(
                        id,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<List<Atividade>> getByTitulo(
            @PathVariable String titulo) {

        return ResponseEntity.ok(
                atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)
        );
    }

    @PostMapping
    public ResponseEntity<Atividade> post(
            @Valid @RequestBody Atividade atividade,
            Authentication authentication) {

        if (turmaRepository.existsById(atividade.getTurma().getId())) {

            atividade.setId(null);

            Atividade salva = atividadeRepository.save(atividade);

            logSistemaService.registrarPorUsername(
                    authentication.getName(),
                    AcaoLog.CRIAR_ATIVIDADE,
                    EntidadeLog.ATIVIDADE,
                    salva.getId(),
                    ResultadoLog.SUCESSO,
                    "Atividade \"" + salva.getTitulo() + "\" criada",
                    Map.of(
                            "titulo", salva.getTitulo(),
                            "turmaId", salva.getTurma().getId(),
                            "pontuacao", salva.getPontuacao()
                    )
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(salva);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Turma não existe!",
                null
        );
    }

    @PutMapping
    public ResponseEntity<Atividade> put(
            @Valid @RequestBody Atividade atividade,
            Authentication authentication) {

        Atividade existente = atividadeRepository.findById(atividade.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!turmaRepository.existsById(atividade.getTurma().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Turma não existe",
                    null
            );
        }

        StatusAtividade statusAnterior = existente.getStatus();

        atividade.setPerguntas(existente.getPerguntas());

        Atividade salva = atividadeRepository.save(atividade);

        boolean foiPublicada =
                statusAnterior == StatusAtividade.PENDENTE
                        && salva.getStatus() == StatusAtividade.EM_ANDAMENTO;

        if (foiPublicada) {

            logSistemaService.registrarPorUsername(
                    authentication.getName(),
                    AcaoLog.PUBLICAR_ATIVIDADE,
                    EntidadeLog.ATIVIDADE,
                    salva.getId(),
                    ResultadoLog.SUCESSO,
                    "Atividade \"" + salva.getTitulo() + "\" publicada.",
                    Map.of(
                            "titulo", salva.getTitulo()
                    )
            );

        } else {

            logSistemaService.registrarPorUsername(
                    authentication.getName(),
                    AcaoLog.EDITAR_ATIVIDADE,
                    EntidadeLog.ATIVIDADE,
                    salva.getId(),
                    ResultadoLog.SUCESSO,
                    "Atividade \"" + salva.getTitulo() + "\" editada.",
                    Map.of(
                            "tituloAnterior", existente.getTitulo(),
                            "tituloNovo", salva.getTitulo(),
                            "statusAnterior", statusAnterior.name(),
                            "statusNovo", salva.getStatus().name()
                    )
            );
        }

        return ResponseEntity.ok(salva);
    }

    @PutMapping("/{id}/progresso")
    public ResponseEntity<ProgressoAtividadeResponse> salvarProgresso(
            @PathVariable Long id,
            @Valid @RequestBody SalvarRespostaRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                progressoAtividadeService.salvarResposta(
                        id,
                        authentication.getName(),
                        request
                )
        );
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        atividadeRepository.deleteById(id);

        logSistemaService.registrarPorUsername(
                authentication.getName(),
                AcaoLog.EXCLUIR_ATIVIDADE,
                EntidadeLog.ATIVIDADE,
                id,
                ResultadoLog.SUCESSO,
                "Atividade \"" + atividade.getTitulo() + "\" excluída.",
                Map.of(
                        "titulo", atividade.getTitulo()
                )
        );
    }
}