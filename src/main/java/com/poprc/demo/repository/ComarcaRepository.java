package com.poprc.demo.repository;

import com.poprc.demo.model.Comarca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComarcaRepository extends JpaRepository<Comarca, Long> {
}
