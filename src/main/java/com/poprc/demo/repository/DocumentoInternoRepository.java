package com.poprc.demo.repository;

import com.poprc.demo.model.DocumentoInterno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoInternoRepository extends JpaRepository<DocumentoInterno, Long> {
    List<DocumentoInterno> findByComarcaIdAndTipoOrderByDataGeracaoDesc(Long comarcaId, String tipo);

    List<DocumentoInterno> findByComarcaIdOrderByDataGeracaoDesc(Long comarcaId);

    List<DocumentoInterno> findByComarcaIdAndTipoAndStatusOrderByDataGeracaoDesc(
            Long comarcaId, String tipo, String status);
}
