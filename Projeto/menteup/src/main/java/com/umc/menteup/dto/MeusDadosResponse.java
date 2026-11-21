package com.umc.menteup.dto;

import java.time.LocalDateTime;
import java.util.List;

// GET /usuarios/me/exportar: todos os dados que a plataforma guarda sobre a pessoa (acesso e portabilidade)
// Nunca leva senha, códigos de verificação, links de senha nem o token do celular
public record MeusDadosResponse(
        LocalDateTime geradoEm,
        Conta conta,
        List<DocumentoLegalDtos.Aceite> aceites,
        List<Turma> turmas,
        List<Atividade> atividades,
        List<Desafio> desafios,
        List<Conquista> conquistas,
        List<Registro> registros
) {

    public record Conta(Long id, String nome, String email, String tipo, String avatar,
                        String situacaoCadastro, LocalDateTime termoAceitoEm,
                        LocalDateTime idadeMinimaConfirmadaEm) {
    }

    public record Turma(Long id, String nome) {
    }

    public record Atividade(String titulo, Integer acertos, Integer pontuacao, LocalDateTime concluidaEm,
                            String comentarioProfessor, LocalDateTime comentadoEm) {
    }

    public record Desafio(String desafio, String pergunta, Boolean correta, Integer pontos,
                          LocalDateTime respondidaEm) {
    }

    public record Conquista(String nome, Boolean desbloqueada, Integer progresso, Integer meta,
                            Integer xpConcedido, LocalDateTime desbloqueadaEm) {
    }

    // Registros de log feitos pela própria pessoa (o que fez, quando e de qual IP)
    public record Registro(String acao, String resultado, String descricao, String ip, LocalDateTime dataHora) {
    }
}
