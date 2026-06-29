package com.poprc.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String diretorioAbsoluto = System.getProperty("user.home") + "/rc_uploads/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + diretorioAbsoluto);
    }
}