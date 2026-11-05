package com.umc.menteup.controller;

import com.umc.menteup.dto.DesempenhoTurmaResponse;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.service.DesempenhoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/turmas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DesempenhoController {

    // O service onde estão as regras: quem pode ver e como os números são calculados
    private final DesempenhoService desempenhoService;
    // Injeção pelo construtor: o Spring cria o service e o entrega aqui
    public DesempenhoController(DesempenhoService desempenhoService) {
        this.desempenhoService = desempenhoService;

    }

    /*
     * GET /turmas/minhas
     * Lista as turmas para o seletor da tela.
     * - Administrador: recebe todas as turmas.
     * - Professor: recebe só as turmas às quais está vinculado.
     * - Aluno: recebe 403 (sem permissão).
     *
     * O Authentication é preenchido pelo Spring a partir do token JWT,
     * e getName() devolve o e-mail do usuário logado. Assim, ninguém
     * consegue se passar por outra pessoa enviando outro e-mail na requisição.
     */
    @GetMapping("/minhas")
    public ResponseEntity<List<TurmaResponse>> listarTurmas(Authentication authentication) {
        return ResponseEntity.ok(desempenhoService.listarTurmas(authentication.getName()));
    }

    /* Mostra o desempenho da turma escolhida: a pontuação, os acertos e
    as atividades concluídas de cada aluno, além da média e da participação da turma.
     */

    @GetMapping("/{id}/desempenho")
    public ResponseEntity<DesempenhoTurmaResponse> buscarDesempenho(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(desempenhoService.buscarDesempenho(id, authentication.getName()));
    }


}
