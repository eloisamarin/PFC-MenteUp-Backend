package com.umc.menteup.repository;

import com.umc.menteup.model.GamificacaoUsuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GamificacaoUsuarioRepository extends JpaRepository<GamificacaoUsuario, Long> {

    Optional<GamificacaoUsuario> findByUsuario_Id(Long usuarioId);

    ///Cria a linha do aluno se ainda não existir (sem erro se dois pedidos chegarem juntos)
    @Modifying
    @Query(value = """
            INSERT INTO tb_gamificacao_usuario (usuario_id, dias_consecutivos, maior_sequencia, atualizado_em)
            VALUES (:usuarioId, 0, 0, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int criarSeNaoExistir(@Param("usuarioId") Long usuarioId);

    ///Busca a linha do aluno e a bloqueia até o fim da operação, para acessos simultâneos não contarem em dobro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GamificacaoUsuario g where g.usuario.id = :usuarioId")
    Optional<GamificacaoUsuario> buscarComLock(@Param("usuarioId") Long usuarioId);
}
