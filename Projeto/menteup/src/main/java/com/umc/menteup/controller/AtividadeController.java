package com.umc.menteup.controller;

import com.umc.menteup.model.Atividade;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/atividades")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AtividadeController {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @GetMapping
    public ResponseEntity<List<Atividade>> getAll(){
        return ResponseEntity.ok(atividadeRepository.findAll());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Atividade> getById(@PathVariable Long id){
        return atividadeRepository.findById(id)
                .map(resposta -> ResponseEntity.ok(resposta))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    @GetMapping("/titulo/{titulo}")
    public ResponseEntity<List<Atividade>> getByTitulo(@PathVariable String titulo)  {
        return ResponseEntity.ok(atividadeRepository.findAllByTituloContainingIgnoreCase((titulo)));
    }

    @PostMapping
    public ResponseEntity<Atividade> post(@Valid @RequestBody Atividade atividade){
        if(turmaRepository.existsById(atividade.getTurma().getId())) {
            atividade.setId(null);
            return ResponseEntity.status((HttpStatus.CREATED)).body(atividadeRepository.save(atividade));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turma não existe!", null);
    }

    @PutMapping
    public ResponseEntity<Atividade> put(@Valid @RequestBody Atividade atividade) {
        if(turmaRepository.existsById(atividade.getTurma().getId())) {
            if(turmaRepository.existsById(atividade.getTurma().getId()))
                return ResponseEntity.status((HttpStatus.OK)).body(atividadeRepository.save(atividade));

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turma não existe!", null);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }


    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete (@PathVariable Long id ){
        Optional<Atividade> atividade = atividadeRepository.findById(id);

        if (atividade.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        atividadeRepository.deleteById(id);
    }
}
