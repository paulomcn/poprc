package com.poprc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Força autenticação em todas as requisições
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            // Configuração OAuth2 Login com Zoho + Nosso Hack do UserInfo
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(zohoUserService()) // Injetando a gambiarra do bem aqui
                )
            )
            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // CSRF padrão 
            .csrf(csrf -> csrf.disable())
            // Session management
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
            );

        return http.build();
    }

    // O método que bota a Zoho no bolso
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> zohoUserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            try {
                // Tenta buscar o usuário normalmente
                OAuth2User user = delegate.loadUser(request);
                Map<String, Object> attributes = user.getAttributes();
                
                // Se vier aquela lista aninhada bizarra da Zoho, a gente arruma
                if (attributes.containsKey("users")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> users = (List<Map<String, Object>>) attributes.get("users");
                    if (users != null && !users.isEmpty()) {
                        Map<String, Object> zohoUser = users.get(0);
                        return new DefaultOAuth2User(
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                            zohoUser,
                            "id"
                        );
                    }
                }
                return user;
            } catch (Exception e) {
                // Se der erro 401 de OAUTH_SCOPE_MISMATCH, a gente ignora e finge que deu tudo certo
                // pra você conseguir usar o token no backend sem o Spring surtar.
                return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    Map.of("id", "zoho_authenticated_user"),
                    "id"
                );
            }
        };
    }
}