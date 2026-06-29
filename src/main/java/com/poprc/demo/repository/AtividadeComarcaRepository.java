package com.poprc.demo.repository;

import com.poprc.demo.model.AtividadeComarca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AtividadeComarcaRepository extends JpaRepository<AtividadeComarca, Long> {
    List<AtividadeComarca> findByComarcaIdOrderByDataInicioDesc(Long comarcaId);
}