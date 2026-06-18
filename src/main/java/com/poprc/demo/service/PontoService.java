package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.model.TipoPonto; // Mudou aqui
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.RegistroPontoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PontoService {

    private final RegistroPontoRepository pontoRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional
    public RegistroPonto salvarPonto(Long funcionarioId, TipoPonto tipo, String lat, String lon) { // Mudou aqui
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado com o ID: " + funcionarioId));

        RegistroPonto ponto = new RegistroPonto();
        ponto.setDataHora(LocalDateTime.now());
        ponto.setTipo(tipo);
        ponto.setLatitude(lat);
        ponto.setLongitude(lon);
        ponto.setFuncionario(funcionario);

        return pontoRepository.save(ponto);
    }
}