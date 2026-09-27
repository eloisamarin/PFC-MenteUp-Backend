package com.umc.menteup.repository;

import com.umc.menteup.model.Codio2FA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Codigo2FARepository  extends JpaRepository<Codio2FA,Long> {

    Optional<Codio2FA> findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(String usuario);

}
