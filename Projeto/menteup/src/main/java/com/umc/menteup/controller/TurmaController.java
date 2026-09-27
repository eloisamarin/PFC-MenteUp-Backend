package com.umc.menteup.controller;

import java.util.List;
import java.util.Optional;

import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.TurmaRepository;

import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TurmaController {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/all")
    public ResponseEntity<List<TurmaResponse>> getAll() {
        List<TurmaResponse> turmas = turmaRepository.findAll()
                .stream()
                .map(turma -> new TurmaResponse(turma.getId(), turma.getNome()))
                .toList();

        return ResponseEntity.ok(turmas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Turma> getById(@PathVariable Long id) {
        return turmaRepository.findById(id)
                .map(resposta -> ResponseEntity.ok(resposta))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<List<Turma>> getAllByNome(
            @PathVariable String nome) {

        return ResponseEntity.ok(
                turmaRepository.findAllByNomeContainingIgnoreCase(nome)
        );
    }

    @PostMapping
    public ResponseEntity<Turma> post(
            @Valid @RequestBody Turma turma) {

        turma.setId(null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(turmaRepository.save(turma));
    }

    @PutMapping
    public ResponseEntity<Turma> put(
            @Valid @RequestBody Turma turma) {

        return turmaRepository.findById(turma.getId())
                .map(resposta -> ResponseEntity.status(HttpStatus.OK)
                        .body(turmaRepository.save(turma)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        Optional<Turma> turma = turmaRepository.findById(id);

        if (turma.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        turmaRepository.deleteById(id);
    }
    @PostMapping("/{turmaId}/alunos/{alunoId}")
    public ResponseEntity<?> adicionarAluno(
            @PathVariable Long turmaId,
            @PathVariable Long alunoId) {

        Optional<Turma> turmaOptional = turmaRepository.findById(turmaId);

        if (turmaOptional.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Turma não encontrada.");
        }

        Optional<Usuario> alunoOptional = usuarioRepository.findById(alunoId);

        if (alunoOptional.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Aluno não encontrado.");
        }

        Turma turma = turmaOptional.get();
        Usuario aluno = alunoOptional.get();

        if (!"ALUNO".equalsIgnoreCase(aluno.getTipoUsuario())) {
            return ResponseEntity
                    .badRequest()
                    .body("O usuário informado não é um aluno.");
        }

        if (turma.getAlunos().contains(aluno)) {
            return ResponseEntity
                    .badRequest()
                    .body("O aluno já está cadastrado nesta turma.");
        }

        turma.getAlunos().add(aluno);

        turmaRepository.save(turma);

        return ResponseEntity.ok(turma);
    }
}
