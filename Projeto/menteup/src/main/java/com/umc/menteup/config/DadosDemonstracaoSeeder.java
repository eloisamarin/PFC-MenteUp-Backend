package com.umc.menteup.config;

import com.umc.menteup.config.ConteudoDemonstracao.AlternativaModelo;
import com.umc.menteup.config.ConteudoDemonstracao.AtividadeModelo;
import com.umc.menteup.config.ConteudoDemonstracao.DesafioModelo;
import com.umc.menteup.config.ConteudoDemonstracao.Materia;
import com.umc.menteup.config.ConteudoDemonstracao.PerguntaDesafioModelo;
import com.umc.menteup.config.ConteudoDemonstracao.PerguntaModelo;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.AlternativaDesafio;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.AvatarAnimal;
import com.umc.menteup.model.Desafio;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.GamificacaoUsuario;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.PerguntaDesafio;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.model.SolicitacaoTurma;
import com.umc.menteup.model.StatusAtividade;
import com.umc.menteup.model.StatusCadastro;
import com.umc.menteup.model.StatusDesafio;
import com.umc.menteup.model.TipoAtividade;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.DesafioRepository;
import com.umc.menteup.repository.GamificacaoUsuarioRepository;
import com.umc.menteup.repository.SolicitacaoTurmaRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.ConquistaService;
import com.umc.menteup.service.DocumentoLegalService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

// Cria dados fictícios para a apresentação: usuários, turmas, atividades, resultados, desafios e registros
// Só roda com DEMO_POPULAR=true e não cria nada se os dados de demonstração já existirem
// Os e-mails seguem o formato seuemail+demo-nome@dominio, assim os códigos de login chegam na sua caixa
@Component
@Order(10)
public class DadosDemonstracaoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracaoSeeder.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // Período coberto pelos dados: cerca de 5 meses, contando a partir dos primeiros cadastros
    private static final int DIAS_DESDE_O_CADASTRO = 152;
    // Os cadastros dos alunos se espalham pelos primeiros dias; as turmas são criadas logo depois
    private static final int DIAS_DE_CADASTRO_DOS_ALUNOS = 10;
    private static final int DIAS_ATE_CRIAR_AS_TURMAS = 11;
    // Primeira e última publicação de atividade (em dias atrás)
    private static final int PRIMEIRA_PUBLICACAO = 138;
    private static final int ULTIMA_PUBLICACAO = 5;
    private static final int TOTAL_ALUNOS = 270;
    private static final int PROFESSORES_APROVADOS = 28;
    private static final int PROFESSORES_PENDENTES = 2;
    private static final int ALUNOS_POR_TURMA = 24;
    // Total de alunos que entram nos desafios encerrados (cada um responde às 10 questões)
    private static final int PARTICIPACOES_EM_DESAFIOS = 96;
    private static final int SOLICITACOES_PENDENTES = 8;
    private static final int SOLICITACOES_RECUSADAS = 2;

    // Matéria (posição em ConteudoDemonstracao.MATERIAS), classe, atividades publicadas, se tem rascunho,
    // desafios encerrados, se tem desafio aguardando e o nome da turma arquivada do mesmo professor
    private record PlanoTurma(int materia, String classe, int publicadas, boolean rascunho, int desafios,
                              boolean aguardando, String arquivada) {
    }

    private static final List<PlanoTurma> TURMAS = List.of(
            new PlanoTurma(0, "1º Ano A", 5, true, 2, true, "Matemática - Reforço"),
            new PlanoTurma(0, "1º Ano B", 5, true, 2, false, null),
            new PlanoTurma(0, "2º Ano A", 4, true, 1, false, null),
            new PlanoTurma(1, "1º Ano A", 5, true, 2, false, "Língua Portuguesa - Redação"),
            new PlanoTurma(1, "1º Ano B", 5, true, 2, false, null),
            new PlanoTurma(1, "2º Ano A", 4, true, 1, true, null),
            new PlanoTurma(2, "1º Ano A", 5, true, 2, false, null),
            new PlanoTurma(2, "1º Ano B", 5, true, 2, false, null),
            new PlanoTurma(2, "2º Ano A", 4, true, 1, true, null),
            new PlanoTurma(3, "1º Ano A", 5, true, 2, false, null),
            new PlanoTurma(3, "2º Ano A", 4, true, 2, false, null),
            new PlanoTurma(4, "1º Ano B", 4, false, 2, false, null),
            new PlanoTurma(4, "2º Ano A", 5, true, 2, false, null));

    private final boolean ativo;
    private final String emailBase;
    private final String senha;
    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final AtividadeRepository atividadeRepository;
    private final DesafioRepository desafioRepository;
    private final SolicitacaoTurmaRepository solicitacaoRepository;
    private final GamificacaoUsuarioRepository gamificacaoRepository;
    private final DocumentoLegalService documentoLegalService;
    private final ConquistaService conquistaService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate jdbcComNomes;
    private final TransactionTemplate transacao;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public DadosDemonstracaoSeeder(
            @Value("${menteup.demo.popular:false}") boolean ativo,
            @Value("${menteup.demo.email-base:}") String emailBase,
            @Value("${menteup.demo.senha:}") String senha,
            UsuarioRepository usuarioRepository,
            TurmaRepository turmaRepository,
            AtividadeRepository atividadeRepository,
            DesafioRepository desafioRepository,
            SolicitacaoTurmaRepository solicitacaoRepository,
            GamificacaoUsuarioRepository gamificacaoRepository,
            DocumentoLegalService documentoLegalService,
            ConquistaService conquistaService,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc,
            PlatformTransactionManager gerenciadorDeTransacao,
            Clock clock
    ) {
        this.ativo = ativo;
        this.emailBase = emailBase;
        this.senha = senha;
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.atividadeRepository = atividadeRepository;
        this.desafioRepository = desafioRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.gamificacaoRepository = gamificacaoRepository;
        this.documentoLegalService = documentoLegalService;
        this.conquistaService = conquistaService;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.jdbcComNomes = new NamedParameterJdbcTemplate(jdbc);
        this.transacao = new TransactionTemplate(gerenciadorDeTransacao);
        this.clock = clock;
    }

    // E-mail base (ex: nome@gmail.com) separado para montar os e-mails das contas de demonstração
    record EmailsDemonstracao(String prefixo, String dominio) {

        static Optional<EmailsDemonstracao> de(String base) {
            if (base == null) {
                return Optional.empty();
            }
            String limpo = base.trim().toLowerCase(Locale.ROOT);
            int arroba = limpo.indexOf('@');
            if (arroba <= 0 || arroba != limpo.lastIndexOf('@') || arroba == limpo.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(new EmailsDemonstracao(limpo.substring(0, arroba) + "+demo-", limpo.substring(arroba)));
        }

        String email(String apelido) {
            return prefixo + apelido + dominio;
        }

        // Condição que encontra as contas de demonstração pela coluna informada
        String condicao(String coluna) {
            return "LOWER(LEFT(" + coluna + ", :tamanhoPrefixo)) = :prefixo"
                    + " AND LOWER(RIGHT(" + coluna + ", :tamanhoDominio)) = :dominio";
        }

        MapSqlParameterSource parametros() {
            return new MapSqlParameterSource()
                    .addValue("prefixo", prefixo)
                    .addValue("tamanhoPrefixo", prefixo.length())
                    .addValue("dominio", dominio)
                    .addValue("tamanhoDominio", dominio.length());
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ativo) {
            return;
        }
        Optional<EmailsDemonstracao> emails = EmailsDemonstracao.de(emailBase);
        if (emails.isEmpty()) {
            log.warn("Dados de demonstração: informe em DEMO_EMAIL_BASE um e-mail válido (ex: nome@gmail.com)");
            return;
        }
        if (senha == null || senha.length() < 8 || senha.length() > 72) {
            log.warn("Dados de demonstração: informe em DEMO_SENHA uma senha de 8 a 72 caracteres");
            return;
        }
        Integer existentes = jdbcComNomes.queryForObject(
                "SELECT COUNT(*) FROM tb_usuarios WHERE " + emails.get().condicao("usuario"),
                emails.get().parametros(), Integer.class);
        if (existentes != null && existentes > 0) {
            log.info("Dados de demonstração: já existem no banco, nada foi criado");
            return;
        }

        long inicio = System.currentTimeMillis();
        Geracao geracao = new Geracao(emails.get());
        transacao.executeWithoutResult(status -> geracao.executar());

        // As conquistas são calculadas depois de tudo gravado, como acontece no uso normal
        int conquistas = 0;
        for (Long alunoId : geracao.alunosParaConquistas) {
            try {
                conquistas += conquistaService.avaliarEmNovaTransacao(alunoId).size();
            } catch (Exception e) {
                log.warn("Dados de demonstração: falha ao verificar as conquistas do aluno {}: {}",
                        alunoId, e.getMessage());
            }
        }

        log.info("Dados de demonstração criados em {} s: {} usuários, {} turmas, {} atividades ({} rascunhos), "
                        + "{} resultados, {} desafios encerrados e {} aguardando, {} respostas de desafio, "
                        + "{} registros de log e {} conquistas",
                (System.currentTimeMillis() - inicio) / 1000, geracao.usuarios, geracao.turmas, geracao.atividades,
                geracao.rascunhos, geracao.resultados, geracao.encerrados, geracao.aguardando,
                geracao.respostasDesafio, geracao.logs.size(), conquistas);
        log.info("Dados de demonstração: entre com {} (professor de Matemática - 1º Ano A) ou {} (aluno destaque)",
                geracao.professorExemplo, geracao.alunoDestaque);
    }

    // Perfil de cada aluno: chance de acertar, de fazer as atividades e de entrar nos desafios
    private enum Perfil {
        DESTAQUE(0.92, 0.97, 0.95),
        BOM(0.78, 0.88, 0.85),
        MEDIO(0.63, 0.72, 0.7),
        BAIXO(0.45, 0.4, 0.45),
        INATIVO(0, 0, 0);

        final double acerto;
        final double engajamento;
        final double participacao;

        Perfil(double acerto, double engajamento, double participacao) {
            this.acerto = acerto;
            this.engajamento = engajamento;
            this.participacao = participacao;
        }
    }

    private record Aluno(Usuario usuario, Perfil perfil, double acerto) {
    }

    private record Distribuicao(List<List<Aluno>> ativas, List<List<Aluno>> arquivadas, List<Aluno> comUmaTurma) {
    }

    private record DesafioPlanejado(DesafioModelo modelo, Usuario professor, Turma turma, LocalDateTime inicio) {
    }

    private record AtividadeCriada(Atividade atividade, LocalDateTime publicadaEm, Usuario professor,
                                   List<Turma> turmas, boolean maisRecente) {
    }

    private record RespostaPlanejada(Long perguntaId, Long alternativaId, List<Long> sequencia, Boolean correta) {
    }

    private record ProgressoPlanejado(Long alunoId, Long atividadeId, LocalDateTime iniciadoEm,
                                      LocalDateTime atualizadoEm, List<RespostaPlanejada> respostas) {
    }

    private final class Geracao {

        private final EmailsDemonstracao emails;
        private final Random aleatorio = new Random(20261115L);
        private final LocalDateTime agora = LocalDateTime.now(clock.withZone(FUSO));
        private final LocalDate hoje = agora.toLocalDate();
        private final String hashSenha = passwordEncoder.encode(senha);

        private final List<Object[]> logs = new ArrayList<>();
        // Primeiro acesso de cada pessoa em cada dia: vira um registro de login
        private final Map<Long, Map<LocalDate, LocalDateTime>> acessos = new HashMap<>();
        private final Map<Long, Usuario> usuarioPorId = new HashMap<>();
        private final Map<Long, String> ipDoUsuario = new HashMap<>();
        private final Map<Long, Aluno> alunoPorId = new LinkedHashMap<>();
        private final Set<String> apelidosUsados = new HashSet<>();
        private final List<ProgressoPlanejado> progressos = new ArrayList<>();
        private final List<Object[]> linhasResultado = new ArrayList<>();
        private final List<Object[]> linhasRespostaDesafio = new ArrayList<>();
        private final List<Object[]> datasDasAtividades = new ArrayList<>();
        private final List<Object[]> datasDosDesafios = new ArrayList<>();
        private final List<Long> alunosParaConquistas = new ArrayList<>();

        private int usuarios;
        private int turmas;
        private int atividades;
        private int resultados;
        private int respostasDesafio;
        private int rascunhos;
        private int encerrados;
        private int aguardando;
        private String alunoDestaque = "";
        private String professorExemplo = "";

        private Geracao(EmailsDemonstracao emails) {
            this.emails = emails;
        }

        private void executar() {
            LocalDateTime cadastro = diaEHora(DIAS_DESDE_O_CADASTRO, 8, 10);

            List<Usuario> professores = new ArrayList<>();
            for (int i = 0; i < PROFESSORES_APROVADOS; i++) {
                professores.add(criarProfessor(cadastro.plusMinutes(7L * i), StatusCadastro.APROVADA));
            }
            for (int i = 0; i < PROFESSORES_PENDENTES; i++) {
                criarProfessor(agora.minusHours(20L + 26L * i), StatusCadastro.PENDENTE);
            }
            professorExemplo = professores.get(0).getUsuario();
            List<Aluno> alunos = criarAlunos(cadastro.plusHours(4));
            Distribuicao distribuicao = distribuirAlunos(alunos);

            // Cada turma ativa tem um professor; os professores restantes ainda não têm turma
            List<AtividadeCriada> criadas = new ArrayList<>();
            List<DesafioPlanejado> desafios = new ArrayList<>();
            List<Turma> turmasAtivas = new ArrayList<>();
            Map<String, Integer> turmasPorMateria = new HashMap<>();
            int proximaArquivada = 0;
            for (int t = 0; t < TURMAS.size(); t++) {
                PlanoTurma plano = TURMAS.get(t);
                Materia materia = ConteudoDemonstracao.MATERIAS.get(plano.materia());
                int ordemNaMateria = turmasPorMateria.merge(materia.nome(), 1, Integer::sum) - 1;
                Usuario professor = professores.get(t);

                Turma turma = criarTurma(materia.nome() + " - " + plano.classe(), professor,
                        alunosDe(distribuicao.ativas().get(t)), false);
                turmasAtivas.add(turma);
                Turma arquivada = null;
                if (plano.arquivada() != null) {
                    arquivada = criarTurma(plano.arquivada(), professor,
                            alunosDe(distribuicao.arquivadas().get(proximaArquivada++)), true);
                }
                criadas.addAll(criarAtividades(plano, materia, ordemNaMateria, professor, turma, arquivada));

                for (int k = 0; k < plano.desafios(); k++) {
                    int dias = plano.desafios() == 1 ? 75 : (k == 0 ? 112 : 38);
                    desafios.add(new DesafioPlanejado(materia.desafios().get(k), professor, turma,
                            diaEHora(dias - (t % 4) * 5, 9, 15)));
                }
                if (plano.aguardando()) {
                    criarDesafioAguardando(materia.desafios().get(plano.desafios()), professor, turma);
                }
            }
            for (int i = TURMAS.size(); i < professores.size(); i++) {
                registrarAcessosEsporadicos(professores.get(i));
            }

            for (AtividadeCriada criada : criadas) {
                planejarRespostas(criada);
            }
            criarDesafiosEncerrados(desafios);
            criarSolicitacoes(distribuicao.comUmaTurma(), turmasAtivas);

            // Salva tudo o que foi montado até aqui antes de gravar o restante
            entityManager.flush();
            gravarDatasOriginais();
            gravarProgressoEResultados();
            jdbc.batchUpdate("INSERT INTO tb_resposta_desafio "
                            + "(pergunta_id, alternativa_id, usuario_id, correta, pontos, respondida_em) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", linhasRespostaDesafio,
                    new int[]{Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BOOLEAN, Types.INTEGER, Types.TIMESTAMP});
            respostasDesafio = linhasRespostaDesafio.size();
            criarSequenciasDeAcesso();
            entityManager.flush();
            gravarLogs();
        }

        // Usuários

        private Usuario criarUsuario(String nome, String apelido, String tipo, LocalDateTime criadoEm,
                                     StatusCadastro status) {
            Usuario usuario = new Usuario();
            usuario.setNomeUsuario(nome);
            usuario.setTipoUsuario(tipo);
            usuario.setUsuario(emails.email(apelido));
            usuario.setSenha(hashSenha);
            usuario.setAvatar(aleatorio.nextDouble() < 0.85 ? sortear(AvatarAnimal.values()) : null);
            usuario.setCriadoEm(criadoEm);
            usuario.setTermoAceitoEm(criadoEm);
            usuario.setStatusCadastro(status);
            if (status != null) {
                usuario.setStatusAlteradoEm(status == StatusCadastro.APROVADA ? criadoEm.plusHours(3) : criadoEm);
            }
            Usuario salvo = usuarioRepository.save(usuario);
            documentoLegalService.registrarAceiteDasVigentes(salvo, criadoEm);
            usuarioPorId.put(salvo.getId(), salvo);
            ipDoUsuario.put(salvo.getId(), (aleatorio.nextBoolean() ? "198.51.100." : "203.0.113.")
                    + (1 + aleatorio.nextInt(250)));
            usuarios++;

            registrar(salvo, AcaoLog.CADASTRAR_USUARIO, EntidadeLog.USUARIO, salvo.getId(), criadoEm,
                    ResultadoLog.SUCESSO,
                    status == StatusCadastro.PENDENTE
                            ? "Pedido de cadastro de professor do usuário #" + salvo.getId() + " enviado."
                            : "Usuário #" + salvo.getId() + " cadastrado.",
                    "{\"tipo\":\"" + tipo + "\"}");
            registrar(salvo, AcaoLog.ACEITAR_TERMO, EntidadeLog.USUARIO, salvo.getId(), criadoEm.plusSeconds(5),
                    ResultadoLog.SUCESSO, "Usuário #" + salvo.getId() + " aceitou o Termo e a Política.", null);
            return salvo;
        }

        // Nome e apelido únicos entre todas as contas da demonstração
        private String[] novoNome() {
            String nome;
            String apelido;
            do {
                String primeiro = sortear(ConteudoDemonstracao.PRIMEIROS_NOMES);
                String sobrenome = sortear(ConteudoDemonstracao.SOBRENOMES);
                nome = primeiro + " " + sobrenome;
                apelido = semAcentos(primeiro + "." + sobrenome);
            } while (!apelidosUsados.add(apelido));
            return new String[]{nome, apelido};
        }

        private Usuario criarProfessor(LocalDateTime criadoEm, StatusCadastro status) {
            String[] nome = novoNome();
            return criarUsuario(nome[0], "prof." + nome[1], TipoUsuario.PROFESSOR, criadoEm, status);
        }

        // Mistura de perfis para o ranking ter destaques, alunos medianos, com dificuldade e inativos
        private List<Aluno> criarAlunos(LocalDateTime cadastro) {
            List<Perfil> perfis = new ArrayList<>();
            adicionar(perfis, Perfil.DESTAQUE, TOTAL_ALUNOS * 8 / 100);
            adicionar(perfis, Perfil.BOM, TOTAL_ALUNOS * 25 / 100);
            adicionar(perfis, Perfil.BAIXO, TOTAL_ALUNOS * 21 / 100);
            adicionar(perfis, Perfil.INATIVO, TOTAL_ALUNOS * 9 / 100);
            adicionar(perfis, Perfil.MEDIO, TOTAL_ALUNOS - perfis.size());
            Collections.shuffle(perfis, aleatorio);

            List<Aluno> alunos = new ArrayList<>();
            for (int i = 0; i < perfis.size(); i++) {
                alunos.add(criarAluno(perfis.get(i),
                        cadastro.plusMinutes(DIAS_DE_CADASTRO_DOS_ALUNOS * 24L * 60L * i / perfis.size())));
            }
            return alunos;
        }

        private Aluno criarAluno(Perfil perfil, LocalDateTime criadoEm) {
            String[] nome = novoNome();
            Usuario usuario = criarUsuario(nome[0], nome[1], TipoUsuario.ALUNO, criadoEm, null);
            double acerto = perfil == Perfil.INATIVO ? 0
                    : Math.max(0.2, Math.min(0.98, perfil.acerto + (aleatorio.nextDouble() - 0.5) * 0.1));
            Aluno aluno = new Aluno(usuario, perfil, acerto);
            alunoPorId.put(usuario.getId(), aluno);
            alunosParaConquistas.add(usuario.getId());
            if (perfil == Perfil.DESTAQUE && alunoDestaque.isEmpty()) {
                alunoDestaque = usuario.getUsuario();
            }
            return aluno;
        }

        // Todo aluno tem uma turma ativa; um terço deles também está em uma segunda turma (ativa ou arquivada)
        private Distribuicao distribuirAlunos(List<Aluno> alunos) {
            int totalAtivas = TURMAS.size();
            int totalArquivadas = (int) TURMAS.stream().filter(p -> p.arquivada() != null).count();
            List<Aluno> embaralhados = new ArrayList<>(alunos);
            Collections.shuffle(embaralhados, aleatorio);

            List<List<Aluno>> ativas = new ArrayList<>();
            for (int t = 0; t < totalAtivas; t++) {
                ativas.add(new ArrayList<>());
            }
            Map<Aluno, Integer> primeiraTurma = new HashMap<>();
            for (int i = 0; i < embaralhados.size(); i++) {
                ativas.get(i % totalAtivas).add(embaralhados.get(i));
                primeiraTurma.put(embaralhados.get(i), i % totalAtivas);
            }

            List<Aluno> comDuasTurmas = new ArrayList<>(embaralhados.subList(0, embaralhados.size() / 3));
            Collections.shuffle(comDuasTurmas, aleatorio);
            List<List<Aluno>> arquivadas = new ArrayList<>();
            for (int a = 0; a < totalArquivadas; a++) {
                List<Aluno> lista = new ArrayList<>();
                while (lista.size() < ALUNOS_POR_TURMA && !comDuasTurmas.isEmpty()) {
                    lista.add(comDuasTurmas.remove(comDuasTurmas.size() - 1));
                }
                arquivadas.add(lista);
            }

            // Os demais vão para uma segunda turma ativa, de preferência de outra matéria
            Map<Aluno, Integer> segundaAtiva = new LinkedHashMap<>();
            for (int t = 0; t < totalAtivas && !comDuasTurmas.isEmpty(); t++) {
                List<Aluno> turma = ativas.get(t);
                int materia = TURMAS.get(t).materia();
                while (turma.size() < ALUNOS_POR_TURMA && !comDuasTurmas.isEmpty()) {
                    final int atual = t;
                    Aluno escolhido = comDuasTurmas.stream()
                            .filter(a -> primeiraTurma.get(a) != atual
                                    && TURMAS.get(primeiraTurma.get(a)).materia() != materia)
                            .findFirst()
                            .orElse(comDuasTurmas.stream().filter(a -> primeiraTurma.get(a) != atual)
                                    .findFirst().orElse(null));
                    if (escolhido == null) {
                        break;
                    }
                    comDuasTurmas.remove(escolhido);
                    turma.add(escolhido);
                    segundaAtiva.put(escolhido, t);
                }
            }
            // Quem sobrou só cabe na própria turma: troca de lugar com alguém já colocado em outra turma
            for (Aluno sobra : new ArrayList<>(comDuasTurmas)) {
                int destino = primeiraTurma.get(sobra);
                for (Map.Entry<Aluno, Integer> colocado : segundaAtiva.entrySet()) {
                    Aluno outro = colocado.getKey();
                    int turmaDoOutro = colocado.getValue();
                    if (turmaDoOutro != destino && primeiraTurma.get(outro) != destino
                            && ativas.get(destino).size() < ALUNOS_POR_TURMA) {
                        ativas.get(turmaDoOutro).remove(outro);
                        ativas.get(destino).add(outro);
                        ativas.get(turmaDoOutro).add(sobra);
                        segundaAtiva.put(outro, destino);
                        segundaAtiva.put(sobra, turmaDoOutro);
                        comDuasTurmas.remove(sobra);
                        break;
                    }
                }
            }

            List<Aluno> comUmaTurma = new ArrayList<>();
            Map<Aluno, Integer> turmasDoAluno = new HashMap<>();
            ativas.forEach(turma -> turma.forEach(a -> turmasDoAluno.merge(a, 1, Integer::sum)));
            arquivadas.forEach(turma -> turma.forEach(a -> turmasDoAluno.merge(a, 1, Integer::sum)));
            for (Aluno aluno : alunos) {
                if (turmasDoAluno.getOrDefault(aluno, 0) == 1) {
                    comUmaTurma.add(aluno);
                }
            }
            return new Distribuicao(ativas, arquivadas, comUmaTurma);
        }

        // Professor aprovado que ainda não tem turma: entra no sistema de vez em quando
        private void registrarAcessosEsporadicos(Usuario professor) {
            for (int dias = DIAS_DESDE_O_CADASTRO - 2; dias >= 1; dias -= 5 + aleatorio.nextInt(8)) {
                registrarLogin(professor, diaEHora(dias, 8, 21));
            }
        }

        // Turmas e atividades

        private Turma criarTurma(String nome, Usuario professor, List<Usuario> alunos, boolean arquivada) {
            Turma turma = new Turma();
            turma.setNome(nome);
            turma.setProfessor(professor);
            turma.setAlunos(new ArrayList<>(alunos));
            turma.setArquivada(arquivada);
            Turma salva = turmaRepository.save(turma);
            turmas++;

            LocalDateTime criadaEm = diaEHora(DIAS_DESDE_O_CADASTRO - DIAS_ATE_CRIAR_AS_TURMAS, 8, 12);
            registrarLogin(professor, criadaEm.minusMinutes(5));
            registrar(professor, AcaoLog.CRIAR_TURMA, EntidadeLog.TURMA, salva.getId(), criadaEm, ResultadoLog.SUCESSO,
                    "Turma \"" + nome + "\" criada.", null);
            LocalDateTime momento = criadaEm;
            for (Usuario aluno : alunos) {
                momento = momento.plusSeconds(20 + aleatorio.nextInt(40));
                registrar(professor, AcaoLog.ADICIONAR_ALUNO_TURMA, EntidadeLog.TURMA, salva.getId(), momento,
                        ResultadoLog.SUCESSO,
                        "Aluno #" + aluno.getId() + " vinculado à turma \"" + nome + "\".", null);
            }
            return salva;
        }

        // Atividades publicadas ao longo das últimas semanas e, quando previsto, um rascunho
        // As duas primeiras atividades também ficam na turma arquivada do professor
        private List<AtividadeCriada> criarAtividades(PlanoTurma plano, Materia materia, int ordemNaMateria,
                                                      Usuario professor, Turma turma, Turma arquivada) {
            List<AtividadeCriada> criadas = new ArrayList<>();
            List<AtividadeModelo> modelos = materia.atividades();
            int total = plano.publicadas() + (plano.rascunho() ? 1 : 0);
            for (int i = 0; i < total; i++) {
                AtividadeModelo modelo = modelos.get((ordemNaMateria * 2 + i) % modelos.size());
                boolean rascunho = i >= plano.publicadas();

                List<Turma> destino = new ArrayList<>();
                destino.add(turma);
                if (arquivada != null && i < 2 && !rascunho) {
                    destino.add(arquivada);
                }

                Atividade atividade = new Atividade();
                atividade.setTitulo(modelo.titulo());
                atividade.setDescricao(modelo.descricao());
                atividade.setTipo(modelo.tipo());
                atividade.setPontuacao(modelo.pontuacao());
                atividade.setStatus(rascunho ? StatusAtividade.PENDENTE
                        : i < plano.publicadas() - 3 ? StatusAtividade.CONCLUIDO : StatusAtividade.EM_ANDAMENTO);
                atividade.setTurmas(destino);
                atividade.setUsuario(professor);
                for (PerguntaModelo perguntaModelo : modelo.perguntas()) {
                    atividade.getPerguntas().add(montarPergunta(atividade, perguntaModelo));
                }
                Atividade salva = atividadeRepository.save(atividade);
                atividades++;
                if (rascunho) {
                    rascunhos++;
                }

                LocalDateTime publicadaEm = null;
                if (!rascunho) {
                    int dias = (int) Math.round(PRIMEIRA_PUBLICACAO - i * (PRIMEIRA_PUBLICACAO - ULTIMA_PUBLICACAO)
                            / (double) Math.max(1, plano.publicadas() - 1)) - aleatorio.nextInt(2);
                    publicadaEm = diaEHora(dias, 7, 10);
                }
                LocalDateTime criadaEm = rascunho ? agora.minusHours(10L + aleatorio.nextInt(60))
                        : publicadaEm.minusHours(14);
                datasDasAtividades.add(new Object[]{
                        Timestamp.valueOf(criadaEm), Timestamp.valueOf(publicadaEm == null ? criadaEm : publicadaEm),
                        salva.getId()});

                registrarLogin(professor, criadaEm.minusMinutes(3));
                registrar(professor, AcaoLog.CRIAR_ATIVIDADE, EntidadeLog.ATIVIDADE, salva.getId(), criadaEm,
                        ResultadoLog.SUCESSO, "Atividade \"" + modelo.titulo() + "\" criada.",
                        "{\"tipo\":\"" + modelo.tipo().name() + "\"}");
                LocalDateTime momento = criadaEm;
                for (Pergunta pergunta : salva.getPerguntas()) {
                    momento = momento.plusMinutes(1 + aleatorio.nextInt(3));
                    registrar(professor, AcaoLog.CRIAR_QUESTAO, EntidadeLog.QUESTAO, pergunta.getId(), momento,
                            ResultadoLog.SUCESSO,
                            "Questão criada na atividade \"" + modelo.titulo() + "\".", null);
                }
                if (publicadaEm != null) {
                    registrarLogin(professor, publicadaEm.minusMinutes(2));
                    registrar(professor, AcaoLog.PUBLICAR_ATIVIDADE, EntidadeLog.ATIVIDADE, salva.getId(),
                            publicadaEm, ResultadoLog.SUCESSO,
                            "Atividade \"" + modelo.titulo() + "\" publicada.", null);
                    criadas.add(new AtividadeCriada(salva, publicadaEm, professor, destino,
                            i == plano.publicadas() - 1));
                }
            }
            return criadas;
        }

        private Pergunta montarPergunta(Atividade atividade, PerguntaModelo modelo) {
            Pergunta pergunta = new Pergunta();
            pergunta.setEnunciado(modelo.enunciado());
            pergunta.setAtividade(atividade);
            List<AlternativaModelo> alternativas = new ArrayList<>(modelo.alternativas());
            // No quiz a alternativa certa não pode ficar sempre na mesma posição
            if (atividade.getTipo() == TipoAtividade.QUIZ) {
                Collections.shuffle(alternativas, aleatorio);
            }
            for (AlternativaModelo alternativaModelo : alternativas) {
                Alternativa alternativa = new Alternativa();
                alternativa.setTexto(alternativaModelo.texto());
                alternativa.setCorreta(alternativaModelo.correta());
                alternativa.setOrdem(alternativaModelo.ordem());
                alternativa.setPergunta(pergunta);
                pergunta.getAlternativas().add(alternativa);
            }
            return pergunta;
        }

        // Respostas dos alunos às atividades

        private void planejarRespostas(AtividadeCriada criada) {
            Atividade atividade = criada.atividade();
            Set<Long> jaPlanejados = new HashSet<>();
            // Algumas questões são mais difíceis que outras
            List<Double> dificuldade = new ArrayList<>();
            for (int i = 0; i < atividade.getPerguntas().size(); i++) {
                dificuldade.add(aleatorio.nextDouble() * 0.25 - 0.08);
            }

            for (Turma turma : criada.turmas()) {
                for (Usuario usuario : turma.getAlunos()) {
                    Aluno aluno = alunoPorId.get(usuario.getId());
                    if (aluno == null || !jaPlanejados.add(usuario.getId())) {
                        continue;
                    }
                    if (aleatorio.nextDouble() < aluno.perfil().engajamento) {
                        planejarConclusao(criada, aluno, dificuldade);
                    } else if (criada.maisRecente() && aluno.perfil() != Perfil.INATIVO && aleatorio.nextDouble() < 0.3) {
                        planejarEmAndamento(criada, aluno);
                    }
                }
            }
        }

        private void planejarConclusao(AtividadeCriada criada, Aluno aluno, List<Double> dificuldade) {
            Atividade atividade = criada.atividade();
            int atraso = aluno.perfil() == Perfil.DESTAQUE ? aleatorio.nextInt(2) : (int) Math.min(6, Math.floor(
                    -Math.log(1 - aleatorio.nextDouble()) * 1.8));
            LocalDateTime concluidaEm = criada.publicadaEm().toLocalDate().plusDays(atraso)
                    .atTime(LocalTime.of(8 + aleatorio.nextInt(14), aleatorio.nextInt(60), aleatorio.nextInt(60)));
            if (!concluidaEm.isAfter(criada.publicadaEm())) {
                concluidaEm = criada.publicadaEm().plusMinutes(30 + aleatorio.nextInt(180));
            }
            if (concluidaEm.isAfter(agora.minusMinutes(30))) {
                return;
            }
            LocalDateTime iniciadoEm = concluidaEm.minusMinutes(3 + aleatorio.nextInt(13));

            List<RespostaPlanejada> respostas = new ArrayList<>();
            int acertos = 0;
            List<Pergunta> perguntas = atividade.getPerguntas();
            for (int i = 0; i < perguntas.size(); i++) {
                boolean correta = aleatorio.nextDouble() < aluno.acerto() - dificuldade.get(i);
                respostas.add(montarResposta(atividade, perguntas.get(i), correta));
                if (correta) {
                    acertos++;
                }
            }
            int pontuacao = (int) Math.round((double) acertos * atividade.getPontuacao() / perguntas.size());
            Long alunoId = aluno.usuario().getId();
            progressos.add(new ProgressoPlanejado(alunoId, atividade.getId(), iniciadoEm, concluidaEm, respostas));

            String comentario = null;
            LocalDateTime comentadoEm = null;
            Long comentadoPor = null;
            LocalDateTime possivelComentario = concluidaEm.plusHours(18 + aleatorio.nextInt(30));
            if (aleatorio.nextDouble() < 0.12 && possivelComentario.isBefore(agora)) {
                comentario = sortear(ConteudoDemonstracao.COMENTARIOS);
                comentadoEm = possivelComentario;
                comentadoPor = criada.professor().getId();
                registrarLogin(criada.professor(), comentadoEm.minusMinutes(4));
                registrar(criada.professor(), AcaoLog.COMENTAR_RESULTADO, EntidadeLog.ATIVIDADE, atividade.getId(),
                        comentadoEm, ResultadoLog.SUCESSO,
                        "Comentário feito na atividade \"" + atividade.getTitulo() + "\".",
                        "{\"alunoId\":" + alunoId + "}");
            }
            linhasResultado.add(new Object[]{alunoId, atividade.getId(), acertos, pontuacao,
                    Timestamp.valueOf(concluidaEm), comentario,
                    comentadoEm == null ? null : Timestamp.valueOf(comentadoEm), comentadoPor});
            resultados++;

            registrarLogin(aluno.usuario(), iniciadoEm.minusMinutes(1 + aleatorio.nextInt(8)));
            registrar(aluno.usuario(), AcaoLog.INICIAR_ATIVIDADE, EntidadeLog.ATIVIDADE, atividade.getId(), iniciadoEm,
                    ResultadoLog.SUCESSO,
                    "Aluno #" + alunoId + " iniciou a atividade \"" + atividade.getTitulo() + "\".",
                    "{\"atividadeId\":" + atividade.getId() + "}");
            registrar(aluno.usuario(), AcaoLog.CONCLUIR_ATIVIDADE, EntidadeLog.ATIVIDADE, atividade.getId(),
                    concluidaEm, ResultadoLog.SUCESSO,
                    "Aluno #" + alunoId + " concluiu a atividade \"" + atividade.getTitulo() + "\".",
                    "{\"acertos\":" + acertos + ",\"totalQuestoes\":" + perguntas.size()
                            + ",\"pontuacao\":" + pontuacao + "}");
        }

        // Aluno que começou a atividade mais recente e ainda não terminou
        private void planejarEmAndamento(AtividadeCriada criada, Aluno aluno) {
            Atividade atividade = criada.atividade();
            LocalDateTime iniciadoEm = criada.publicadaEm().plusHours(6 + aleatorio.nextInt(72));
            if (iniciadoEm.isAfter(agora.minusHours(1))) {
                return;
            }
            List<Pergunta> perguntas = atividade.getPerguntas();
            int respondidas = 1 + aleatorio.nextInt(perguntas.size() - 1);
            List<RespostaPlanejada> respostas = new ArrayList<>();
            for (int i = 0; i < respondidas; i++) {
                RespostaPlanejada resposta = montarResposta(atividade, perguntas.get(i),
                        aleatorio.nextDouble() < aluno.acerto());
                respostas.add(new RespostaPlanejada(resposta.perguntaId(), resposta.alternativaId(),
                        resposta.sequencia(), null));
            }
            progressos.add(new ProgressoPlanejado(aluno.usuario().getId(), atividade.getId(), iniciadoEm,
                    iniciadoEm.plusMinutes(2L * respondidas), respostas));
            registrarLogin(aluno.usuario(), iniciadoEm.minusMinutes(2));
            registrar(aluno.usuario(), AcaoLog.INICIAR_ATIVIDADE, EntidadeLog.ATIVIDADE, atividade.getId(), iniciadoEm,
                    ResultadoLog.SUCESSO,
                    "Aluno #" + aluno.usuario().getId() + " iniciou a atividade \"" + atividade.getTitulo() + "\".",
                    "{\"atividadeId\":" + atividade.getId() + "}");
        }

        // Monta a resposta no formato de cada tipo de atividade, certa ou errada
        private RespostaPlanejada montarResposta(Atividade atividade, Pergunta pergunta, boolean correta) {
            List<Alternativa> alternativas = pergunta.getAlternativas();
            switch (atividade.getTipo()) {
                case ORDENACAO -> {
                    List<Alternativa> certa = new ArrayList<>(alternativas);
                    certa.sort(Comparator.comparing(Alternativa::getOrdem));
                    if (!correta) {
                        int i = aleatorio.nextInt(certa.size() - 1);
                        Collections.swap(certa, i, i + 1);
                    }
                    return new RespostaPlanejada(pergunta.getId(), null, ids(certa), correta);
                }
                case LACUNAS -> {
                    List<Alternativa> certa = new ArrayList<>(alternativas.stream()
                            .filter(a -> Boolean.TRUE.equals(a.getCorreta()))
                            .sorted(Comparator.comparing(Alternativa::getOrdem))
                            .toList());
                    List<Alternativa> extras = alternativas.stream()
                            .filter(a -> !Boolean.TRUE.equals(a.getCorreta()))
                            .toList();
                    if (!correta) {
                        certa.set(aleatorio.nextInt(certa.size()), sortear(extras));
                    }
                    return new RespostaPlanejada(pergunta.getId(), null, ids(certa), correta);
                }
                case ASSOCIACAO -> {
                    Alternativa escolhida = alternativas.get(0);
                    if (!correta) {
                        List<Pergunta> outras = atividade.getPerguntas().stream()
                                .filter(p -> !p.getId().equals(pergunta.getId()))
                                .toList();
                        escolhida = sortear(outras).getAlternativas().get(0);
                    }
                    return new RespostaPlanejada(pergunta.getId(), escolhida.getId(), List.of(), correta);
                }
                default -> {
                    List<Alternativa> candidatas = alternativas.stream()
                            .filter(a -> Boolean.TRUE.equals(a.getCorreta()) == correta)
                            .toList();
                    return new RespostaPlanejada(pergunta.getId(), sortear(candidatas).getId(), List.of(), correta);
                }
            }
        }

        // Desafios

        // O total de participações é fixo; cada participante responde a todas as questões do desafio
        private void criarDesafiosEncerrados(List<DesafioPlanejado> planejados) {
            int base = PARTICIPACOES_EM_DESAFIOS / planejados.size();
            int sobra = PARTICIPACOES_EM_DESAFIOS % planejados.size();
            List<Integer> comUmAMais = new ArrayList<>();
            for (int i = 0; i < planejados.size(); i++) {
                comUmAMais.add(i);
            }
            Collections.shuffle(comUmAMais, aleatorio);
            Set<Integer> recebemMais = new HashSet<>(comUmAMais.subList(0, sobra));
            for (int i = 0; i < planejados.size(); i++) {
                DesafioPlanejado planejado = planejados.get(i);
                criarDesafioEncerrado(planejado.modelo(), planejado.professor(), planejado.turma(),
                        planejado.inicio(), base + (recebemMais.contains(i) ? 1 : 0));
            }
        }

        private Desafio montarDesafio(DesafioModelo modelo, Usuario professor, Turma turma, StatusDesafio status) {
            Desafio desafio = new Desafio();
            desafio.setTitulo(modelo.titulo());
            desafio.setDescricao(modelo.descricao());
            desafio.setStatus(status);
            desafio.setTurma(turma);
            desafio.setUsuario(professor);
            for (int i = 0; i < modelo.perguntas().size(); i++) {
                PerguntaDesafioModelo perguntaModelo = modelo.perguntas().get(i);
                PerguntaDesafio pergunta = new PerguntaDesafio();
                pergunta.setEnunciado(perguntaModelo.enunciado());
                pergunta.setTempoSegundos(perguntaModelo.tempoSegundos());
                pergunta.setOrdem(i);
                pergunta.setDesafio(desafio);
                List<AlternativaDesafio> alternativas = new ArrayList<>();
                alternativas.add(alternativaDesafio(pergunta, perguntaModelo.correta(), true));
                for (String errada : perguntaModelo.erradas()) {
                    alternativas.add(alternativaDesafio(pergunta, errada, false));
                }
                Collections.shuffle(alternativas, aleatorio);
                pergunta.getAlternativas().addAll(alternativas);
                desafio.getPerguntas().add(pergunta);
            }
            return desafioRepository.save(desafio);
        }

        private AlternativaDesafio alternativaDesafio(PerguntaDesafio pergunta, String texto, boolean correta) {
            AlternativaDesafio alternativa = new AlternativaDesafio();
            alternativa.setTexto(texto);
            alternativa.setCorreta(correta);
            alternativa.setPergunta(pergunta);
            return alternativa;
        }

        private void criarDesafioEncerrado(DesafioModelo modelo, Usuario professor, Turma turma, LocalDateTime inicio,
                                           int quantidadeDeParticipantes) {
            Desafio desafio = montarDesafio(modelo, professor, turma, StatusDesafio.ENCERRADO);
            encerrados++;
            LocalDateTime criadoEm = inicio.minusDays(1).minusHours(2);

            // Sorteio que favorece os alunos mais participativos
            List<Aluno> candidatos = new ArrayList<>();
            Map<Aluno, Double> sorteio = new HashMap<>();
            for (Usuario usuario : turma.getAlunos()) {
                Aluno aluno = alunoPorId.get(usuario.getId());
                if (aluno != null && aluno.perfil() != Perfil.INATIVO) {
                    candidatos.add(aluno);
                    sorteio.put(aluno, Math.pow(aleatorio.nextDouble(), 1.0 / aluno.perfil().participacao));
                }
            }
            candidatos.sort(Comparator.comparing(sorteio::get).reversed());
            List<Aluno> participantes = candidatos.subList(0, Math.min(quantidadeDeParticipantes, candidatos.size()));
            for (Aluno aluno : participantes) {
                registrarLogin(aluno.usuario(), inicio.minusMinutes(1 + aleatorio.nextInt(4)));
            }

            LocalDateTime aberturaDaPergunta = inicio;
            for (PerguntaDesafio pergunta : desafio.getPerguntas()) {
                long tempoMs = pergunta.getTempoSegundos() * 1000L;
                AlternativaDesafio certa = pergunta.getAlternativas().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getCorreta())).findFirst().orElseThrow();
                List<AlternativaDesafio> erradas = pergunta.getAlternativas().stream()
                        .filter(a -> !Boolean.TRUE.equals(a.getCorreta())).toList();
                for (Aluno aluno : participantes) {
                    double rapidez = aluno.perfil() == Perfil.DESTAQUE
                            ? 0.1 + aleatorio.nextDouble() * 0.4
                            : 0.25 + aleatorio.nextDouble() * 0.7;
                    long demoraMs = Math.round(tempoMs * rapidez);
                    boolean correta = aleatorio.nextDouble() < aluno.acerto();
                    int pontos = correta ? 25 + (int) Math.round(50.0 * (tempoMs - demoraMs) / tempoMs) : 0;
                    LocalDateTime respondidaEm = aberturaDaPergunta.plusNanos(demoraMs * 1_000_000L);
                    linhasRespostaDesafio.add(new Object[]{pergunta.getId(),
                            (correta ? certa : sortear(erradas)).getId(), aluno.usuario().getId(), correta, pontos,
                            Timestamp.valueOf(respondidaEm)});
                    registrar(aluno.usuario(), AcaoLog.RESPONDER_DESAFIO, EntidadeLog.DESAFIO, desafio.getId(),
                            respondidaEm, ResultadoLog.SUCESSO,
                            "Aluno respondeu uma pergunta do desafio: \"" + desafio.getTitulo() + "\".",
                            "{\"titulo\":\"" + desafio.getTitulo() + "\",\"turmaId\":" + turma.getId() + "}");
                }
                aberturaDaPergunta = aberturaDaPergunta.plusSeconds(pergunta.getTempoSegundos() + 12L);
            }
            LocalDateTime fim = aberturaDaPergunta;
            datasDosDesafios.add(new Object[]{Timestamp.valueOf(criadoEm), Timestamp.valueOf(fim), desafio.getId()});

            String dados = "{\"titulo\":\"" + desafio.getTitulo() + "\",\"turmaId\":" + turma.getId() + "}";
            registrarLogin(professor, criadoEm.minusMinutes(3));
            registrar(professor, AcaoLog.CRIAR_DESAFIO, EntidadeLog.DESAFIO, desafio.getId(), criadoEm,
                    ResultadoLog.SUCESSO, "Desafio criado: \"" + desafio.getTitulo() + "\".", dados);
            registrarLogin(professor, inicio.minusMinutes(6));
            registrar(professor, AcaoLog.INICIAR_DESAFIO, EntidadeLog.DESAFIO, desafio.getId(), inicio,
                    ResultadoLog.SUCESSO, "Desafio iniciado: \"" + desafio.getTitulo() + "\".", dados);
            registrar(professor, AcaoLog.ENCERRAR_DESAFIO, EntidadeLog.DESAFIO, desafio.getId(), fim,
                    ResultadoLog.SUCESSO, "Desafio encerrado: \"" + desafio.getTitulo() + "\".", dados);
        }

        // Desafio deixado aguardando para ser iniciado durante a apresentação
        private void criarDesafioAguardando(DesafioModelo modelo, Usuario professor, Turma turma) {
            Desafio desafio = montarDesafio(modelo, professor, turma, StatusDesafio.AGUARDANDO);
            aguardando++;
            LocalDateTime criadoEm = agora.minusHours(10L + aleatorio.nextInt(30));
            datasDosDesafios.add(new Object[]{Timestamp.valueOf(criadoEm), Timestamp.valueOf(criadoEm), desafio.getId()});
            registrarLogin(professor, criadoEm.minusMinutes(3));
            registrar(professor, AcaoLog.CRIAR_DESAFIO, EntidadeLog.DESAFIO, desafio.getId(), criadoEm,
                    ResultadoLog.SUCESSO, "Desafio criado: \"" + desafio.getTitulo() + "\".",
                    "{\"titulo\":\"" + desafio.getTitulo() + "\",\"turmaId\":" + turma.getId() + "}");
        }

        // Pedidos de alunos para entrar em mais uma turma: a maioria aguardando o professor
        private void criarSolicitacoes(List<Aluno> comUmaTurma, List<Turma> turmasAtivas) {
            List<Aluno> candidatos = new ArrayList<>(comUmaTurma.stream()
                    .filter(a -> a.perfil() != Perfil.INATIVO).toList());
            Collections.shuffle(candidatos, aleatorio);
            int total = Math.min(SOLICITACOES_PENDENTES + SOLICITACOES_RECUSADAS, candidatos.size());
            for (int i = 0; i < total; i++) {
                Usuario aluno = candidatos.get(i).usuario();
                List<Turma> opcoes = turmasAtivas.stream()
                        .filter(t -> t.getAlunos().stream().noneMatch(a -> a.getId().equals(aluno.getId())))
                        .toList();
                Turma turma = opcoes.get(aleatorio.nextInt(opcoes.size()));
                SolicitacaoTurma solicitacao = new SolicitacaoTurma(turma, aluno, SolicitacaoTurma.Tipo.ENTRADA);
                LocalDateTime criadaEm = agora.minusHours(5L + 7L * i);
                solicitacao.setCriadaEm(criadaEm);
                if (i >= SOLICITACOES_PENDENTES) {
                    solicitacao.setStatus(SolicitacaoTurma.Status.RECUSADA);
                    solicitacao.setRespondidaEm(criadaEm.plusHours(3));
                    solicitacao.setRespondidaPor(turma.getProfessor());
                }
                solicitacaoRepository.save(solicitacao);
                registrarLogin(aluno, criadaEm.minusMinutes(2));
            }
        }

        // Gravação em bloco, mais rápida quando são muitas linhas

        // As datas de criação são preenchidas automaticamente, por isso são corrigidas depois
        private void gravarDatasOriginais() {
            jdbc.batchUpdate("UPDATE tb_atividade SET data_criacao = ?, data_atualizacao = ? WHERE id = ?",
                    datasDasAtividades);
            jdbc.batchUpdate("UPDATE tb_desafio SET data_criacao = ?, data_atualizacao = ? WHERE id = ?",
                    datasDosDesafios);
        }

        private void gravarProgressoEResultados() {
            List<Object[]> linhasProgresso = progressos.stream()
                    .map(p -> new Object[]{p.alunoId(), p.atividadeId(),
                            Timestamp.valueOf(p.iniciadoEm()), Timestamp.valueOf(p.atualizadoEm())})
                    .toList();
            jdbc.batchUpdate("INSERT INTO progresso_atividade (usuario_id, atividade_id, iniciado_em, atualizado_em) "
                    + "VALUES (?, ?, ?, ?)", linhasProgresso);

            MapSqlParameterSource alunos = new MapSqlParameterSource("ids", alunoPorId.keySet());
            Map<String, Long> progressoPorChave = new HashMap<>();
            jdbcComNomes.query("SELECT id, usuario_id, atividade_id FROM progresso_atividade WHERE usuario_id IN (:ids)",
                    alunos, (RowCallbackHandler) rs -> {
                        progressoPorChave.put(rs.getLong(2) + ":" + rs.getLong(3), rs.getLong(1));
                    });

            List<Object[]> linhasResposta = new ArrayList<>();
            for (ProgressoPlanejado progresso : progressos) {
                Long progressoId = progressoPorChave.get(progresso.alunoId() + ":" + progresso.atividadeId());
                for (RespostaPlanejada resposta : progresso.respostas()) {
                    linhasResposta.add(new Object[]{progressoId, resposta.perguntaId(), resposta.alternativaId(),
                            resposta.correta()});
                }
            }
            jdbc.batchUpdate("INSERT INTO resposta_atividade (progresso_id, pergunta_id, alternativa_id, correta) "
                    + "VALUES (?, ?, ?, ?)", linhasResposta,
                    new int[]{Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BOOLEAN});

            Map<String, Long> respostaPorChave = new HashMap<>();
            jdbcComNomes.query("SELECT r.id, r.progresso_id, r.pergunta_id FROM resposta_atividade r "
                            + "JOIN progresso_atividade p ON p.id = r.progresso_id WHERE p.usuario_id IN (:ids)",
                    alunos, (RowCallbackHandler) rs -> {
                        respostaPorChave.put(rs.getLong(2) + ":" + rs.getLong(3), rs.getLong(1));
                    });

            List<Object[]> linhasSequencia = new ArrayList<>();
            for (ProgressoPlanejado progresso : progressos) {
                Long progressoId = progressoPorChave.get(progresso.alunoId() + ":" + progresso.atividadeId());
                for (RespostaPlanejada resposta : progresso.respostas()) {
                    Long respostaId = respostaPorChave.get(progressoId + ":" + resposta.perguntaId());
                    for (int posicao = 0; posicao < resposta.sequencia().size(); posicao++) {
                        linhasSequencia.add(new Object[]{respostaId, posicao, resposta.sequencia().get(posicao)});
                    }
                }
            }
            jdbc.batchUpdate("INSERT INTO resposta_atividade_sequencia (resposta_id, posicao, alternativa_id) "
                    + "VALUES (?, ?, ?)", linhasSequencia);

            jdbc.batchUpdate("INSERT INTO resultado_atividade (usuario_id, atividade_id, acertos, pontuacao, "
                            + "concluida_em, comentario_professor, comentado_em, comentado_por_id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", linhasResultado,
                    new int[]{Types.BIGINT, Types.BIGINT, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP,
                            Types.VARCHAR, Types.TIMESTAMP, Types.BIGINT});
        }

        // Dias seguidos de acesso (Intensivão): os destaques entram todo dia nas últimas semanas
        private void criarSequenciasDeAcesso() {
            for (Aluno aluno : alunoPorId.values()) {
                Usuario usuario = aluno.usuario();
                int seguidos = switch (aluno.perfil()) {
                    case DESTAQUE -> 7 + aleatorio.nextInt(9);
                    case BOM -> 2 + aleatorio.nextInt(5);
                    case MEDIO -> aleatorio.nextInt(3);
                    default -> 0;
                };
                for (int dias = 1; dias <= seguidos; dias++) {
                    registrarLogin(usuario, diaEHora(dias, 7, 22));
                }
                // Acessos avulsos ao longo do período, para o histórico não ficar só nos dias de atividade
                double chance = switch (aluno.perfil()) {
                    case DESTAQUE -> 0.3;
                    case BOM -> 0.18;
                    case MEDIO -> 0.1;
                    case BAIXO -> 0.05;
                    default -> 0;
                };
                for (int dias = DIAS_DESDE_O_CADASTRO - 1; dias >= 1; dias--) {
                    LocalDateTime momento = diaEHora(dias, 7, 22);
                    if (aleatorio.nextDouble() < chance && momento.isAfter(usuario.getCriadoEm())) {
                        registrarLogin(usuario, momento);
                    }
                }

                TreeSet<LocalDate> dias = new TreeSet<>(acessos.getOrDefault(usuario.getId(), Map.of()).keySet());
                if (dias.isEmpty()) {
                    continue;
                }
                GamificacaoUsuario estado = GamificacaoUsuario.novo(usuario);
                estado.setUltimoAcesso(dias.last());
                estado.setDiasConsecutivos(sequenciaAte(dias, dias.last()));
                estado.setMaiorSequencia(maiorSequencia(dias));
                gamificacaoRepository.save(estado);
            }
        }

        private int sequenciaAte(TreeSet<LocalDate> dias, LocalDate ultimo) {
            int sequencia = 0;
            LocalDate dia = ultimo;
            while (dias.contains(dia)) {
                sequencia++;
                dia = dia.minusDays(1);
            }
            return sequencia;
        }

        private int maiorSequencia(TreeSet<LocalDate> dias) {
            int maior = 0;
            int atual = 0;
            LocalDate anterior = null;
            for (LocalDate dia : dias) {
                atual = anterior != null && anterior.plusDays(1).equals(dia) ? atual + 1 : 1;
                maior = Math.max(maior, atual);
                anterior = dia;
            }
            return maior;
        }

        private void gravarLogs() {
            // Alguns erros de senha espalhados pelo período, como acontece no uso real
            List<Aluno> alunos = new ArrayList<>(alunoPorId.values());
            for (int i = 0; i < 90; i++) {
                Usuario usuario = sortear(alunos).usuario();
                registrar(usuario, AcaoLog.FALHA_LOGIN, EntidadeLog.USUARIO, null,
                        diaEHora(1 + aleatorio.nextInt(DIAS_DESDE_O_CADASTRO - 2), 7, 22), ResultadoLog.FALHA,
                        "Usuário ou senha inválidos.", null);
            }

            acessos.forEach((usuarioId, porDia) -> porDia.values().forEach(momento ->
                    registrar(usuarioPorId.get(usuarioId), AcaoLog.LOGIN, EntidadeLog.USUARIO, usuarioId, momento,
                            ResultadoLog.SUCESSO, "Usuário #" + usuarioId + " entrou no sistema.", null)));

            // Em ordem de data, para a lista de registros ficar na sequência em que tudo aconteceu
            logs.sort(Comparator.comparing((Object[] linha) -> (Timestamp) linha[4]));
            jdbc.batchUpdate("INSERT INTO log_sistema "
                            + "(usuario_id, acao, entidade, entidade_id, data_hora, resultado, descricao, ip, dados) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", logs,
                    new int[]{Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.BIGINT, Types.TIMESTAMP,
                            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        }

        // Registros de log

        private void registrar(Usuario usuario, AcaoLog acao, EntidadeLog entidade, Long entidadeId,
                               LocalDateTime momento, ResultadoLog resultado, String descricao, String dados) {
            if (momento.isAfter(agora)) {
                return;
            }
            logs.add(new Object[]{usuario == null ? null : usuario.getId(), acao.name(),
                    entidade == null ? null : entidade.name(), entidadeId, Timestamp.valueOf(momento),
                    resultado.name(), descricao.length() > 500 ? descricao.substring(0, 500) : descricao,
                    usuario == null ? null : ipDoUsuario.get(usuario.getId()), dados});
        }

        // Um login por dia para cada pessoa basta para o histórico (fica o horário mais cedo)
        private void registrarLogin(Usuario usuario, LocalDateTime momento) {
            if (momento.isAfter(agora)) {
                return;
            }
            acessos.computeIfAbsent(usuario.getId(), id -> new HashMap<>())
                    .merge(momento.toLocalDate(), momento, (atual, novo) -> novo.isBefore(atual) ? novo : atual);
        }

        // Utilitários

        private LocalDateTime diaEHora(int diasAtras, int horaInicial, int horaFinal) {
            return hoje.minusDays(diasAtras).atTime(LocalTime.of(
                    horaInicial + aleatorio.nextInt(Math.max(1, horaFinal - horaInicial)),
                    aleatorio.nextInt(60), aleatorio.nextInt(60)));
        }

        private <T> T sortear(List<T> lista) {
            return lista.get(aleatorio.nextInt(lista.size()));
        }

        private <T> T sortear(T[] valores) {
            return valores[aleatorio.nextInt(valores.length)];
        }

        private List<Usuario> alunosDe(List<Aluno> alunos) {
            return alunos.stream().map(Aluno::usuario).toList();
        }

        private List<Long> ids(List<Alternativa> alternativas) {
            return alternativas.stream().map(Alternativa::getId).toList();
        }

        private void adicionar(List<Perfil> lista, Perfil perfil, int quantidade) {
            for (int i = 0; i < quantidade; i++) {
                lista.add(perfil);
            }
        }

        private String semAcentos(String texto) {
            return Normalizer.normalize(texto, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
        }
    }
}
