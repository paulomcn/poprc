package com.poprc.demo.repository;

import com.poprc.demo.model.NotificacaoOperacional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacaoOperacionalRepository extends JpaRepository<NotificacaoOperacional, Long> {

    boolean existsByChave(String chave);

    Optional<NotificacaoOperacional> findFirstByChaveBaseAndResolvidaEmIsNull(String chaveBase);

    List<NotificacaoOperacional> findAllByTipoAndResolvidaEmIsNull(String tipo);

    @EntityGraph(attributePaths = {"ordemServico", "destinatario"})
    List<NotificacaoOperacional> findAllByOrderByCriadaEmDesc();

    @EntityGraph(attributePaths = {"ordemServico", "destinatario"})
    @Query("""
            select n from NotificacaoOperacional n
            where n.destinatario is null or n.destinatario.id = :funcionarioId
            order by n.criadaEm desc
            """)
    List<NotificacaoOperacional> listarVisiveisPara(@Param("funcionarioId") Long funcionarioId);
}
