package com.poprc.demo.service;

import com.poprc.demo.model.Comarca;
import com.poprc.demo.model.Contrato;
import com.poprc.demo.model.OrdemRetirada;
import com.poprc.demo.model.OrdemServico;
import com.poprc.demo.model.Projeto;
import com.poprc.demo.model.StatusOS;
import com.poprc.demo.repository.ComarcaRepository;
import com.poprc.demo.repository.ContratoRepository;
import com.poprc.demo.repository.OrdemRetiradaRepository;
import com.poprc.demo.repository.OrdemServicoRepository;
import com.poprc.demo.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArquivamentoService {

    private final ContratoRepository contratoRepository;
    private final ProjetoRepository projetoRepository;
    private final ComarcaRepository comarcaRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final OrdemRetiradaRepository ordemRetiradaRepository;

    @Transactional
    public Contrato arquivarContrato(Long id, String usuario, String motivo) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        validarNovoArquivamento(contrato.getArquivado(), usuario, motivo, "Contrato");
        if (projetoRepository.findByContratoId(id).stream().anyMatch(this::ativo)) {
            throw new IllegalStateException("Arquive primeiro todos os projetos ativos deste contrato.");
        }
        if (ordemServicoRepository.findByContratoId(id).stream().anyMatch(this::ativa)) {
            throw new IllegalStateException("Arquive primeiro todas as Ordens de Serviço ativas deste contrato.");
        }
        marcarArquivado(contrato, usuario, motivo);
        return contratoRepository.save(contrato);
    }

    @Transactional
    public Projeto arquivarProjeto(Long id, String usuario, String motivo) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
        validarNovoArquivamento(projeto.getArquivado(), usuario, motivo, "Projeto");
        if (comarcaRepository.findByProjetoId(id).filter(this::ativa).isPresent()) {
            throw new IllegalStateException("Arquive primeiro a obra/comarca ativa vinculada a este projeto.");
        }
        if (ordemServicoRepository.findByProjetoId(id).stream().anyMatch(this::ativa)) {
            throw new IllegalStateException("Arquive primeiro todas as Ordens de Serviço ativas deste projeto.");
        }
        marcarArquivado(projeto, usuario, motivo);
        return projetoRepository.save(projeto);
    }

    @Transactional
    public Comarca arquivarComarca(Long id, String usuario, String motivo) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obra/comarca não encontrada."));
        validarNovoArquivamento(comarca.getArquivado(), usuario, motivo, "Obra/comarca");
        if (comarca.getOrdemServico() != null && ativa(comarca.getOrdemServico())) {
            throw new IllegalStateException("Arquive primeiro a Ordem de Serviço vinculada a esta obra.");
        }
        marcarArquivado(comarca, usuario, motivo);
        return comarcaRepository.save(comarca);
    }

    @Transactional
    public OrdemServico arquivarOrdemServico(Long id, String usuario, String motivo) {
        OrdemServico ordem = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada."));
        validarNovoArquivamento(ordem.getArquivado(), usuario, motivo, "Ordem de Serviço");
        List<OrdemRetirada> retiradas = ordemRetiradaRepository.findByOrdemServicoIdOrderByDataGeracaoDesc(id);
        if (retiradas.stream().anyMatch(or -> !"DEVOLVIDA".equals(or.getStatus()))) {
            throw new IllegalStateException(
                    "A OS possui Ordem de Retirada ainda não devolvida. Conclua a devolução antes de arquivar.");
        }
        if (!retiradas.isEmpty()
                && ordem.getStatus() != StatusOS.CONCLUIDA
                && ordem.getStatus() != StatusOS.FATURADA) {
            throw new IllegalStateException(
                    "Uma OS com movimentação de estoque só pode ser arquivada após sua conclusão.");
        }
        marcarArquivado(ordem, usuario, motivo);
        return ordemServicoRepository.save(ordem);
    }

    @Transactional
    public Contrato restaurarContrato(Long id) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        limparArquivamento(contrato);
        return contratoRepository.save(contrato);
    }

    @Transactional
    public Projeto restaurarProjeto(Long id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
        if (projeto.getContrato() != null && Boolean.TRUE.equals(projeto.getContrato().getArquivado())) {
            throw new IllegalStateException("Restaure primeiro o contrato deste projeto.");
        }
        limparArquivamento(projeto);
        return projetoRepository.save(projeto);
    }

    @Transactional
    public OrdemServico restaurarOrdemServico(Long id) {
        OrdemServico ordem = ordemServicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada."));
        if (ordem.getContrato() != null && Boolean.TRUE.equals(ordem.getContrato().getArquivado())) {
            throw new IllegalStateException("Restaure primeiro o contrato desta OS.");
        }
        if (ordem.getProjeto() != null && Boolean.TRUE.equals(ordem.getProjeto().getArquivado())) {
            throw new IllegalStateException("Restaure primeiro o projeto desta OS.");
        }
        limparArquivamento(ordem);
        return ordemServicoRepository.save(ordem);
    }

    @Transactional
    public Comarca restaurarComarca(Long id) {
        Comarca comarca = comarcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obra/comarca não encontrada."));
        if (comarca.getProjeto() != null && Boolean.TRUE.equals(comarca.getProjeto().getArquivado())) {
            throw new IllegalStateException("Restaure primeiro o projeto desta obra.");
        }
        if (comarca.getOrdemServico() != null && Boolean.TRUE.equals(comarca.getOrdemServico().getArquivado())) {
            throw new IllegalStateException("Restaure primeiro a Ordem de Serviço desta obra.");
        }
        limparArquivamento(comarca);
        return comarcaRepository.save(comarca);
    }

    private void validarNovoArquivamento(Boolean arquivado, String usuario, String motivo, String entidade) {
        if (Boolean.TRUE.equals(arquivado)) {
            throw new IllegalStateException(entidade + " já está arquivado(a).");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("Informe quem está realizando o arquivamento.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Informe o motivo do arquivamento.");
        }
    }

    private boolean ativo(Projeto projeto) {
        return !Boolean.TRUE.equals(projeto.getArquivado());
    }

    private boolean ativa(Comarca comarca) {
        return !Boolean.TRUE.equals(comarca.getArquivado());
    }

    private boolean ativa(OrdemServico ordem) {
        return !Boolean.TRUE.equals(ordem.getArquivado());
    }

    private void marcarArquivado(Contrato contrato, String usuario, String motivo) {
        contrato.setArquivado(true);
        contrato.setArquivadoEm(LocalDateTime.now());
        contrato.setArquivadoPor(usuario.trim());
        contrato.setMotivoArquivamento(motivo.trim());
    }

    private void marcarArquivado(Projeto projeto, String usuario, String motivo) {
        projeto.setArquivado(true);
        projeto.setArquivadoEm(LocalDateTime.now());
        projeto.setArquivadoPor(usuario.trim());
        projeto.setMotivoArquivamento(motivo.trim());
    }

    private void marcarArquivado(Comarca comarca, String usuario, String motivo) {
        comarca.setArquivado(true);
        comarca.setArquivadoEm(LocalDateTime.now());
        comarca.setArquivadoPor(usuario.trim());
        comarca.setMotivoArquivamento(motivo.trim());
    }

    private void marcarArquivado(OrdemServico ordem, String usuario, String motivo) {
        ordem.setArquivado(true);
        ordem.setArquivadoEm(LocalDateTime.now());
        ordem.setArquivadoPor(usuario.trim());
        ordem.setMotivoArquivamento(motivo.trim());
    }

    private void limparArquivamento(Contrato contrato) {
        contrato.setArquivado(false);
        contrato.setArquivadoEm(null);
        contrato.setArquivadoPor(null);
        contrato.setMotivoArquivamento(null);
    }

    private void limparArquivamento(Projeto projeto) {
        projeto.setArquivado(false);
        projeto.setArquivadoEm(null);
        projeto.setArquivadoPor(null);
        projeto.setMotivoArquivamento(null);
    }

    private void limparArquivamento(Comarca comarca) {
        comarca.setArquivado(false);
        comarca.setArquivadoEm(null);
        comarca.setArquivadoPor(null);
        comarca.setMotivoArquivamento(null);
    }

    private void limparArquivamento(OrdemServico ordem) {
        ordem.setArquivado(false);
        ordem.setArquivadoEm(null);
        ordem.setArquivadoPor(null);
        ordem.setMotivoArquivamento(null);
    }
}
