package com.umc.menteup.repository;

import com.umc.menteup.model.AceiteDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AceiteDocumentoRepository extends JpaRepository<AceiteDocumento, Long> {

    // Já traz o documento junto, para mostrar tipo e versão sem novas consultas
    @Query("""
            select a from AceiteDocumento a
            join fetch a.documento
            where a.usuario.id = :usuarioId
            order by a.aceitoEm desc, a.id desc
            """)
    List<AceiteDocumento> buscarDoUsuario(@Param("usuarioId") Long usuarioId);

    boolean existsByUsuario_IdAndDocumento_Id(Long usuarioId, Long documentoId);

    boolean existsByUsuario_Id(Long usuarioId);

    // Versão que alguém já aceitou não pode ser cancelada
    boolean existsByDocumento_Id(Long documentoId);
}
