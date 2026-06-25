package com.poprc.demo.repository;

import com.poprc.demo.model.ConfiguracaoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoNotificacaoRepository extends JpaRepository<ConfiguracaoNotificacao, Long> {
}