package com.sigea.demosigea_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a todos los endpoints (/api/v1/...)
                .allowedOrigins(
                    "http://localhost:5173",          // Frontend local (Vite/React)
                    "https://sigea-team.github.io"     // Frontend en GitHub Pages
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Retry-After") // HU-01: el frontend lee los segundos restantes del bloqueo
                .allowCredentials(true);
    }
}