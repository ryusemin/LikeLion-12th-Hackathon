package com.demo.nimn.config;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(
                        new Components()
                                // accessToken이라는 스키마 만들어주기
                                .addSecuritySchemes("accessToken", new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("Bearer")
                                        .bearerFormat("JWT")
                                )
                )
                .info(apiInfo())
//                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
//                        .url("https://nimn.store")
//                        .description("Production server"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server()
                        .url("http://localhost:8080")
                        .description("Local development server"));
    }

    private Info apiInfo() {
        return new Info()
                .title("우리동네 영양사")
                .description("우리동네 영양사 API Docs")
                .version("1.0.0");
    }
}