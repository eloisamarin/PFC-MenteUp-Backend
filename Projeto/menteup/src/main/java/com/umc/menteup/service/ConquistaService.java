package com.umc.menteup.service;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

///Verifica e libera as conquistas de um aluno
///Sempre olha os números atuais do aluno, então pode rodar várias vezes sem dar nada em dobro
///Se uma verificação falhar, a próxima (atividade, desafio ou login) corrige
///Se a meta mudar no catálogo, a nova vale só para quem ainda não ganhou
@Service
public class ConquistaService {

    private final ConquistaRepository conquistaRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final GamificacaoUsuarioRepository gamificacaoRepository;
    private final ResultadoAtividadeRepository resultadoRepository;
    private final RespostaDesafioRepository respostaDesafioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RankingService rankingService;
    private final LogSistemaService logSistemaService;
    private final Clock clock;

    public ConquistaService(
            ConquistaRepository conquistaRepository,
            UsuarioConquistaRepository usuarioConquistaRepository,
            GamificacaoUsuarioRepository gamificacaoRepository,
            ResultadoAtividadeRepository resultadoRepository,
            RespostaDesafioRepository respostaDesafioRepository,
            UsuarioRepository usuarioRepository,
            RankingService rankingService,
            LogSistemaService logSistemaService,
            Clock clock
    ) {
        this.conquistaRepository = conquistaRepository;
        this.usuarioConquistaRepository = usuarioConquistaRepository;
        this.gamificacaoRepository = gamificacaoRepository;
        this.resultadoRepository = resultadoRepository;
        this.respostaDesafioRepository = respostaDesafioRepository;
        this.usuarioRepository = usuarioRepository;
        this.rankingService = rankingService;
        this.logSistemaService = logSistemaService;
        this.clock = clock;
    }

    ///Números do aluno usados pelas regras
    record Indicadores(
            long atividadesConcluidas,
            long atividadesPerfeitas,
            long xpTotal,             ///atividades + desafios + conquistas
            int diasConsecutivos,
            Integer posicaoRanking,   ///null se ainda não está no ranking
            int posicoesSubidas
    ) {}

    ///Verifica as conquistas do aluno e devolve as que ele acabou de ganhar. Ignora professores e admins
    @Transactional
    public List<UsuarioConquista> avaliar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !TipoUsuario.ehAluno(usuario)) {
            return List.of();
        }

        ///Bloqueia o aluno durante a verificação: duas ao mesmo tempo esperam uma pela outra
        GamificacaoUsuario estado = travarEstado(usuarioId);
        Indicadores indicadores = calcularIndicadores(usuarioId, estado);

        Map<Long, UsuarioConquista> porConquista = usuarioConquistaRepository.buscarDoUsuario(usuarioId)
                .stream()
                .collect(Collectors.toMap(uc -> uc.getConquista().getId(), Function.identity()));

        LocalDateTime agora = LocalDateTime.now(clock);
        List<UsuarioConquista> novas = new ArrayList<>();

        for (Conquista conquista : conquistaRepository.findAllByAtivaTrueOrderByOrdemExibicaoAsc()) {
            UsuarioConquista uc = porConquista.computeIfAbsent(
                    conquista.getId(), id -> UsuarioConquista.nova(usuario, conquista));

            if (uc.getDesbloqueada()) {
                continue;
            }

            int meta = conquista.getMeta();
            long valor = valorAtual(conquista.getTipoRegra(), meta, indicadores);

            uc.setMeta(meta);
            uc.setProgressoAtual((int) Math.min(valor, meta));
            uc.setUltimaAvaliacao(agora);

            if (valor >= meta) {
                uc.setDesbloqueada(true);
                uc.setDataDesbloqueio(agora);
                uc.setXpConcedido(conquista.getRecompensaXp());
                uc.setVisualizada(false);
                novas.add(uc);
            }
        }

        usuarioConquistaRepository.saveAll(porConquista.values());
        registrarLogsAposCommit(usuario, novas);

        ///Se ganhou algo, verifica de novo: o XP do bônus pode liberar um nível ou subir no ranking
        if (!novas.isEmpty()) {
            List<UsuarioConquista> todas = new ArrayList<>(novas);
            todas.addAll(avaliar(usuarioId));
            return todas;
        }
        return novas;
    }

    ///Mesma verificação, mas gravada separadamente. Usada pelo ConquistaListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UsuarioConquista> avaliarEmNovaTransacao(Long usuarioId) {
        return avaliar(usuarioId);
    }

    ///Cria (se preciso) e bloqueia a linha de gamificação do aluno até o fim da operação
    @Transactional
    public GamificacaoUsuario travarEstado(Long usuarioId) {
        gamificacaoRepository.criarSeNaoExistir(usuarioId);
        return gamificacaoRepository.buscarComLock(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "Registro de gamificação não encontrado para o usuário " + usuarioId));
    }

    ///XP total: atividades + desafios + conquistas. É o mesmo número usado no ranking
    @Transactional(readOnly = true)
    public long calcularXpTotal(Long usuarioId) {
        return resultadoRepository.somarPontuacaoDoUsuario(usuarioId)
                + respostaDesafioRepository.somarPontosDoUsuario(usuarioId)
                + usuarioConquistaRepository.somarXpDeConquistas(usuarioId);
    }

    private Indicadores calcularIndicadores(Long usuarioId, GamificacaoUsuario estado) {
        Integer posicao = buscarPosicaoNoRanking(usuarioId);

        ///"Subir N posições" = estar N posições acima da pior colocação já registrada
        estado.setPosicaoRankingAtual(posicao);
        if (posicao != null && (estado.getPiorPosicaoRanking() == null || posicao > estado.getPiorPosicaoRanking())) {
            estado.setPiorPosicaoRanking(posicao);
        }
        int subidas = posicao == null ? 0 : estado.getPiorPosicaoRanking() - posicao;

        return new Indicadores(
                resultadoRepository.countByUsuario_Id(usuarioId),
                resultadoRepository.contarAtividadesPerfeitas(usuarioId),
                calcularXpTotal(usuarioId),
                estado.getDiasConsecutivos(),
                posicao,
                subidas
        );
    }

    ///Posição no ranking (a mesma da tela de ranking)
    private Integer buscarPosicaoNoRanking(Long usuarioId) {
        List<RankingResponse> ranking = rankingService.buscarRanking();
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).usuarioId().equals(usuarioId)) {
                return i + 1;
            }
        }
        return null;
    }

    ///Quanto o aluno já tem em cada tipo de regra. Ganha quando valor >= meta. Tipo novo entra aqui
    static long valorAtual(TipoRegraConquista tipo, int meta, Indicadores ind) {
        return switch (tipo) {
            case XP_ACUMULADO -> ind.xpTotal();
            case CONTAGEM_ATIVIDADES -> ind.atividadesConcluidas();
            case APROVEITAMENTO -> ind.atividadesPerfeitas();
            case SEQUENCIA_DIAS -> ind.diasConsecutivos();
            case EVOLUCAO_RANKING -> ind.posicoesSubidas();
            ///Ou chegou na posição, ou não
            case POSICAO_RANKING -> ind.posicaoRanking() != null && ind.posicaoRanking() <= meta ? meta : 0;
        };
    }

    ///Grava o log só se o desbloqueio for salvo de fato
    private void registrarLogsAposCommit(Usuario usuario, List<UsuarioConquista> novas) {
        if (novas.isEmpty()) {
            return;
        }

        Runnable registrar = () -> novas.forEach(uc -> logSistemaService.registrarSucesso(
                usuario,
                AcaoLog.DESBLOQUEAR_CONQUISTA,
                EntidadeLog.CONQUISTA,
                uc.getConquista().getId(),
                "Aluno \"" + usuario.getNomeUsuario() + "\" desbloqueou a conquista \""
                        + uc.getConquista().getNome() + "\".",
                Map.of(
                        "codigo", uc.getConquista().getCodigo(),
                        "xpConcedido", uc.getXpConcedido()
                )
        ));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    registrar.run();
                }
            });
        } else {
            registrar.run();
        }
    }
}
