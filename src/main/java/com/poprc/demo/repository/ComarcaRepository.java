package com.poprc.demo.repository;

import com.poprc.demo.model.Comarca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ComarcaRepository extends JpaRepository<Comarca, Long> {
    Optional<Comarca> findByProjetoId(Long projetoId);
    Optional<Comarca> findByOrdemServicoNumeroOs(String numeroOs);
}
