package com.poprc.demo.repository;

import com.poprc.demo.model.RegistroPonto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, Long> {
    Optional<RegistroPonto> findTopByFuncionarioIdOrderByDataHoraDesc(Long funcionarioId);
}
