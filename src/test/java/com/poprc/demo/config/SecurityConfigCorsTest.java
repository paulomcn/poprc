package com.poprc.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.poprc.demo.repository.FuncionarioRepository;
import com.poprc.demo.security.ValidacaoSessaoFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsTest {

    @Test
    void permiteCabecalhoDeAuditoriaUsadoPelosDocumentos() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(FuncionarioRepository.class),
                mock(ValidacaoSessaoFilter.class),
                true,
                false,
                "http://localhost:5173",
                List.of("http://localhost:[*]", "http://127.0.0.1:[*]"));

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/documentos-internos/vistoria");
        request.addHeader("Origin", "http://127.0.0.1:5173");
        CorsConfiguration cors = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedHeaders()).contains("X-XSRF-TOKEN");
        assertThat(cors.getAllowedHeaders()).doesNotContain("X-Usuario-Atual");
    }
}
