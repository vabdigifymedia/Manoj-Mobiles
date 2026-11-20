package com.api.manojmobiles.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Default Server (Relative URL - Recommended for HTTPS & Local)"),
                        new Server().url("https://api.emistore.in").description("Production Server (HTTPS)"),
                        new Server().url("http://localhost:8080").description("Localhost (HTTP)")
                ))
                .info(new Info()
                        .title("Manoj Mobiles API")
                        .version("1.0")
                        .description("API Documentation for Manoj Mobiles E-commerce application.")
                        .contact(new Contact().name("Manoj Mobiles Support").email("support@manojmobiles.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
