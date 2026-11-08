package com.umc.menteup.service;

import com.umc.menteup.dto.ConquistaAlunoResponse;
import com.umc.menteup.dto.ResumoConquistasResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Conquista;
import com.umc.menteup.model.GamificacaoUsuario;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioConquista;
import com.umc.menteup.repository.ConquistaRepository;
import com.umc.menteup.repository.GamificacaoUsuarioRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioConquistaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

///Consultas de conquistas para o front-end
@Service
public class ConquistaConsultaService {

    private final ConquistaRepository conquistaRepository;
    private final UsuarioConquistaRepository usuarioConquistaRepository;
    private final GamificacaoUsuarioRepository gamificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurmaRepository turmaRepository;
    private final ConquistaService conquistaService;

    public ConquistaConsultaService(
            ConquistaRepository conquistaRepository,
            UsuarioConquistaRepository usuarioConquistaRepository,
            GamificacaoUsuarioRepository gamificacaoRepository,
            UsuarioRepository usuarioRepository,
            TurmaRepository turmaRepository,
            ConquistaService conquistaService
    ) {
        this.conquistaRepository = conquistaRepository;
        this.usuarioConquistaRepository = usuarioConquistaRepository;
        this.gamificacaoRepository = gamificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.turmaRepository = turmaRepository;
        this.conquistaService = conquistaService;
    }

    ///Conquistas ativas, com as secretas escondidas
    @Transactional(readOnly = true)
    public List<ConquistaAlunoResponse> listarCatalogo() {
        return conquistaRepository.findAllByAtivaTrueOrderByOrdemExibicaoAsc().stream()
                .map(c -> ConquistaAlunoResponse.de(c, null, true))
                .toList();
    }

    ///Conquistas do aluno logado (identificado pelo token)
    @Transactional(readOnly = true)
    public ResumoConquistasResponse buscarDoAlunoLogado(String username) {
        Usuario aluno = carregarUsuario(username);
        return montarResumo(aluno.getId(), true);
    }

    ///Devolve as conquistas que o aluno ainda não viu e já as marca como vistas
    @Transactional
    public List<ConquistaAlunoResponse> consumirNovas(String username) {
        Usuario aluno = carregarUsuario(username);

        List<UsuarioConquista> novas = usuarioConquistaRepository.buscarNaoVisualizadas(aluno.getId());
        novas.forEach(uc -> uc.setVisualizada(true));

        return novas.stream()
                .map(uc -> ConquistaAlunoResponse.de(uc.getConquista(), uc, false))
                .toList();
    }

    ///Conquistas de um aluno. Podem ver: admin, professor de uma turma do aluno ou o próprio aluno
    @Transactional(readOnly = true)
    public ResumoConquistasResponse buscarDoAluno(Long alunoId, String username) {
        Usuario solicitante = carregarUsuario(username);

        Usuario aluno = usuarioRepository.findById(alunoId)
                .filter(TipoUsuario::ehAluno)
                .orElseThrow(() -> ApiException.naoEncontrado("ALUNO_NAO_ENCONTRADO", "Aluno não encontrado."));

        boolean permitido = TipoUsuario.ehAdministrador(solicitante)
                || solicitante.getId().equals(aluno.getId())
                || (TipoUsuario.ehProfessor(solicitante) && professorDoAluno(solicitante.getId(), aluno.getId()));

        if (!permitido) {
            throw ApiException.proibido(
                    "SEM_ACESSO_AO_ALUNO", "Você não tem permissão para ver as conquistas deste aluno.");
        }

        ///Só o próprio aluno não vê as secretas
        boolean ocultarSecretas = solicitante.getId().equals(aluno.getId());
        return montarResumo(aluno.getId(), ocultarSecretas);
    }

    ///

    private ResumoConquistasResponse montarResumo(Long alunoId, boolean ocultarSecretas) {
        Map<Long, UsuarioConquista> estado = usuarioConquistaRepository.buscarDoUsuario(alunoId).stream()
                .collect(Collectors.toMap(uc -> uc.getConquista().getId(), Function.identity()));

        List<ConquistaAlunoResponse> conquistas = new ArrayList<>();
        for (Conquista c : conquistaRepository.findAllByOrderByOrdemExibicaoAsc()) {
            UsuarioConquista uc = estado.get(c.getId());
            boolean desbloqueada = uc != null && Boolean.TRUE.equals(uc.getDesbloqueada());

            ///Conquista desativada só aparece para quem já ganhou
            if (Boolean.TRUE.equals(c.getAtiva()) || desbloqueada) {
                conquistas.add(ConquistaAlunoResponse.de(c, uc, ocultarSecretas));
            }
        }

        GamificacaoUsuario g = gamificacaoRepository.findByUsuario_Id(alunoId).orElse(null);

        return new ResumoConquistasResponse(
                alunoId,
                conquistaService.calcularXpTotal(alunoId),
                (int) conquistas.stream().filter(ConquistaAlunoResponse::desbloqueada).count(),
                conquistas.size(),
                g != null ? g.getDiasConsecutivos() : 0,
                g != null ? g.getMaiorSequencia() : 0,
                conquistas
        );
    }

    private boolean professorDoAluno(Long professorId, Long alunoId) {
        return turmaRepository.findAllByAlunos_Id(alunoId).stream()
                .anyMatch(t -> t.getProfessor() != null && professorId.equals(t.getProfessor().getId()));
    }

    private Usuario carregarUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
}
