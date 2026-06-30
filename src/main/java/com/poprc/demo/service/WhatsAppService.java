package com.poprc.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    // Cliente HTTP padrão do Spring injetado via Lombok
    private final RestTemplate restTemplate;

    /**
     * Faz o disparo HTTP POST simulado para o gateway do WhatsApp
     */
    public void enviarMensagemAlerta(String numeroDestino, String mensagem) {
        // URL fictícia de um gateway de integração (Ex: Z-API, Twilio, etc.)
        String urlGateway = "https://api.z-api.io/instances/SUA_INSTANCIA/token/TEXTO/send-text";

        try {
            // 1. Configura os Headers da requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Client-Token", "FATOR_TOKEN_SEGURANCA_AQUI"); // Exemplo de Header de Auth

            // 2. Monta o Payload JSON que a API externa espera receber
            Map<String, String> payload = new HashMap<>();
            payload.put("phone", numeroDestino);
            payload.put("message", mensagem);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

            // 3. Executa a chamada POST real
            ResponseEntity<String> response = restTemplate.postForEntity(urlGateway, request, String.class);

            // 4. Trata o retorno do status HTTP
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(" Alerta de WhatsApp enviado com sucesso para: " + numeroDestino);
            } else {
                System.err.println(
                        " ️ Gateway do WhatsApp aceitou a chamada, mas retornou status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // Tratamento robusto de exceção para a sua aplicação não crashar se a API do
            // Zap cair
            System.err.println("  Falha crítica ao conectar com a API externa do WhatsApp: " + e.getMessage());
        }
    }
}