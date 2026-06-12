package com.poprc.demo.repository;

import com.poprc.demo.model.RegistroPonto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, Long> {
}