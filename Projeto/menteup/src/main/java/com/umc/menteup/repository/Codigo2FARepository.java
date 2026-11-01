package com.umc.menteup.repository;

import com.umc.menteup.model.Codigo2FA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Codigo2FARepository  extends JpaRepository<Codigo2FA,Long> {

    Optional<Codigo2FA> findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(String usuario);

}
