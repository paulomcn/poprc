package com.poprc.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final com.poprc.demo.security.OperacaoSensivelInterceptor operacaoSensivelInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(operacaoSensivelInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String diretorioAbsoluto = System.getProperty("user.home") + "/rc_uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + diretorioAbsoluto);
    }
}
