package com.poprc.demo.repository;

import com.poprc.demo.model.LogOperacaoSensivel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogOperacaoSensivelRepository extends JpaRepository<LogOperacaoSensivel, Long> {
    List<LogOperacaoSensivel> findTop100ByTipoEventoStartingWithOrderByRegistradoEmDesc(String prefixo);
}
