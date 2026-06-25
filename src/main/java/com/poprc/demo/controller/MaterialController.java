package com.poprc.demo.controller;

import com.poprc.demo.model.Material;
import com.poprc.demo.repository.MaterialRepository; // Assumindo que você já tem esse repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    @Autowired
    private MaterialRepository materialRepository;

    /**
     *  Lista todos os materiais do estoque real do Postgres
     */
    @GetMapping
    public ResponseEntity<List<Material>> listarTodos() {
        List<Material> materiais = materialRepository.findAll();
        return ResponseEntity.ok(materiais);
    }
}