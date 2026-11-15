package com.umc.menteup.service;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.model.StatusCadastro;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.TokenRedefinicaoSenhaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

// Descarta os dados que já passaram do prazo de guarda
// Os prazos ficam no application.properties e precisam bater com o item 8 da Política de Privacidade
// Os aceites do termo e da política não são apagados: são a prova do consentimento
@Service
public class RetencaoDadosService {
    private static final Logger log = LoggerFactory.getLogger(RetencaoDadosService.class);

    private final Codigo2FARepository codigo2FARepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final LogSistemaRepository logSistemaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilService perfilService;
    private final LogSistemaService logSistemaService;
    private final TransactionTemplate transacao;
    private final Clock clock;

    private final boolean ativa;
    private final int logsMeses;
    private final int codigosDias;
    private final int linksDias;
    private final int cadastroRejeitadoDias;
    private final int conviteVencidoDias;

    public RetencaoDadosService(
            Codigo2FARepository codigo2FARepository,
            TokenRedefinicaoSenhaRepository tokenRepository,
            LogSistemaRepository logSistemaRepository,
            UsuarioRepository usuarioRepository,
            PerfilService perfilService,
            LogSistemaService logSistemaService,
            PlatformTransactionManager gerenciadorDeTransacao,
            Clock clock,
            @Value("${menteup.retencao.ativa:true}") boolean ativa,
            @Value("${menteup.retencao.logs-meses:6}") int logsMeses,
            @Value("${menteup.retencao.codigos-dias:1}") int codigosDias,
            @Value("${menteup.retencao.links-dias:7}") int linksDias,
            @Value("${menteup.retencao.cadastro-rejeitado-dias:30}") int cadastroRejeitadoDias,
            @Value("${menteup.retencao.convite-vencido-dias:30}") int conviteVencidoDias
    ) {
        this.codigo2FARepository = codigo2FARepository;
        this.tokenRepository = tokenRepository;
        this.logSistemaRepository = logSistemaRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilService = perfilService;
        this.logSistemaService = logSistemaService;
        this.transacao = new TransactionTemplate(gerenciadorDeTransacao);
        this.clock = clock;
        this.ativa = ativa;
        // O Marco Civil exige no mínimo 6 meses de registros de acesso: um valor menor é ignorado
        this.logsMeses = Math.max(6, logsMeses);
        this.codigosDias = codigosDias;
        this.linksDias = linksDias;
        this.cadastroRejeitadoDias = cadastroRejeitadoDias;
        this.conviteVencidoDias = conviteVencidoDias;
    }

    // Roda uma vez por dia (3h da manhã por padrão)
    @Scheduled(cron = "${menteup.retencao.horario:0 0 3 * * *}", zone = "America/Sao_Paulo")
    public void executarAgendado() {
        if (ativa) {
            executar();
        }
    }

    // Roda também quando a aplicação sobe, porque no Render ela pode estar dormindo às 3h
    @EventListener(ApplicationReadyEvent.class)
    public void executarAoIniciar() {
        if (ativa) {
            executar();
        }
    }

    // Faz o descarte e devolve quantos itens saíram de cada grupo
    public Map<String, Integer> executar() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Map<String, Integer> descartados = new LinkedHashMap<>();

        descartados.put("codigos2FA", etapa("códigos de verificação",
                () -> codigo2FARepository.apagarVencidosAntesDe(agora.minusDays(codigosDias))));

        descartados.put("links", etapa("links de senha e convites",
                () -> tokenRepository.apagarVencidosAntesDe(agora.minusDays(linksDias))));

        descartados.put("cadastrosRejeitados", etapa("pedidos de professor rejeitados",
                () -> anonimizar(usuarioRepository.findAllByStatusCadastroAndStatusAlteradoEmBefore(
                        StatusCadastro.REJEITADA, agora.minusDays(cadastroRejeitadoDias)))));

        descartados.put("convitesVencidos", etapa("convites não aceitos",
                () -> anonimizar(usuarioRepository.findAllByPrimeiroAcessoPendenteTrueAndCriadoEmBefore(
                        agora.minusDays(conviteVencidoDias)))));

        descartados.put("logs", etapa("registros de acesso e auditoria",
                () -> logSistemaRepository.apagarAntesDe(agora.minusMonths(logsMeses))));

        int total = descartados.values().stream().mapToInt(Integer::intValue).sum();

        // Só deixa registro quando algo foi descartado, para não encher o log todo dia
        if (total > 0) {
            logSistemaService.registrar(null, AcaoLog.DESCARTAR_DADOS, null, null, ResultadoLog.SUCESSO,
                    "Descarte automático de dados vencidos: " + total + " item(ns).", descartados);
            log.info("Retenção: {} item(ns) descartado(s) {}", total, descartados);
        }

        return descartados;
    }

    // Cada grupo roda na sua transação: se um falhar, os outros continuam
    private int etapa(String nome, Supplier<Integer> acao) {
        try {
            Integer quantidade = transacao.execute(status -> acao.get());
            return quantidade == null ? 0 : quantidade;
        } catch (Exception e) {
            log.warn("Retenção: falha ao descartar {}: {}", nome, e.getMessage());
            return 0;
        }
    }

    private int anonimizar(List<Usuario> contas) {
        int quantidade = 0;
        for (Usuario conta : contas) {
            if (perfilService.anonimizarPorRetencao(conta)) {
                quantidade++;
            }
        }
        return quantidade;
    }
}
