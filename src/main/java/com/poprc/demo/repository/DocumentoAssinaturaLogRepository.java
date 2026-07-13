package com.poprc.demo.repository;

import com.poprc.demo.model.DocumentoAssinaturaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoAssinaturaLogRepository extends JpaRepository<DocumentoAssinaturaLog, Long> {
    List<DocumentoAssinaturaLog> findByDocumentoIdOrderByRegistradoEmAsc(Long documentoId);
}
