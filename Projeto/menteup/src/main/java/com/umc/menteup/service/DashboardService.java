package com.umc.menteup.service;

import com.umc.menteup.dto.DashboardDtos.AcertoTurma;
import com.umc.menteup.dto.DashboardDtos.AcoesDia;
import com.umc.menteup.dto.DashboardDtos.ConclusoesDia;
import com.umc.menteup.dto.DashboardDtos.DesafiosDia;
import com.umc.menteup.dto.DashboardDtos.AlunoPainel;
import com.umc.menteup.dto.DashboardDtos.Item;
import com.umc.menteup.dto.DashboardDtos.Opcao;
import com.umc.menteup.dto.DashboardDtos.Painel;
import com.umc.menteup.dto.DashboardDtos.Resumo;
import com.umc.menteup.dto.DashboardDtos.ResumoPainel;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    // Períodos que o painel do sistema aceita (em dias)
    private static final Set<Integer> DIAS_DO_PAINEL = Set.of(7, 30, 90, 365);

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate jdbcComNomes;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(JdbcTemplate jdbc, UsuarioRepository usuarioRepository) {
        this.jdbc = jdbc;
        this.jdbcComNomes = new NamedParameterJdbcTemplate(jdbc);
        this.usuarioRepository = usuarioRepository;
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

    // ------------------------------------------------------------- Painel do sistema (GET /painel)

    /*
     * Dashboard de cada perfil, com filtro de período, turma e aluno:
     * - aluno: só os próprios dados (o filtro de turma escolhe entre as turmas dele);
     * - professor: as turmas em que é o responsável e os alunos dessas turmas;
     * - administrador: tudo.
     * Turma ou aluno fora do recorte do perfil: 403.
     */
    @Transactional(readOnly = true)
    public Painel painel(String username, int dias, Long turmaId, Long alunoId) {
        if (!DIAS_DO_PAINEL.contains(dias)) {
            throw ApiException.requisicaoInvalida("PERIODO_INVALIDO", "Escolha um período de 7, 30, 90 ou 365 dias.");
        }

        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
        boolean administrador = TipoUsuario.ehAdministrador(usuario);
        boolean professor = TipoUsuario.ehProfessor(usuario);
        boolean aluno = !administrador && !professor;

        // Turmas que o perfil pode escolher no filtro
        List<Opcao> turmas = turmasPermitidas(usuario.getId(), administrador, professor);
        if (turmaId != null && turmas.stream().noneMatch(t -> t.id().equals(turmaId))) {
            throw ApiException.proibido("SEM_ACESSO_A_TURMA", "Você não tem acesso aos dados desta turma.");
        }

        // Recorte de turmas das consultas. Aluno e administrador sem filtro: sem restrição de turma
        // (o aluno já fica restrito aos próprios dados). Professor: sempre as turmas dele.
        List<Long> turmasDoRecorte = turmaId != null ? List.of(turmaId) : turmas.stream().map(Opcao::id).toList();
        boolean todasAsTurmas = turmaId == null && !professor;

        // Alunos do recorte (tabela e filtro): só para professor e administrador
        List<Opcao> alunos = aluno ? List.of() : alunosDoRecorte(turmasDoRecorte, todasAsTurmas);
        Long alunoFiltrado;
        if (aluno) {
            if (alunoId != null && !alunoId.equals(usuario.getId())) {
                throw ApiException.proibido("SEM_ACESSO_AO_ALUNO", "Você só pode ver os seus próprios dados.");
            }
            alunoFiltrado = usuario.getId();
        } else {
            if (alunoId != null && alunos.stream().noneMatch(a -> a.id().equals(alunoId))) {
                throw ApiException.proibido("SEM_ACESSO_AO_ALUNO", "Este aluno não está nas turmas que você pode ver.");
            }
            alunoFiltrado = alunoId;
        }

        String perfil = administrador ? "ADMINISTRADOR" : professor ? "PROFESSOR" : "ALUNO";
        long inicioEmMilissegundos = LocalDate.now(FUSO).minusDays(dias - 1L)
                .atStartOfDay(FUSO).toInstant().toEpochMilli();
        Periodo periodo = periodo(inicioEmMilissegundos, null);

        // Professor sem nenhuma turma: não há o que mostrar
        if (!todasAsTurmas && turmasDoRecorte.isEmpty()) {
            return new Painel(perfil, dias, turmaId, alunoId, turmas, alunos,
                    new ResumoPainel(0, 0, 0, 0, null, 0, 0, null, 0),
                    serieVazia(periodo), List.of(), List.of());
        }

        Recorte recorte = new Recorte(turmasDoRecorte, todasAsTurmas, alunoFiltrado, periodo);
        List<Opcao> alunosDaTabela = alunoFiltrado == null || aluno
                ? alunos
                : alunos.stream().filter(a -> a.id().equals(alunoFiltrado)).toList();

        return new Painel(
                perfil, dias, turmaId, alunoId, turmas, alunos,
                resumoDoPainel(recorte, turmaId != null ? 1 : turmas.size(), alunosDaTabela.size()),
                conclusoesPorDia(recorte),
                acertoPorTurma(recorte),
                aluno ? List.of() : desempenhoDosAlunos(recorte, alunosDaTabela));
    }

    private List<Opcao> turmasPermitidas(Long usuarioId, boolean administrador, boolean professor) {
        String sql;
        if (administrador) {
            sql = "SELECT t.id, t.nome FROM tb_turma t ORDER BY t.nome";
        } else if (professor) {
            sql = "SELECT t.id, t.nome FROM tb_turma t WHERE t.professor_id = :usuario ORDER BY t.nome";
        } else {
            sql = "SELECT t.id, t.nome FROM tb_turma t JOIN tb_turma_aluno ta ON ta.turma_id = t.id "
                    + "WHERE ta.aluno_id = :usuario ORDER BY t.nome";
        }
        return jdbcComNomes.query(sql, new MapSqlParameterSource("usuario", usuarioId),
                (rs, linha) -> new Opcao(rs.getLong(1), rs.getString(2)));
    }

    private List<Opcao> alunosDoRecorte(List<Long> turmas, boolean todasAsTurmas) {
        if (todasAsTurmas) {
            return jdbc.query("SELECT id, nome_usuario FROM tb_usuarios WHERE UPPER(tipo_usuario) = 'ALUNO' AND "
                            + ATIVO + " ORDER BY nome_usuario",
                    (rs, linha) -> new Opcao(rs.getLong(1), rs.getString(2)));
        }
        if (turmas.isEmpty()) {
            return List.of();
        }
        return jdbcComNomes.query("SELECT DISTINCT u.id, u.nome_usuario FROM tb_turma_aluno ta "
                        + "JOIN tb_usuarios u ON u.id = ta.aluno_id "
                        + "WHERE ta.turma_id IN (:turmas) AND u.usuario NOT LIKE 'removido-%@menteup.invalid' "
                        + "ORDER BY u.nome_usuario",
                new MapSqlParameterSource("turmas", turmas),
                (rs, linha) -> new Opcao(rs.getLong(1), rs.getString(2)));
    }

    private ResumoPainel resumoDoPainel(Recorte recorte, long totalTurmas, long totalAlunos) {
        long[] atividades = jdbcComNomes.queryForObject(
                "SELECT COUNT(*), COALESCE(SUM(r.acertos), 0), COALESCE(SUM(q.total), 0), "
                        + "COALESCE(SUM(r.pontuacao), 0) "
                        + "FROM resultado_atividade r JOIN tb_atividade a ON a.id = r.atividade_id "
                        + "LEFT JOIN " + PERGUNTAS_POR_ATIVIDADE + " q ON q.atividade_id = r.atividade_id "
                        + "WHERE r.concluida_em >= :inicio AND r.concluida_em < :fim"
                        + recorte.turma("a") + recorte.aluno("r.usuario_id"),
                recorte.parametros(),
                (rs, linha) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)});

        long[] desafios = jdbcComNomes.queryForObject(
                "SELECT COUNT(*), COALESCE(SUM(CASE WHEN rd.correta THEN 1 ELSE 0 END), 0), "
                        + "COALESCE(SUM(rd.pontos), 0) "
                        + "FROM tb_resposta_desafio rd "
                        + "JOIN tb_pergunta_desafio pd ON pd.id = rd.pergunta_id "
                        + "JOIN tb_desafio d ON d.id = pd.desafio_id "
                        + "WHERE rd.respondida_em >= :inicio AND rd.respondida_em < :fim"
                        + recorte.turma("d") + recorte.aluno("rd.usuario_id"),
                recorte.parametros(),
                (rs, linha) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)});

        Long publicadas = jdbcComNomes.queryForObject(
                "SELECT COUNT(*) FROM tb_atividade a WHERE a.status <> 'PENDENTE'" + recorte.turma("a"),
                recorte.parametros(), Long.class);

        return new ResumoPainel(
                totalTurmas,
                totalAlunos,
                publicadas == null ? 0 : publicadas,
                atividades[0],
                percentual(atividades[1], atividades[2]),
                atividades[3],
                desafios[0],
                percentual(desafios[1], desafios[0]),
                desafios[2]);
    }

    private List<ConclusoesDia> conclusoesPorDia(Recorte recorte) {
        Map<LocalDate, Long> porDia = new HashMap<>();
        jdbcComNomes.query("SELECT CAST(r.concluida_em AS DATE), COUNT(*) FROM resultado_atividade r "
                        + "JOIN tb_atividade a ON a.id = r.atividade_id "
                        + "WHERE r.concluida_em >= :inicio AND r.concluida_em < :fim"
                        + recorte.turma("a") + recorte.aluno("r.usuario_id")
                        + " GROUP BY CAST(r.concluida_em AS DATE)",
                recorte.parametros(),
                rs -> {
                    porDia.put(rs.getObject(1, LocalDate.class), rs.getLong(2));
                });

        return recorte.periodo().dias().stream()
                .map(dia -> new ConclusoesDia(formatar(dia), porDia.getOrDefault(dia, 0L)))
                .toList();
    }

    private List<ConclusoesDia> serieVazia(Periodo periodo) {
        return periodo.dias().stream().map(dia -> new ConclusoesDia(formatar(dia), 0L)).toList();
    }

    private List<AcertoTurma> acertoPorTurma(Recorte recorte) {
        return jdbcComNomes.query("SELECT t.nome, SUM(r.acertos), SUM(q.total) FROM resultado_atividade r "
                        + "JOIN tb_atividade a ON a.id = r.atividade_id "
                        + "JOIN tb_turma t ON t.id = a.turma_id "
                        + "JOIN " + PERGUNTAS_POR_ATIVIDADE + " q ON q.atividade_id = r.atividade_id "
                        + "WHERE r.concluida_em >= :inicio AND r.concluida_em < :fim"
                        + recorte.turma("a") + recorte.aluno("r.usuario_id")
                        + " GROUP BY t.id, t.nome ORDER BY t.nome",
                recorte.parametros(),
                (rs, linha) -> new AcertoTurma(rs.getString(1), percentual(rs.getLong(2), rs.getLong(3))));
    }

    // Uma linha por aluno do recorte, inclusive quem ainda não fez nada (para o professor perceber)
    private List<AlunoPainel> desempenhoDosAlunos(Recorte recorte, List<Opcao> alunos) {
        Map<Long, long[]> atividades = new HashMap<>();
        jdbcComNomes.query("SELECT r.usuario_id, COUNT(*), COALESCE(SUM(r.acertos), 0), "
                        + "COALESCE(SUM(q.total), 0), COALESCE(SUM(r.pontuacao), 0) FROM resultado_atividade r "
                        + "JOIN tb_atividade a ON a.id = r.atividade_id "
                        + "LEFT JOIN " + PERGUNTAS_POR_ATIVIDADE + " q ON q.atividade_id = r.atividade_id "
                        + "WHERE r.concluida_em >= :inicio AND r.concluida_em < :fim"
                        + recorte.turma("a") + recorte.aluno("r.usuario_id")
                        + " GROUP BY r.usuario_id",
                recorte.parametros(),
                rs -> {
                    atividades.put(rs.getLong(1),
                            new long[]{rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)});
                });

        Map<Long, long[]> desafios = new HashMap<>();
        jdbcComNomes.query("SELECT rd.usuario_id, COUNT(*), COALESCE(SUM(rd.pontos), 0) FROM tb_resposta_desafio rd "
                        + "JOIN tb_pergunta_desafio pd ON pd.id = rd.pergunta_id "
                        + "JOIN tb_desafio d ON d.id = pd.desafio_id "
                        + "WHERE rd.respondida_em >= :inicio AND rd.respondida_em < :fim"
                        + recorte.turma("d") + recorte.aluno("rd.usuario_id")
                        + " GROUP BY rd.usuario_id",
                recorte.parametros(),
                rs -> {
                    desafios.put(rs.getLong(1), new long[]{rs.getLong(2), rs.getLong(3)});
                });

        return alunos.stream()
                .map(a -> {
                    long[] at = atividades.getOrDefault(a.id(), new long[4]);
                    long[] de = desafios.getOrDefault(a.id(), new long[2]);
                    return new AlunoPainel(a.id(), a.nome(), at[0], percentual(at[1], at[2]), at[3], de[0], de[1]);
                })
                .sorted(Comparator.comparingLong(AlunoPainel::pontosAtividades).reversed()
                        .thenComparing(AlunoPainel::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // Filtros aplicados às consultas do painel (turma, aluno e período)
    private record Recorte(List<Long> turmas, boolean todasAsTurmas, Long aluno, Periodo periodo) {

        String turma(String alias) {
            return todasAsTurmas ? "" : " AND " + alias + ".turma_id IN (:turmas)";
        }

        String aluno(String coluna) {
            return aluno == null ? "" : " AND " + coluna + " = :aluno";
        }

        MapSqlParameterSource parametros() {
            MapSqlParameterSource parametros = new MapSqlParameterSource()
                    .addValue("inicio", periodo.inicio())
                    .addValue("fim", periodo.fim());
            if (!todasAsTurmas) {
                parametros.addValue("turmas", turmas);
            }
            if (aluno != null) {
                parametros.addValue("aluno", aluno);
            }
            return parametros;
        }
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
