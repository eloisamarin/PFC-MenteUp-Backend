package com.umc.menteup.repository;

import com.umc.menteup.dto.TotalPorAluno;
import com.umc.menteup.model.UsuarioConquista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioConquistaRepository extends JpaRepository<UsuarioConquista, Long> {

    ///Conquistas do aluno, na ordem da tela
    @Query("""
            select uc from UsuarioConquista uc
            join fetch uc.conquista c
            where uc.usuario.id = :usuarioId
            order by c.ordemExibicao
            """)
    List<UsuarioConquista> buscarDoUsuario(@Param("usuarioId") Long usuarioId);

    ///Conquistas ganhas que o aluno ainda não viu
    @Query("""
            select uc from UsuarioConquista uc
            join fetch uc.conquista c
            where uc.usuario.id = :usuarioId
              and uc.desbloqueada = true
              and uc.visualizada = false
            order by uc.dataDesbloqueio
            """)
    List<UsuarioConquista> buscarNaoVisualizadas(@Param("usuarioId") Long usuarioId);

    ///XP ganho com conquistas
    @Query("""
            select coalesce(sum(uc.xpConcedido), 0L) from UsuarioConquista uc
            where uc.usuario.id = :usuarioId
              and uc.desbloqueada = true
            """)
    long somarXpDeConquistas(@Param("usuarioId") Long usuarioId);

    ///XP de conquistas de cada aluno, usado no ranking
    @Query("""
            select new com.umc.menteup.dto.TotalPorAluno(uc.usuario.id, sum(uc.xpConcedido), count(uc.id))
            from UsuarioConquista uc
            where uc.desbloqueada = true
            group by uc.usuario.id
            """)
    List<TotalPorAluno> somarPorAluno();
}
