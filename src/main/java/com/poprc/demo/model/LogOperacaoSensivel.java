package com.poprc.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_operacoes_sensiveis")
public class LogOperacaoSensivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long funcionarioId;
    private String usuario;
    private String perfil;
    private String metodoHttp;
    @Column(length = 500)
    private String caminho;
    private Integer statusHttp;
    private String enderecoIp;
    private LocalDateTime registradoEm;

    public Long getId() { return id; }
    public Long getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Long funcionarioId) { this.funcionarioId = funcionarioId; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public String getMetodoHttp() { return metodoHttp; }
    public void setMetodoHttp(String metodoHttp) { this.metodoHttp = metodoHttp; }
    public String getCaminho() { return caminho; }
    public void setCaminho(String caminho) { this.caminho = caminho; }
    public Integer getStatusHttp() { return statusHttp; }
    public void setStatusHttp(Integer statusHttp) { this.statusHttp = statusHttp; }
    public String getEnderecoIp() { return enderecoIp; }
    public void setEnderecoIp(String enderecoIp) { this.enderecoIp = enderecoIp; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
}
