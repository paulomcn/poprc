package com.poprc.demo.service;

import com.poprc.demo.model.Funcionario;
import com.poprc.demo.model.RegistroPonto;
import com.poprc.demo.model.TipoPonto; // Mudou aqui
import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.repository.RegistroPontoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PontoService {

    private final RegistroPontoRepository pontoRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional
    public RegistroPonto salvarPonto(Long funcionarioId, TipoPonto tipo, String lat, String lon) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("Funcionário é obrigatório para registrar o ponto.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do ponto é obrigatório.");
        }
        validarCoordenada(lat, -90, 90, "latitude");
        validarCoordenada(lon, -180, 180, "longitude");

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado com o ID: " + funcionarioId));

        Optional<RegistroPonto> ultimoPonto = pontoRepository
                .findTopByFuncionarioIdOrderByDataHoraDesc(funcionarioId);
        if (tipo == TipoPonto.ENTRADA
                && ultimoPonto.map(RegistroPonto::getTipo).orElse(TipoPonto.SAIDA) == TipoPonto.ENTRADA) {
            throw new IllegalArgumentException("Já existe uma entrada aberta para este funcionário.");
        }
        if (tipo == TipoPonto.SAIDA
                && ultimoPonto.map(RegistroPonto::getTipo).orElse(TipoPonto.SAIDA) != TipoPonto.ENTRADA) {
            throw new IllegalArgumentException("Não existe uma entrada aberta para registrar a saída.");
        }

        RegistroPonto ponto = new RegistroPonto();
        ponto.setDataHora(LocalDateTime.now());
        ponto.setTipo(tipo);
        ponto.setLatitude(lat);
        ponto.setLongitude(lon);
        ponto.setFuncionario(funcionario);

        return pontoRepository.save(ponto);
    }

    public Optional<RegistroPonto> obterUltimoPonto(Long funcionarioId) {
        if (!funcionarioRepository.existsById(funcionarioId)) {
            throw new IllegalArgumentException("Funcionário não encontrado com o ID: " + funcionarioId);
        }
        return pontoRepository.findTopByFuncionarioIdOrderByDataHoraDesc(funcionarioId);
    }

    private void validarCoordenada(String valor, double minimo, double maximo, String nome) {
        try {
            double numero = Double.parseDouble(valor);
            if (numero < minimo || numero > maximo) {
                throw new IllegalArgumentException("Coordenada de " + nome + " inválida.");
            }
        } catch (NullPointerException | NumberFormatException ex) {
            throw new IllegalArgumentException("Coordenada de " + nome + " inválida.");
        }
    }
}
