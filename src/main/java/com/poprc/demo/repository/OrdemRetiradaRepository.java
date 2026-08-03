package com.poprc.demo.repository;

import com.poprc.demo.model.OrdemRetirada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdemRetiradaRepository extends JpaRepository<OrdemRetirada, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemRetirada o where o.id = :id")
    Optional<OrdemRetirada> findByIdForUpdate(@Param("id") Long id);

    long countByOrdemServicoId(Long ordemServicoId);

    boolean existsByNumeroOr(String numeroOr);

    List<OrdemRetirada> findByOrdemServicoIdOrderByDataGeracaoDesc(Long ordemServicoId);

    boolean existsByOrdemServicoIdAndStatusIn(Long ordemServicoId, List<String> status);

    List<OrdemRetirada> findByComarcaIdOrderByDataGeracaoDesc(Long comarcaId);

    List<OrdemRetirada> findAllByOrderByDataGeracaoDesc();
}
