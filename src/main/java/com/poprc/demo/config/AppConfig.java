package com.poprc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     *  Registra o RestTemplate na memória do Spring para ele conseguir
     * injetar automaticamente no nosso WhatsAppService.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}