package com.poprc.demo.config;

import com.poprc.demo.repository.MaterialRepository;
import com.poprc.demo.service.SaldoLocalService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EstoqueLocalBootstrap implements ApplicationRunner {
    private final MaterialRepository materialRepository;
    private final SaldoLocalService saldoLocalService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        materialRepository.findAll().forEach(saldoLocalService::sincronizarSaldoInicial);
    }
}
