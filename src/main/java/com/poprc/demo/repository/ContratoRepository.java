package com.poprc.demo.repository;

import com.poprc.demo.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    
    @Query("SELECT c FROM Contrato c WHERE c.cliente = ?1")
    List<Contrato> findByCliente(String cliente);
    
    @Query("SELECT c FROM Contrato c WHERE CURRENT_DATE BETWEEN c.vigenciaInicio AND c.vigenciaFim")
    List<Contrato> findContratoAtivos();
}
