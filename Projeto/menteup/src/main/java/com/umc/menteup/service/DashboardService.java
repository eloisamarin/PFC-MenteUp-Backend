package com.umc.menteup.service;

import com.umc.menteup.dto.DashboardDtos.AcertoTurma;
import com.umc.menteup.dto.DashboardDtos.AcoesDia;
import com.umc.menteup.dto.DashboardDtos.ConclusoesDia;
import com.umc.menteup.dto.DashboardDtos.DesafiosDia;
import com.umc.menteup.dto.DashboardDtos.Item;
import com.umc.menteup.dto.DashboardDtos.Resumo;
import com.umc.menteup.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@Transactional(readOnly = true)
public class DashboardService {

    // As datas são gravadas no horário de Brasília (LocalDateTime, sem fuso)
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_PADRAO = 30;
    private static final int DIAS_MAXIMO = 366;

    // Contas excluídas (anonimizadas) não contam como alunos ou professores
    private static final String ATIVO = "usuario NOT LIKE 'removido-%@menteup.invalid'";

    // Total de perguntas de cada atividade, para calcular a % de acerto
    private static final String PERGUNTAS_POR_ATIVIDADE =
            "(SELECT atividade_id, COUNT(*) AS total FROM perguntas GROUP BY atividade_id)";

    private static final Set<String> ACOES_DE_PROFESSOR = Set.of(
            "CRIAR_ATIVIDADE", "EDITAR_ATIVIDADE", "EXCLUIR_ATIVIDADE", "PUBLICAR_ATIVIDADE",
            "CRIAR_QUESTAO", "CRIAR_TURMA", "ADICIONAR_ALUNO_TURMA", "REMOVER_ALUNO_TURMA");
    private static final Set<String> ACOES_DE_ALUNO = Set.of(
            "INICIAR_ATIVIDADE", "CONCLUIR_ATIVIDADE", "RESPONDER_QUESTAO");

    private static final Map<String, String> TEMPLATES = Map.of(
            "QUIZ", "Quiz",
            "VERDADEIRO_FALSO", "Verdadeiro ou falso",
            "ASSOCIACAO", "Associação",
            "ORDENACAO", "Ordenação",
            "LACUNAS", "Lacunas");
    private static final Map<String, String> STATUS = Map.of(
            "PENDENTE", "Pendente",
            "EM_ANDAMENTO", "Em andamento",
            "CONCLUIDO", "Concluída");
    private static final Map<String, String> AVATARES = Map.of(
            "CACHORRO", "Cachorro", "GATO", "Gato", "RAPOSA", "Raposa", "PANDA", "Panda",
            "COALA", "Coala", "TIGRE", "Tigre", "LEAO", "Leão", "SAPO", "Sapo",
            "MACACO", "Macaco", "COELHO", "Coelho");

    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------- Big Numbers

    public Resumo resumo() {
        long[] atividades = jdbc.queryForObject(
                "SELECT COALESCE(SUM(r.acertos), 0), COALESCE(SUM(q.total), 0) "
                        + "FROM resultado_atividade r JOIN " + PERGUNTAS_POR_ATIVIDADE
                        + " q ON q.atividade_id = r.atividade_id",
                (rs, linha) -> new long[]{rs.getLong(1), rs.getLong(2)});

        long[] desafios = jdbc.queryForObject(
                "SELECT COUNT(*), COALESCE(SUM(CASE WHEN correta THEN 1 ELSE 0 END), 0) FROM tb_resposta_desafio",
                (rs, linha) -> new long[]{rs.getLong(1), rs.getLong(2)});

        return new Resumo(
                contar("SELECT COUNT(*) FROM tb_usuarios WHERE UPPER(tipo_usuario) = 'ALUNO' AND " + ATIVO),
                contar("SELECT COUNT(*) FROM tb_usuarios WHERE UPPER(tipo_usuario) = 'PROFESSOR' AND " + ATIVO),
                contar("SELECT COUNT(*) FROM tb_turma"),
                contar("SELECT COUNT(*) FROM tb_atividade"),
                contar("SELECT COUNT(*) FROM resultado_atividade"),
                percentual(atividades[0], atividades[1]),
                contar("SELECT COUNT(*) FROM tb_desafio WHERE status = 'ENCERRADO'"),
                percentual(desafios[1], desafios[0]));
    }

    // ------------------------------------------------------------- Séries por dia

    public List<ConclusoesDia> atividadesConcluidasPorDia(Long de, Long ate) {
        Periodo periodo = periodo(de, ate);
        Map<LocalDate, Long> porDia = new HashMap<>();

        jdbc.query("SELECT CAST(concluida_em AS DATE), COUNT(*) FROM resultado_atividade "
                        + "WHERE concluida_em >= ? AND concluida_em < ? GROUP BY CAST(concluida_em AS DATE)",
                rs -> {
                    porDia.put(rs.getObject(1, LocalDate.class), rs.getLong(2));
                },
                periodo.inicio(), periodo.fim());

        return periodo.dias().stream()
                .map(dia -> new ConclusoesDia(formatar(dia), porDia.getOrDefault(dia, 0L)))
                .toList();
    }

    public List<DesafiosDia> desafiosPorDia(Long de, Long ate) {
        Periodo periodo = periodo(de, ate);
        Map<LocalDate, long[]> porDia = new HashMap<>();

        jdbc.query("SELECT CAST(respondida_em AS DATE), "
                        + "SUM(CASE WHEN correta THEN 1 ELSE 0 END), SUM(CASE WHEN correta THEN 0 ELSE 1 END) "
                        + "FROM tb_resposta_desafio WHERE respondida_em >= ? AND respondida_em < ? "
                        + "GROUP BY CAST(respondida_em AS DATE)",
                rs -> {
                    porDia.put(rs.getObject(1, LocalDate.class), new long[]{rs.getLong(2), rs.getLong(3)});
                },
                periodo.inicio(), periodo.fim());

        return periodo.dias().stream()
                .map(dia -> {
                    long[] valores = porDia.getOrDefault(dia, new long[]{0, 0});
                    return new DesafiosDia(formatar(dia), valores[0], valores[1]);
                })
                .toList();
    }

    // Ações do log por dia, separadas em ações de professor e de aluno
    public List<AcoesDia> acoesPorDia(Long de, Long ate) {
        Periodo periodo = periodo(de, ate);
        Map<LocalDate, long[]> porDia = new HashMap<>();

        jdbc.query("SELECT CAST(data_hora AS DATE), acao, COUNT(*) FROM log_sistema "
                        + "WHERE data_hora >= ? AND data_hora < ? GROUP BY CAST(data_hora AS DATE), acao",
                rs -> {
                    long[] valores = porDia.computeIfAbsent(rs.getObject(1, LocalDate.class), dia -> new long[2]);
                    String acao = rs.getString(2);

                    if (ACOES_DE_PROFESSOR.contains(acao)) {
                        valores[0] += rs.getLong(3);
                    } else if (ACOES_DE_ALUNO.contains(acao)) {
                        valores[1] += rs.getLong(3);
                    }
                },
                periodo.inicio(), periodo.fim());

        return periodo.dias().stream()
                .map(dia -> {
                    long[] valores = porDia.getOrDefault(dia, new long[]{0, 0});
                    return new AcoesDia(formatar(dia), valores[0], valores[1]);
                })
                .toList();
    }

    // ------------------------------------------------------------- Por categoria

    public List<AcertoTurma> acertoPorTurma() {
        return jdbc.query("SELECT t.nome, SUM(r.acertos), SUM(q.total) "
                                + "FROM resultado_atividade r "
                                + "JOIN tb_atividade a ON a.id = r.atividade_id "
                                + "JOIN tb_turma t ON t.id = a.turma_id "
                                + "JOIN " + PERGUNTAS_POR_ATIVIDADE + " q ON q.atividade_id = r.atividade_id "
                                + "GROUP BY t.id, t.nome",
                        (rs, linha) -> new AcertoTurma(rs.getString(1), percentual(rs.getLong(2), rs.getLong(3))))
                .stream()
                .filter(item -> item.acerto() != null)
                .sorted(Comparator.comparing(AcertoTurma::acerto).reversed())
                .toList();
    }

    public List<Item> alunosPorTurma() {
        return jdbc.query("SELECT t.nome, COUNT(ta.aluno_id) FROM tb_turma t "
                        + "LEFT JOIN tb_turma_aluno ta ON ta.turma_id = t.id "
                        + "GROUP BY t.id, t.nome ORDER BY COUNT(ta.aluno_id) DESC, t.nome",
                (rs, linha) -> new Item(rs.getString(1), rs.getLong(2)));
    }

    public List<Item> atividadesPorTemplate() {
        return agrupar("SELECT COALESCE(tipo, 'QUIZ'), COUNT(*) FROM tb_atividade GROUP BY COALESCE(tipo, 'QUIZ')",
                TEMPLATES);
    }

    public List<Item> atividadesPorStatus() {
        return agrupar("SELECT status, COUNT(*) FROM tb_atividade GROUP BY status", STATUS);
    }

    public List<Item> avatares() {
        return agrupar("SELECT avatar, COUNT(*) FROM tb_usuarios WHERE avatar IS NOT NULL AND " + ATIVO
                + " GROUP BY avatar", AVATARES);
    }

    // ------------------------------------------------------------- Auxiliares

    private long contar(String sql) {
        Long total = jdbc.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    // Percentual com 1 casa decimal; null quando ainda não há base de cálculo
    private static Double percentual(long parte, long total) {
        if (total == 0) {
            return null;
        }
        return Math.round(1000.0 * parte / total) / 10.0;
    }

    // Troca o código do banco (ex.: VERDADEIRO_FALSO) pelo nome que aparece no gráfico
    private List<Item> agrupar(String sql, Map<String, String> nomes) {
        return jdbc.query(sql, (rs, linha) ->
                        new Item(nomes.getOrDefault(rs.getString(1), rs.getString(1)), rs.getLong(2)))
                .stream()
                .sorted(Comparator.comparingLong(Item::quantidade).reversed().thenComparing(Item::nome))
                .toList();
    }

    private static String formatar(LocalDate dia) {
        return dia.atStartOfDay(FUSO).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // "de" e "ate" chegam em milissegundos (como o Grafana manda: ${__from} e ${__to})
    private Periodo periodo(Long de, Long ate) {
        LocalDate fim = ate == null ? LocalDate.now(FUSO) : Instant.ofEpochMilli(ate).atZone(FUSO).toLocalDate();
        LocalDate inicio = de == null
                ? fim.minusDays(DIAS_PADRAO - 1L)
                : Instant.ofEpochMilli(de).atZone(FUSO).toLocalDate();

        if (inicio.isAfter(fim)) {
            throw ApiException.requisicaoInvalida("PERIODO_INVALIDO", "A data inicial é depois da data final.");
        }
        if (inicio.plusDays(DIAS_MAXIMO).isBefore(fim)) {
            throw ApiException.requisicaoInvalida("PERIODO_MUITO_LONGO", "O período pode ter no máximo 366 dias.");
        }
        return new Periodo(inicio, fim);
    }

    private record Periodo(LocalDate primeiroDia, LocalDate ultimoDia) {

        LocalDateTime inicio() {
            return primeiroDia.atStartOfDay();
        }

        LocalDateTime fim() {
            return ultimoDia.plusDays(1).atStartOfDay();
        }

        List<LocalDate> dias() {
            List<LocalDate> dias = new ArrayList<>();
            for (LocalDate dia = primeiroDia; !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
                dias.add(dia);
            }
            return dias;
        }
    }
}
