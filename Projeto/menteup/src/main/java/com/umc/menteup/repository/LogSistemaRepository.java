package com.umc.menteup.repository;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.LogSistema;
import com.umc.menteup.model.ResultadoLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogSistemaRepository  extends JpaRepository<LogSistema, Long> {
    @Query("""
            SELECT l FROM LogSistema l
            WHERE (:usuarioId IS NULL OR l.usuario.id = :usuarioId)
                AND (:acao IS NULL OR l.acao = :acao)
                AND (:entidade IS NULL OR l.entidade = :entidade)
                AND (:resultado IS NULL OR l.resultado = :resultado)
            """)
    Page<LogSistema> buscar(
            @Param ("usuarioId") Long usuarioId,
            @Param ("acao")AcaoLog acao,
            @Param ("entidade")EntidadeLog entidade,
            @Param ("resultado")ResultadoLog resultado,
            Pageable pageable
            );

    // Troca o nome da pessoa nos textos de log antigos (usado quando a conta é excluída)
    @Modifying
    @Query(value = "UPDATE log_sistema SET descricao = REPLACE(descricao, :nome, :substituto) "
            + "WHERE descricao LIKE CONCAT('%', :nome, '%')", nativeQuery = true)
    int substituirNasDescricoes(@Param("nome") String nome, @Param("substituto") String substituto);

    // Exportação dos dados: registros feitos pela própria pessoa
    List<LogSistema> findAllByUsuario_IdOrderByDataHoraDesc(Long usuarioId);
}
