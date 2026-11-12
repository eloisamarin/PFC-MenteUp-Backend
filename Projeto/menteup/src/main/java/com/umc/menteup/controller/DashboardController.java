package com.umc.menteup.controller;


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
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public Resumo resumo() {
        return dashboardService.resumo();
    }

    @GetMapping("/atividades-concluidas-por-dia")
    public List<ConclusoesDia> atividadesConcluidasPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.atividadesConcluidasPorDia(de, ate);
    }

    @GetMapping("/desafios-por-dia")
    public List<DesafiosDia> desafiosPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.desafiosPorDia(de, ate);
    }

    @GetMapping("/acoes-por-dia")
    public List<AcoesDia> acoesPorDia(
            @RequestParam(required = false) Long de,
            @RequestParam(required = false) Long ate) {
        return dashboardService.acoesPorDia(de, ate);
    }

    @GetMapping("/acerto-por-turma")
    public List<AcertoTurma> acertoPorTurma() {
        return dashboardService.acertoPorTurma();
    }

    @GetMapping("/alunos-por-turma")
    public List<Item> alunosPorTurma() {
        return dashboardService.alunosPorTurma();
    }

    @GetMapping("/atividades-por-template")
    public List<Item> atividadesPorTemplate() {
        return dashboardService.atividadesPorTemplate();
    }

    @GetMapping("/atividades-por-status")
    public List<Item> atividadesPorStatus() {
        return dashboardService.atividadesPorStatus();
    }

    @GetMapping("/avatares")
    public List<Item> avatares() {
        return dashboardService.avatares();
    }
}

