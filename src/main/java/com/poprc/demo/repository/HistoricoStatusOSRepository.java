package com.poprc.demo.repository;

import com.poprc.demo.model.HistoricoStatusOS;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusOSRepository extends JpaRepository<HistoricoStatusOS, Long> {
    List<HistoricoStatusOS> findByOrdemServicoIdOrderByRegistradoEmAscIdAsc(Long ordemServicoId);
}
