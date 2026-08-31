package com.poprc.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EdicaoRetiradaHistoricaRequest(
        BigDecimal quantidadeRetirada,
        LocalDate dataRetirada,
        String motivo) {
}
