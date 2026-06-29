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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. ATIVANDO O CORS NA FILTRAÇÃO DO SPRING SECURITY
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. DESATIVANDO O CSRF PARA COLEGAS EXTERNOS (COMO O REST CLIENT) CONSEGUIREM
                // DAR POST
                .csrf(csrf -> csrf.disable())

                // 3. REGRAS DE PERMISSÃO
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**", "/error", "/uploads/**").permitAll()
                        .anyRequest().authenticated())
                // Configuração OAuth2 Login com Zoho + Nosso Hack do UserInfo
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(zohoUserService())))
                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // Session management
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession()));

        return http.build();
    }

    // O BEAN QUE CONFIGURA O QUE O CORS VAI LIBERAR
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permite qualquer origem usando padrões (essencial para testar no celular
        // usando o IP da rede local)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Libera os métodos HTTP que o seu front vai usar
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Libera todos os headers (Content-Type, Authorization, etc.)
        configuration.setAllowedHeaders(List.of("*"));

        // Permite o envio de cookies e headers de autenticação
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // O método que bota a Zoho no bolso
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> zohoUserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            try {
                OAuth2User user = delegate.loadUser(request);
                Map<String, Object> attributes = user.getAttributes();

                if (attributes.containsKey("users")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> users = (List<Map<String, Object>>) attributes.get("users");
                    if (users != null && !users.isEmpty()) {
                        Map<String, Object> zohoUser = users.get(0);
                        return new DefaultOAuth2User(
                                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                                zohoUser,
                                "id");
                    }
                }
                return user;
            } catch (Exception e) {
                return new DefaultOAuth2User(
                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                        Map.of("id", "zoho_authenticated_user"),
                        "id");
            }
        };
    }
}