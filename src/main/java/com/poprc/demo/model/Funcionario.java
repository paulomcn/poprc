package com.poprc.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String funcao;

    private String cidade;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(length = 11)
    private String cpf;

    private String telefone;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_acesso", nullable = false)
    private PerfilAcesso perfilAcesso = PerfilAcesso.TECNICO;

    @Column(nullable = false)
    private Boolean ativo = true;

    @JsonIgnore
    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "troca_senha_obrigatoria", nullable = false)
    private Boolean trocaSenhaObrigatoria = false;

    @JsonIgnore
    @Column(name = "tentativas_login", nullable = false)
    private Integer tentativasLogin = 0;

    @JsonIgnore
    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @ElementCollection
    @CollectionTable(name = "funcionario_certificacoes", joinColumns = @JoinColumn(name = "funcionario_id"))
    @Column(name = "certificacao")
    private List<String> certificacoes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "funcionario_document_paths", joinColumns = @JoinColumn(name = "funcionario_id"))
    @Column(name = "document_path")
    private List<String> documentPaths = new ArrayList<>();

    public Funcionario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFuncao() {
        return funcao;
    }

    public void setFuncao(String funcao) {
        this.funcao = funcao;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getCpfMascarado() {
        return cpf == null || cpf.length() != 11
                ? null
                : "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PerfilAcesso getPerfilAcesso() {
        return perfilAcesso;
    }

    public void setPerfilAcesso(PerfilAcesso perfilAcesso) {
        this.perfilAcesso = perfilAcesso;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Boolean getTrocaSenhaObrigatoria() { return trocaSenhaObrigatoria; }
    public void setTrocaSenhaObrigatoria(Boolean trocaSenhaObrigatoria) {
        this.trocaSenhaObrigatoria = trocaSenhaObrigatoria;
    }
    public Integer getTentativasLogin() { return tentativasLogin; }
    public void setTentativasLogin(Integer tentativasLogin) { this.tentativasLogin = tentativasLogin; }
    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(LocalDateTime bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    @JsonProperty("senhaConfigurada")
    public boolean isSenhaConfigurada() {
        return senhaHash != null && !senhaHash.isBlank();
    }

    public List<String> getCertificacoes() {
        return certificacoes;
    }

    public void setCertificacoes(List<String> certificacoes) {
        this.certificacoes = certificacoes;
    }

    public List<String> getDocumentPaths() {
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        this.documentPaths = documentPaths;
    }
}
