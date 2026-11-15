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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/atividades")
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

    @Operation(summary = "Lista as atividades (administrador: todas; professor: as das turmas dele)")
    @GetMapping
    public ResponseEntity<List<Atividade>> getAll(Authentication authentication) {
        return ResponseEntity.ok(atividadeService.listarParaGestao(authentication.getName(), null));
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

    @Operation(summary = "Busca atividades pelo título (mesma regra de acesso da listagem)")
    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<List<Atividade>> getByTitulo(
            @PathVariable String titulo,
            Authentication authentication) {

        return ResponseEntity.ok(
                atividadeService.listarParaGestao(authentication.getName(), titulo)
        );
    }

    @Operation(summary = "Cria uma atividade")
    @PostMapping
    public ResponseEntity<Atividade> post(
            @Valid @RequestBody Atividade atividade,
            Authentication authentication) {

        exigirTurmaInformada(atividade);

        List<Turma> turmas = carregarTurmas(atividade, "Turma não existe!");

        // Professor só cria atividade nas turmas dele. O autor é quem está logado (token),
        // nunca um "usuario" vindo no corpo da requisição.
        Usuario autor = null;
        for (Turma turma : turmas) {
            autor = atividadeService.exigirPodeAlterar(turma, authentication.getName());
            TurmaService.exigirTurmaAtiva(turma);
        }
        AtividadeService.exigirMesmoProfessor(turmas);

        atividade.setId(null);
        atividade.setUsuario(autor);
        atividade.setTurmas(turmas);

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
                        "turmaIds", idsDasTurmas(salva),
                        "pontuacao", salva.getPontuacao()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salva);
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

        List<Turma> novasTurmas = carregarTurmas(atividade, "Turma não existe");

        // Professor só edita atividade das turmas dele e não pode passar para a turma de outro professor
        if (existente.getTurmas().isEmpty()) {
            atividadeService.exigirPodeAlterar((Turma) null, authentication.getName());
        }
        for (Turma turma : existente.getTurmas()) {
            atividadeService.exigirPodeAlterar(turma, authentication.getName());
        }
        List<Long> turmasAtuais = idsDasTurmas(existente);
        for (Turma turma : novasTurmas) {
            atividadeService.exigirPodeAlterar(turma, authentication.getName());
            // Colocar a atividade em mais uma turma: só se ela não estiver arquivada
            if (!turmasAtuais.contains(turma.getId())) {
                TurmaService.exigirTurmaAtiva(turma);
            }
        }
        AtividadeService.exigirMesmoProfessor(novasTurmas);
        atividade.setTurmas(novasTurmas);

        StatusAtividade statusAnterior = existente.getStatus();
        // Guarda os valores antigos antes de salvar (o save copia os novos para o mesmo objeto)
        // A pontuação vale nota, então o log mostra a antiga e a nova
        String tituloAnterior = existente.getTitulo();
        Integer pontuacaoAnterior = existente.getPontuacao();

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
                            "tituloAnterior", tituloAnterior,
                            "tituloNovo", salva.getTitulo(),
                            "statusAnterior", statusAnterior.name(),
                            "statusNovo", salva.getStatus().name(),
                            "pontuacaoAnterior", pontuacaoAnterior,
                            "pontuacaoNova", salva.getPontuacao()
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

        // Professor só exclui atividade das turmas dele
        atividadeService.exigirPodeAlterarAtividade(atividade, authentication.getName());

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
        if (atividade.getTurmas() == null || atividade.getTurmas().isEmpty()
                || atividade.getTurmas().stream().anyMatch(t -> t == null || t.getId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A turma da atividade é obrigatória.",
                    null
            );
        }
    }

    ///Busca cada turma informada no banco, sem repetir
    private List<Turma> carregarTurmas(Atividade atividade, String mensagemSeNaoExistir) {
        List<Turma> turmas = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(idsDasTurmas(atividade))) {
            turmas.add(turmaRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            mensagemSeNaoExistir,
                            null
                    )));
        }
        return turmas;
    }

    private static List<Long> idsDasTurmas(Atividade atividade) {
        return atividade.getTurmas().stream().map(Turma::getId).toList();
    }
}
