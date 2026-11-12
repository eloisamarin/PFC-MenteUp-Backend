package com.umc.menteup.controller;

import com.umc.menteup.dto.SolicitacaoTurmaDtos.NovaSolicitacao;
import com.umc.menteup.dto.SolicitacaoTurmaDtos.Solicitacao;
import com.umc.menteup.dto.SolicitacaoTurmaDtos.TurmaDoAluno;
import com.umc.menteup.service.SolicitacaoTurmaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Pedidos de entrada e saída de turma.
 * Aluno:      /alunos/me/...  (só alunos, pela regra do SecurityConfig)
 * Professor e administrador: /turmas/{turmaId}/solicitacoes/...
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SolicitacaoTurmaController {

    private final SolicitacaoTurmaService solicitacaoService;

    public SolicitacaoTurmaController(SolicitacaoTurmaService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    // ------------------------------------------------------------- Aluno

    // Todas as turmas, com "matriculado" e o pedido aberto (se houver)
    @GetMapping("/alunos/me/turmas/disponiveis")
    public List<TurmaDoAluno> turmasDoAluno(Authentication authentication) {
        return solicitacaoService.turmasDoAluno(authentication.getName());
    }

    @GetMapping("/alunos/me/solicitacoes")
    public List<Solicitacao> minhasSolicitacoes(Authentication authentication) {
        return solicitacaoService.minhasSolicitacoes(authentication.getName());
    }

    // Pede para entrar (ENTRADA) ou sair (SAIDA) de uma turma
    @PostMapping("/alunos/me/solicitacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Solicitacao solicitar(@Valid @RequestBody NovaSolicitacao pedido, Authentication authentication) {
        return solicitacaoService.solicitar(authentication.getName(), pedido.turmaId(), pedido.tipo());
    }

    // Cancela um pedido ainda não respondido
    @DeleteMapping("/alunos/me/solicitacoes/{solicitacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long solicitacaoId, Authentication authentication) {
        solicitacaoService.cancelar(authentication.getName(), solicitacaoId);
    }

    // ------------------------------------------------------------- Professor e administrador

    @GetMapping("/turmas/{turmaId}/solicitacoes")
    public List<Solicitacao> pendentesDaTurma(@PathVariable Long turmaId, Authentication authentication) {
        return solicitacaoService.pendentesDaTurma(authentication.getName(), turmaId);
    }

    @PutMapping("/turmas/{turmaId}/solicitacoes/{solicitacaoId}/aprovar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aprovar(@PathVariable Long turmaId, @PathVariable Long solicitacaoId, Authentication authentication) {
        solicitacaoService.aprovar(authentication.getName(), turmaId, solicitacaoId);
    }

    @PutMapping("/turmas/{turmaId}/solicitacoes/{solicitacaoId}/recusar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recusar(@PathVariable Long turmaId, @PathVariable Long solicitacaoId, Authentication authentication) {
        solicitacaoService.recusar(authentication.getName(), turmaId, solicitacaoId);
    }
}
