package com.umc.menteup.controller;

import com.umc.menteup.dto.DashboardDtos.Painel;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.umc.menteup.dto.DashboardDtos.AcertoTurma;
import com.umc.menteup.dto.DashboardDtos.AcoesDia;
import com.umc.menteup.dto.DashboardDtos.ConclusoesDia;
import com.umc.menteup.dto.DashboardDtos.DesafiosDia;
import com.umc.menteup.dto.DashboardDtos.Item;
import com.umc.menteup.dto.DashboardDtos.Resumo;
import com.umc.menteup.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 - API lida pelo Grafana. Acesso: administrador logado (JWT) ou o token
 da dashboard no cabeçalho X-Dashboard-Token (ver DashboardTokenFilter).

 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Painel do sistema por perfil e indicadores lidos pelo Grafana")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Painel do sistema com filtros (aluno: os próprios dados; professor: as turmas dele; "
            + "administrador: tudo)")
    @GetMapping("/painel")
    public Painel painel(
            @RequestParam(defaultValue = "30") int dias,
            @RequestParam(required = false) Long turmaId,
            @RequestParam(required = false) Long alunoId,
            Authentication authentication) {
        return dashboardService.painel(authentication.getName(), dias, turmaId, alunoId);
    }

    @Operation(summary = "Números gerais (big numbers)")
    @GetMapping("/resumo")
    public Resumo resumo() {
        return dashboardService.resumo();
    }

    @Operation(summary = "Atividades concluídas por dia")
    @GetMapping("/atividades-concluidas-por-dia")
    public List<ConclusoesDia> atividadesConcluidasPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.atividadesConcluidasPorDia(de, ate);
    }

    @Operation(summary = "Desafios por dia")
    @GetMapping("/desafios-por-dia")
    public List<DesafiosDia> desafiosPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.desafiosPorDia(de, ate);
    }

    @Operation(summary = "Ações no sistema por dia")
    @GetMapping("/acoes-por-dia")
    public List<AcoesDia> acoesPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.acoesPorDia(de, ate);
    }

    @Operation(summary = "Percentual de acerto por turma")
    @GetMapping("/acerto-por-turma")
    public List<AcertoTurma> acertoPorTurma() {
        return dashboardService.acertoPorTurma();
    }

    @Operation(summary = "Alunos por turma")
    @GetMapping("/alunos-por-turma")
    public List<Item> alunosPorTurma() {
        return dashboardService.alunosPorTurma();
    }

    @Operation(summary = "Atividades por tipo (template)")
    @GetMapping("/atividades-por-template")
    public List<Item> atividadesPorTemplate() {
        return dashboardService.atividadesPorTemplate();
    }

    @Operation(summary = "Atividades por status")
    @GetMapping("/atividades-por-status")
    public List<Item> atividadesPorStatus() {
        return dashboardService.atividadesPorStatus();
    }

    @Operation(summary = "Avatares mais escolhidos")
    @GetMapping("/avatares")
    public List<Item> avatares() {
        return dashboardService.avatares();
    }
}

