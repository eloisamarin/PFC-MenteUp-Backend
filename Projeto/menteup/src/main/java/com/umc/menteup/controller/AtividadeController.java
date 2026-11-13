package com.umc.menteup.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.umc.menteup.dto.*;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.service.AtividadeService;
import com.umc.menteup.service.LogSistemaService;
import com.umc.menteup.service.ProgressoAtividadeService;
import com.umc.menteup.service.TurmaService;
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
@Tag(name = "Atividades", description = "Criação, questionário, progresso, respostas e exclusão de atividades")
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

    @Operation(summary = "Lista todas as atividades")
    @GetMapping
    public ResponseEntity<List<Atividade>> getAll() {
        return ResponseEntity.ok(atividadeRepository.findAll());
    }

    @Operation(summary = "Busca a atividade com as perguntas")
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

    @Operation(summary = "Busca atividades pelo título")
    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<List<Atividade>> getByTitulo(
            @PathVariable String titulo) {

        return ResponseEntity.ok(
                atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)
        );
    }

    @Operation(summary = "Cria uma atividade")
    @PostMapping
    public ResponseEntity<Atividade> post(
            @Valid @RequestBody Atividade atividade,
            Authentication authentication) {

        exigirTurmaInformada(atividade);

        Turma turma = turmaRepository.findById(atividade.getTurma().getId()).orElse(null);

        if (turma != null) {

            // Professor só cria atividade nas turmas dele. O autor é quem está logado (token),
            // nunca um "usuario" vindo no corpo da requisição.
            Usuario autor = atividadeService.exigirPodeAlterar(turma, authentication.getName());
            TurmaService.exigirTurmaAtiva(turma);

            atividade.setId(null);
            atividade.setUsuario(autor);

            ///Salva pelo service para os alunos receberem a notificação
            Atividade salva = atividadeService.criarAtividade(atividade);

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

    @Operation(summary = "Edita ou publica a atividade")
    @PutMapping
    public ResponseEntity<Atividade> put(
            @Valid @RequestBody Atividade atividade,
            Authentication authentication) {

        if (atividade.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O id da atividade é obrigatório para atualizar.",
                    null
            );
        }

        exigirTurmaInformada(atividade);

        Atividade existente = atividadeRepository.findById(atividade.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        Turma novaTurma = turmaRepository.findById(atividade.getTurma().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Turma não existe",
                        null
                ));

        // Professor só edita atividade da turma dele e não pode mover para a turma de outro professor
        atividadeService.exigirPodeAlterar(existente.getTurma(), authentication.getName());
        atividadeService.exigirPodeAlterar(novaTurma, authentication.getName());
        // Mudar a atividade para outra turma: só se ela não estiver arquivada
        if (!novaTurma.getId().equals(existente.getTurma().getId())) {
            TurmaService.exigirTurmaAtiva(novaTurma);
        }

        StatusAtividade statusAnterior = existente.getStatus();

        atividade.setPerguntas(existente.getPerguntas());
        // O autor não muda na edição
        atividade.setUsuario(existente.getUsuario());
        // O tipo é definido no cadastro do questionário (etapa 2) e não muda pela edição dos dados gerais.
        atividade.setTipo(existente.getTipo());

        Atividade salva = atividadeRepository.save(atividade);

        boolean foiPublicada =
                statusAnterior == StatusAtividade.PENDENTE
                        && salva.getStatus() == StatusAtividade.EM_ANDAMENTO;

        if (foiPublicada) {

            ///Avisa os alunos que a atividade está disponível
            atividadeService.notificarPublicacao(salva);

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

    @Operation(summary = "Salva a resposta do aluno em andamento")
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
    @Operation(summary = "Exclui a atividade")
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Professor só exclui atividade da turma dele
        atividadeService.exigirPodeAlterar(atividade.getTurma(), authentication.getName());

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

    private void exigirTurmaInformada(Atividade atividade) {
        if (atividade.getTurma() == null || atividade.getTurma().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A turma da atividade é obrigatória.",
                    null
            );
        }
    }
}