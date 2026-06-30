package com.poprc.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal) {
        // Pega o ID que a gente forçou lá no hack do SecurityConfig
        String userId = principal.getAttribute("id");

        return "<h1>Deu bom!  </h1>" +
                "<p>Você passou pelo OAuth2 da Zoho!</p>" +
                "<p>Usuário logado: <b>" + userId + "</b></p>";
    }
}