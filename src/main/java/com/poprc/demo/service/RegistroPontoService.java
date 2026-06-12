package com.poprc.demo.service;

import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.repository.RegistroPontoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistroPontoService {

    private final RegistroPontoRepository repository;

    public RegistroPonto salvarPonto(RegistroPonto ponto) {
        // Obs: Em um cenário real, você pegaria o ID do funcionário logado
        // via SecurityContextHolder do Spring Security aqui.
        if (ponto.getDataHora() == null) {
            ponto.setDataHora(LocalDateTime.now());
        }
        return repository.save(ponto);
    }
}