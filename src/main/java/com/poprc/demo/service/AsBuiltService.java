package com.poprc.demo.service;

import com.poprc.demo.model.AsBuilt;
import com.poprc.demo.repository.AsBuiltRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AsBuiltService {

    private final AsBuiltRepository repository;

    @Transactional
    public AsBuilt salvar(AsBuilt asBuilt) {
        if (asBuilt.getVersao() == null) {
            asBuilt.setVersao(1);
        }
        String logInicial = String.format("Versão 1 criada em %s por Sistema.\n", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        asBuilt.setHistoricoVersoes(logInicial);
        return repository.save(asBuilt);
    }

    @Transactional
    public AsBuilt atualizarVersao(Long asBuiltId, String novaUrl, String nomeUsuarioModificou) {
        AsBuilt asBuilt = repository.findById(asBuiltId)
                .orElseThrow(() -> new RuntimeException("Arquivo As-Built não encontrado"));

        // Incrementa a versão em +1
        int novaVersao = asBuilt.getVersao() + 1;
        asBuilt.setVersao(novaVersao);
        asBuilt.setUrlArquivo(novaUrl);

        // Alimenta o histórico TEXT anexando o log da modificação
        String novoLog = String.format("Versão %d atualizada em %s por %s.\n",
                novaVersao,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                nomeUsuarioModificou);
        
        asBuilt.setHistoricoVersoes(asBuilt.getHistoricoVersoes() + novoLog);

        return repository.save(asBuilt);
    }
}