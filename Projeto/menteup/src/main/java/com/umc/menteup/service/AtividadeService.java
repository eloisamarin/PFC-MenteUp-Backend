package com.umc.menteup.service;

import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Turma;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.umc.menteup.model.Usuario;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
public class AtividadeService {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private NotificacaoService notificacaoService;



    /*
     * LISTAR TODAS AS ATIVIDADES
     */
    public List<Atividade> listarTodas() {
        return atividadeRepository.findAll();
    }

    /*
     * BUSCAR ATIVIDADE PELO ID
     */
    public Optional<Atividade> buscarPorId(Long id) {
        return atividadeRepository.findById(id);
    }

    /*
     * BUSCAR ATIVIDADES PELO TÍTULO
     */
    public List<Atividade> buscarPorTitulo(String titulo) {
        return atividadeRepository
                .findAllByTituloContainingIgnoreCase(titulo);
    }

    /*
     * CRIAR ATIVIDADE
     */
    @Transactional
    public Atividade criarAtividade(Atividade atividade) {

        if (atividade.getTurma() == null ||
                atividade.getTurma().getId() == null) {

            throw new IllegalArgumentException(
                    "A turma da atividade é obrigatória."
            );
        }

        // Busca a turma REAL no banco
        Turma turma = turmaRepository.findById(
                atividade.getTurma().getId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Turma não encontrada."
                )
        );

        // Substitui a turma que veio no JSON pela turma do banco
        atividade.setTurma(turma);

        Atividade novaAtividade =
                atividadeRepository.save(atividade);

        System.out.println(
                "ATIVIDADE CRIADA: "
                        + novaAtividade.getTitulo()
        );

        System.out.println(
                "TURMA ENCONTRADA: "
                        + turma.getId()
        );

        System.out.println(
                "QUANTIDADE DE ALUNOS: "
                        + turma.getAlunos().size()
        );

        for (Usuario aluno : turma.getAlunos()) {

            System.out.println(
                    "ALUNO: "
                            + aluno.getUsuario()
            );

            if (aluno.getFcmToken() != null &&
                    !aluno.getFcmToken().isBlank()) {

                System.out.println(
                        "Enviando notificação para o aluno..."
                );

                notificacaoService.notificarNovaAtividade(
                        novaAtividade,
                        aluno.getFcmToken()
                );

            } else {

                System.out.println(
                        "Aluno sem FCM Token."
                );
            }
        }

        return novaAtividade;
    }

    /*
     * ATUALIZAR ATIVIDADE
     */
    public Optional<Atividade> atualizarAtividade(
            Long id,
            Atividade atividadeAtualizada) {

        Optional<Atividade> atividadeExistente =
                atividadeRepository.findById(id);

        if (atividadeExistente.isEmpty()) {
            return Optional.empty();
        }

        Atividade atividade = atividadeExistente.get();

        atividade.setTitulo(atividadeAtualizada.getTitulo());
        atividade.setDescricao(atividadeAtualizada.getDescricao());
        atividade.setPontuacao(atividadeAtualizada.getPontuacao());
        atividade.setStatus(atividadeAtualizada.getStatus());
        atividade.setTurma(atividadeAtualizada.getTurma());
        atividade.setUsuario(atividadeAtualizada.getUsuario());
        atividade.setPerguntas(atividadeAtualizada.getPerguntas());

        return Optional.of(
                atividadeRepository.save(atividade)
        );
    }

    /*
     * DELETAR ATIVIDADE
     */
    public boolean deletarAtividade(Long id) {

        if (!atividadeRepository.existsById(id)) {
            return false;
        }

        atividadeRepository.deleteById(id);

        return true;
    }
}