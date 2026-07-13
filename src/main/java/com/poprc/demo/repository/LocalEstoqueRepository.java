package com.poprc.demo.repository;

import com.poprc.demo.model.LocalEstoque;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalEstoqueRepository extends JpaRepository<LocalEstoque, Long> {
    Optional<LocalEstoque> findByNomeIgnoreCase(String nome);
    List<LocalEstoque> findAllByOrderByNomeAsc();
}
