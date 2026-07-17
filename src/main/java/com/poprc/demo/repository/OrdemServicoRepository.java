package com.poprc.demo.repository;

import com.poprc.demo.model.OrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> findByIdForUpdate(@Param("id") Long id);

    // Query dinâmica de alta performance processada direto pelo Postgres
    @Query("SELECT o FROM OrdemServico o LEFT JOIN o.contrato c WHERE " +
            "(:numeroOs IS NULL OR :numeroOs = '' OR LOWER(o.numeroOs) LIKE LOWER(CONCAT('%', :numeroOs, '%'))) AND " +
            "(:cliente IS NULL OR :cliente = '' OR LOWER(c.cliente) LIKE LOWER(CONCAT('%', :cliente, '%')))")
    List<OrdemServico> buscarComFiltros(@Param("numeroOs") String numeroOs, @Param("cliente") String cliente);

    long countByContratoId(Long contratoId);

    boolean existsByNumeroOs(String numeroOs);
}
