package com.umc.menteup.controller;

import java.util.List;

import com.umc.menteup.dto.ExclusaoTurmaResponse;
import com.umc.menteup.model.Turma;
import com.umc.menteup.repository.TurmaRepository;

import com.umc.menteup.service.TurmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Turmas", description = "Criação de turmas e vínculo de alunos e professor")
public class TurmaController {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private TurmaService turmaService;
    @Operation(summary = "Lista as turmas (as arquivadas só com incluirArquivadas=true)")
    @GetMapping("/all")
    public ResponseEntity<List<Turma>> getAll(
            @RequestParam(defaultValue = "false") boolean incluirArquivadas) {
        return ResponseEntity.ok(turmaService.listar(incluirArquivadas));
    }

    @Operation(summary = "Verifica se a turma pode ser excluída e o que a impede")
    @GetMapping("/{id}/exclusao")
    public ResponseEntity<ExclusaoTurmaResponse> verificarExclusao(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(turmaService.verificarExclusao(id, authentication.getName()));
    }

    @Operation(summary = "Arquiva a turma (some das listas e mantém o histórico)")
    @PutMapping("/{id}/arquivar")
    public ResponseEntity<Turma> arquivar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(turmaService.alterarArquivamento(id, true, authentication.getName()));
    }

    @Operation(summary = "Desarquiva a turma")
    @PutMapping("/{id}/desarquivar")
    public ResponseEntity<Turma> desarquivar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(turmaService.alterarArquivamento(id, false, authentication.getName()));
    }

    @Operation(summary = "Busca uma turma pelo id")
    @GetMapping("/{id}")
    public ResponseEntity<Turma> getById(@PathVariable Long id) {
        return turmaRepository.findById(id)
                .map(resposta -> ResponseEntity.ok(resposta))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(summary = "Busca turmas pelo nome")
    @GetMapping("/nome/{nome}")
    public ResponseEntity<List<Turma>> getAllByNome(
            @PathVariable String nome) {

        return ResponseEntity.ok(
                turmaRepository.findAllByNomeContainingIgnoreCase(nome)
        );
    }

    // Somente PROFESSOR ou ADMINISTRADOR realiza criação de Turma
    @Operation(summary = "Cria uma turma (o administrador pode escolher o professor)")
    @PostMapping
    public ResponseEntity<Turma> post(
            @Valid @RequestBody Turma turma,
            Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(turmaService.criarTurma(turma, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR realiza vinculamento de alunos com Turmas
    @Operation(summary = "Vincula um aluno à turma")
    @PostMapping("/{turmaId}/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vincularAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId,
            Authentication authentication) {

        turmaService.vincularAluno(turmaId, alunoId, authentication.getName());
    }

    // Somente PROFESSOR ou ADMINISTRADOR remove alunos de Turmas
    @Operation(summary = "Remove um aluno da turma")
    @DeleteMapping("/{turmaId}/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId,
            Authentication authentication) {

        turmaService.removerAluno(turmaId, alunoId, authentication.getName());
    }

    // Somente PROFESSOR ou ADMINISTRADOR realiza atualizar a Turma
    @Operation(summary = "Renomeia a turma ou troca o professor responsável")
    @PutMapping
    public ResponseEntity<Turma> put(
            @Valid @RequestBody Turma turma,
            Authentication authentication) {

        return ResponseEntity.ok(
                turmaService.atualizarTurma(turma, authentication.getName()));
    }

    // Somente PROFESSOR ou ADMINISTRADOR deletar a turma
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a turma")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {

        turmaService.excluirTurma(id, authentication.getName());
    }
}
