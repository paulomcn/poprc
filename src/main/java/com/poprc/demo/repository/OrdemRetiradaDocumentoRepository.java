package com.poprc.demo.repository;

import com.poprc.demo.model.OrdemRetiradaDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdemRetiradaDocumentoRepository extends JpaRepository<OrdemRetiradaDocumento, Long> {

    Optional<OrdemRetiradaDocumento> findByOrdemRetiradaIdAndFase(Long ordemRetiradaId, String fase);

    Optional<OrdemRetiradaDocumento> findFirstByOrdemRetiradaIdOrderByGeradoEmDescIdDesc(Long ordemRetiradaId);

    List<OrdemRetiradaDocumento> findByOrdemRetiradaIdOrderByGeradoEmAscIdAsc(Long ordemRetiradaId);
}
