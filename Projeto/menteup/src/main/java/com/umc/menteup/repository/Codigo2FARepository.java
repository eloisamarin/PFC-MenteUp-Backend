package com.umc.menteup.repository;

import com.umc.menteup.model.Codigo2FA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.Optional;

public interface Codigo2FARepository  extends JpaRepository<Codigo2FA,Long> {

    Optional<Codigo2FA> findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(String usuario);

    // Último código gerado (usado ou não): base para o reenvio
    Optional<Codigo2FA> findTopByUsuarioOrderByIdDesc(String usuario);
    void deleteByUsuario(String usuario);

    // Descarte: códigos que venceram antes da data limite
    @Modifying
    @Query("delete from Codigo2FA c where c.expiracao < :limite")
    int apagarVencidosAntesDe(@Param("limite") LocalDateTime limite);

}
