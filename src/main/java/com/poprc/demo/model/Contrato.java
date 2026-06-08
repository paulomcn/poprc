package com.poprc.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cliente;

    private String contrato;

    private LocalDate vigenciaInicio;

    private LocalDate vigenciaFim;

    @ManyToOne
    @JoinColumn(name = "fiscal_tecnico_id")
    private Funcionario fiscalTecnico;

    @ManyToOne
    @JoinColumn(name = "gestor_contrato_id")
    private Funcionario gestorContrato;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorGlobal;

    @Column(length = 2000)
    private String escopo;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Projeto> projetos = new ArrayList<>();

    public Contrato() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getContrato() {
        return contrato;
    }

    public void setContrato(String contrato) {
        this.contrato = contrato;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public void setVigenciaInicio(LocalDate vigenciaInicio) {
        this.vigenciaInicio = vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public void setVigenciaFim(LocalDate vigenciaFim) {
        this.vigenciaFim = vigenciaFim;
    }

    public Funcionario getFiscalTecnico() {
        return fiscalTecnico;
    }

    public void setFiscalTecnico(Funcionario fiscalTecnico) {
        this.fiscalTecnico = fiscalTecnico;
    }

    public Funcionario getGestorContrato() {
        return gestorContrato;
    }

    public void setGestorContrato(Funcionario gestorContrato) {
        this.gestorContrato = gestorContrato;
    }

    public BigDecimal getValorGlobal() {
        return valorGlobal;
    }

    public void setValorGlobal(BigDecimal valorGlobal) {
        this.valorGlobal = valorGlobal;
    }

    public String getEscopo() {
        return escopo;
    }

    public void setEscopo(String escopo) {
        this.escopo = escopo;
    }

    public List<Projeto> getProjetos() {
        return projetos;
    }

    public void setProjetos(List<Projeto> projetos) {
        this.projetos = projetos;
    }
}
