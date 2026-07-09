package com.poprc.demo.repository;

import com.poprc.demo.model.OrdemRetirada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdemRetiradaRepository extends JpaRepository<OrdemRetirada, Long> {
    long countByOrdemServicoId(Long ordemServicoId);

    boolean existsByNumeroOr(String numeroOr);

    List<OrdemRetirada> findByOrdemServicoIdOrderByDataGeracaoDesc(Long ordemServicoId);

    List<OrdemRetirada> findByComarcaIdOrderByDataGeracaoDesc(Long comarcaId);

    List<OrdemRetirada> findAllByOrderByDataGeracaoDesc();
}
