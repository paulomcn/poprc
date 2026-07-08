package com.poprc.demo.repository;

import com.poprc.demo.model.MaterialItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialItemRepository extends JpaRepository<MaterialItem, Long> {
}
