package com.poprc.demo.specification;

import com.poprc.demo.model.Contrato;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ContratoSpecification {

    public static Specification<Contrato> filtrar(
            List<String> clientes, String contrato, List<String> status,
            String recorrencia, String segmento, String gestor,
            LocalDate dataInicio, LocalDate dataFim,
            BigDecimal valorMin, BigDecimal valorMax) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 💥 Multi-seleção de Clientes (Cláusula IN)
            if (clientes != null && !clientes.isEmpty()) {
                predicates.add(root.get("cliente").in(clientes));
            }
            if (contrato != null && !contrato.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("contrato")), "%" + contrato.toLowerCase() + "%"));
            }
            // 💥 Multi-seleção de Status (Cláusula IN)
            if (status != null && !status.isEmpty()) {
                predicates.add(root.get("status").in(status));
            }
            // 💥 Filtros de Recorrência e Segmento
            if (recorrencia != null && !recorrencia.isBlank()) {
                predicates.add(cb.equal(root.get("recorrencia"), recorrencia));
            }
            if (segmento != null && !segmento.isBlank()) {
                predicates.add(cb.equal(root.get("segmento"), segmento));
            }
            if (gestor != null && !gestor.isBlank()) {
                predicates.add(cb.equal(root.get("gestorResponsavel"), gestor));
            }

            // Intervalos e Datas
            if (dataInicio != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("vigenciaInicio"), dataInicio));
            if (dataFim != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("vigenciaFim"), dataFim));
            if (valorMin != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("valorGlobal"), valorMin));
            if (valorMax != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("valorGlobal"), valorMax));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}