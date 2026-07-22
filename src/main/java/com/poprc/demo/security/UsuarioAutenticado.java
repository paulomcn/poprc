package com.poprc.demo.security;

import com.poprc.demo.model.Funcionario;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class UsuarioAutenticado implements OAuth2User, Serializable {
    private final Long funcionarioId;
    private final String nome;
    private final String email;
    private final String perfil;
    private final String metodoAutenticacao;
    private final boolean trocaSenhaObrigatoria;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioAutenticado(Long funcionarioId, String nome, String email, String perfil) {
        this(funcionarioId, nome, email, perfil, "ZOHO", false);
    }

    public UsuarioAutenticado(Long funcionarioId, String nome, String email, String perfil,
            String metodoAutenticacao, boolean trocaSenhaObrigatoria) {
        this.funcionarioId = funcionarioId;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.metodoAutenticacao = metodoAutenticacao;
        this.trocaSenhaObrigatoria = trocaSenhaObrigatoria;
        this.attributes = criarAtributos();
        this.authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
    }

    public static UsuarioAutenticado de(Funcionario funcionario) {
        return de(funcionario, "ZOHO");
    }

    public static UsuarioAutenticado de(Funcionario funcionario, String metodoAutenticacao) {
        return new UsuarioAutenticado(funcionario.getId(), funcionario.getNome(), funcionario.getEmail(),
                funcionario.getPerfilAcesso().name(), metodoAutenticacao,
                !"DEV".equals(metodoAutenticacao)
                        && Boolean.TRUE.equals(funcionario.getTrocaSenhaObrigatoria()));
    }

    public Long getFuncionarioId() { return funcionarioId; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getPerfil() { return perfil; }
    public String getMetodoAutenticacao() { return metodoAutenticacao; }
    public boolean isTrocaSenhaObrigatoria() { return trocaSenhaObrigatoria; }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getName() {
        return email == null || email.isBlank() ? "funcionario-" + funcionarioId : email;
    }

    private Map<String, Object> criarAtributos() {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("id", funcionarioId);
        valores.put("funcionarioId", funcionarioId);
        valores.put("nome", nome);
        valores.put("email", email);
        valores.put("perfil", perfil);
        valores.put("metodoAutenticacao", metodoAutenticacao);
        valores.put("trocaSenhaObrigatoria", trocaSenhaObrigatoria);
        return java.util.Collections.unmodifiableMap(valores);
    }
}
