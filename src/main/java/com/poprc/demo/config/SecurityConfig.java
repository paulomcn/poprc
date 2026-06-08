package com.poprc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configuração moderna do Spring Security (Spring Boot 4.x)
     * Força qualquer requisição a passar pela autenticação Zoho
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Força autenticação em todas as requisições
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            // Configuração OAuth2 Login com Zoho
            .oauth2Login(oauth2 -> oauth2
                //    .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
            )
            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // CSRF padrão (habilitado por padrão no Spring Security)
            .csrf(csrf -> csrf.disable())
            // Session management
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
            );

        return http.build();
    }
}
