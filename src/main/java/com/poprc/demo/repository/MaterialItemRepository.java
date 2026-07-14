package com.poprc.demo.repository;

import com.poprc.demo.model.MaterialItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialItemRepository extends JpaRepository<MaterialItem, Long> {
    List<MaterialItem> findByComarcaIdOrderByIdAsc(Long comarcaId);
}
