package com.umc.menteup.repository;

import com.umc.menteup.model.SolicitacaoTurma;
import com.umc.menteup.model.SolicitacaoTurma.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoTurmaRepository extends JpaRepository<SolicitacaoTurma, Long> {

    // Impede dois pedidos abertos do mesmo aluno para a mesma turma
    boolean existsByTurma_IdAndAluno_IdAndStatus(Long turmaId, Long alunoId, Status status);

    // Pedidos do aluno, do mais novo para o mais antigo
    List<SolicitacaoTurma> findAllByAluno_IdOrderByCriadaEmDesc(Long alunoId);

    // Pedidos de uma turma com um status (ex.: PENDENTE), do mais antigo para o mais novo
    List<SolicitacaoTurma> findAllByTurma_IdAndStatusOrderByCriadaEmAsc(Long turmaId, Status status);

    Optional<SolicitacaoTurma> findByIdAndTurma_Id(Long id, Long turmaId);

    Optional<SolicitacaoTurma> findByIdAndAluno_Id(Long id, Long alunoId);

    // Usado ao excluir a turma: os pedidos dela vão junto
    @Modifying
    @Query("delete from SolicitacaoTurma s where s.turma.id = :turmaId")
    void apagarDaTurma(@Param("turmaId") Long turmaId);
}
