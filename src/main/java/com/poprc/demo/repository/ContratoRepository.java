package com.poprc.demo.repository;

import com.poprc.demo.model.Contrato;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT c FROM Contrato c WHERE c.cliente = ?1")
    List<Contrato> findByCliente(String cliente);

    @Query("SELECT c FROM Contrato c WHERE c.arquivado = false AND CURRENT_DATE BETWEEN c.vigenciaInicio AND c.vigenciaFim")
    List<Contrato> findContratoAtivos();
}
