package com.umc.menteup.service;

import com.umc.menteup.dto.DocumentoLegalDtos;
import com.umc.menteup.dto.MeusDadosResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.RespostaDesafioRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioConquistaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// Monta o arquivo com os dados da própria pessoa (direito de acesso e de portabilidade da LGPD)
@Service
public class ExportacaoDadosService {

    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final ResultadoAtividadeRepository resultadoRepository;
    private final RespostaDesafioRepository respostaDesafioRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final LogSistemaRepository logSistemaRepository;
    private final DocumentoLegalService documentoLegalService;
    private final LogSistemaService logSistemaService;
    private final Clock clock;

    public ExportacaoDadosService(UsuarioRepository usuarioRepository,
                                  TurmaRepository turmaRepository,
                                  ResultadoAtividadeRepository resultadoRepository,
                                  RespostaDesafioRepository respostaDesafioRepository,
                                  UsuarioConquistaRepository usuarioConquistaRepository,
                                  LogSistemaRepository logSistemaRepository,
                                  DocumentoLegalService documentoLegalService,
                                  LogSistemaService logSistemaService,
                                  Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.resultadoRepository = resultadoRepository;
        this.respostaDesafioRepository = respostaDesafioRepository;
        this.usuarioConquistaRepository = usuarioConquistaRepository;
        this.logSistemaRepository = logSistemaRepository;
        this.documentoLegalService = documentoLegalService;
        this.logSistemaService = logSistemaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MeusDadosResponse exportar(String username) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
        Long id = usuario.getId();

        MeusDadosResponse dados = new MeusDadosResponse(
                LocalDateTime.now(clock),
                new MeusDadosResponse.Conta(
                        id,
                        usuario.getNomeUsuario(),
                        usuario.getUsuario(),
                        usuario.getTipoUsuario(),
                        usuario.getAvatar() == null ? null : usuario.getAvatar().name(),
                        usuario.getStatusCadastro() == null ? null : usuario.getStatusCadastro().name(),
                        usuario.getTermoAceitoEm()),
                documentoLegalService.aceitesDo(id).stream()
                        .map(DocumentoLegalDtos.Aceite::de)
                        .toList(),
                turmaRepository.findAllByAlunos_Id(id).stream()
                        .map(t -> new MeusDadosResponse.Turma(t.getId(), t.getNome()))
                        .toList(),
                resultadoRepository.buscarDoUsuarioComAtividade(id).stream()
                        .map(r -> new MeusDadosResponse.Atividade(
                                r.getAtividade().getTitulo(), r.getAcertos(), r.getPontuacao(),
                                r.getConcluida_em(), r.getComentarioProfessor(), r.getComentadoEm()))
                        .toList(),
                respostaDesafioRepository.buscarDoUsuarioComDesafio(id).stream()
                        .map(r -> new MeusDadosResponse.Desafio(
                                r.getPergunta().getDesafio().getTitulo(), r.getPergunta().getEnunciado(),
                                r.getCorreta(), r.getPontos(), r.getRespondidaEm()))
                        .toList(),
                usuarioConquistaRepository.buscarDoUsuario(id).stream()
                        .map(c -> new MeusDadosResponse.Conquista(
                                c.getConquista().getNome(), c.getDesbloqueada(), c.getProgressoAtual(),
                                c.getMeta(), c.getXpConcedido(), c.getDataDesbloqueio()))
                        .toList(),
                logSistemaRepository.findAllByUsuario_IdOrderByDataHoraDesc(id).stream()
                        .map(l -> new MeusDadosResponse.Registro(
                                l.getAcao() == null ? null : l.getAcao().name(),
                                l.getResultado() == null ? null : l.getResultado().name(),
                                l.getDescricao(), l.getIp(), l.getDataHora()))
                        .toList()
        );

        // Registro do atendimento do pedido (direito do titular)
        logSistemaService.registrarSucesso(usuario, AcaoLog.EXPORTAR_DADOS, EntidadeLog.USUARIO, id,
                "Usuário #" + id + " baixou os próprios dados.", null);

        return dados;
    }
}
